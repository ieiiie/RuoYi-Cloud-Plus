package com.ym.iot.jetlinks.service.impl;

import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.domain.dto.FertilizerTaskReady;
import com.ym.iot.jetlinks.mapper.JetLinksCommandTaskMapper;
import com.ym.jetlinks.rpc.IotChangeDispatchRpcService;

import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** 提交/回执驱动的单步执行器。提交后推进一步，失败任务合并重试，启动时仅扫描一次存量任务。 延时重试只用于启动握手或失败中的操作；空闲时不查数据库、设备或 JetLinks。 */
@Slf4j
@Component
@ConditionalOnJetLinks
public class JetLinksTaskDispatcher {
    private final JetLinksFertilizerTaskRunner runner;
    private final JetLinksCommandTaskMapper taskMapper;
    private final String consumer;
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread thread = new Thread(r, "jetlinks-task-events");
                        thread.setDaemon(true);
                        return thread;
                    });
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean draining = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();
    private volatile boolean closed;

    @DubboReference(
            group = "jetlinks-iot",
            version = "1.0.0",
            check = false,
            retries = 0,
            timeout = 5000)
    private IotChangeDispatchRpcService dispatch;

    public JetLinksTaskDispatcher(
            JetLinksFertilizerTaskRunner runner,
            JetLinksCommandTaskMapper taskMapper,
            @Value("${ym.iot.jetlinks.consumer-id:ym-iot-business-v1}") String consumer) {
        this.runner = runner;
        this.taskMapper = taskMapper;
        this.consumer = consumer;
    }

    @EventListener
    public void submitted(FertilizerTaskReady event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) return;
        // AFTER_COMMIT 事件监听器在 afterCompletion 中运行，异常只会被记录，无法让
        // 入站 RPC 返回失败。使用 afterCommit 同步回调，让已提交投影的推进失败可被重投。
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        committed(event);
                    }
                });
    }

    private void committed(FertilizerTaskReady event) {
        if (closed) throw new IllegalStateException("任务执行器正在停止，请重试");
        try {
            // 原事务已提交并释放锁；新事务推进一步后，入站 RPC 才可以确认回执。
            // 避免先确认事件再留在内存队列，进程退出后再无回执来推进下一步。
            runner.advance(event.requestId(), false);
        } catch (RuntimeException failure) {
            pending.add(event.requestId());
            drain();
            throw failure;
        }
    }

    private void drain() {
        if (closed || !draining.compareAndSet(false, true)) return;
        executor.execute(
                () -> {
                    boolean failed = false;
                    for (String id : new ArrayList<>(pending)) {
                        pending.remove(id);
                        try {
                            runner.advance(id, false);
                        } catch (RuntimeException error) {
                            pending.add(id);
                            failed = true;
                            log.warn("任务推进失败，保留任务等待重试 requestId={}", id);
                        }
                    }
                    if (failed) {
                        executor.schedule(
                                () -> {
                                    draining.set(false);
                                    drain();
                                },
                                5,
                                TimeUnit.SECONDS);
                    } else {
                        draining.set(false);
                        if (!pending.isEmpty()) drain();
                    }
                });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        if (started.compareAndSet(false, true)) {
            executor.execute(() -> recover(""));
            executor.execute(() -> connect(0));
        }
    }

    private void connect(int attempts) {
        if (closed) return;
        try {
            if (!Boolean.TRUE.equals(dispatch.wakeUp(consumer).get(6, TimeUnit.SECONDS)))
                throw new IllegalStateException("JetLinks did not accept consumer");
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("业务事件推送握手未完成，将重试");
            executor.schedule(
                    () -> connect(attempts + 1),
                    Math.min(60, 1L << Math.min(6, attempts)),
                    TimeUnit.SECONDS);
        }
    }

    private void recover(String afterId) {
        if (closed) return;
        try {
            List<String> ids = taskMapper.selectRecoveryTasks(afterId);
            for (String id : ids) {
                try {
                    runner.advance(id, true);
                } catch (RuntimeException failure) {
                    // Startup reconciliation failure must be retried as reconciliation, not as a
                    // blind resubmit.
                    retryRecovery(id);
                }
            }
            if (ids.size() == 100) executor.execute(() -> recover(ids.get(ids.size() - 1)));
        } catch (RuntimeException failure) {
            executor.schedule(() -> recover(afterId), 5, TimeUnit.SECONDS);
        }
    }

    private void retryRecovery(String id) {
        if (closed) return;
        executor.schedule(
                () -> {
                    try {
                        runner.advance(id, true);
                    } catch (RuntimeException failure) {
                        retryRecovery(id);
                    }
                },
                5,
                TimeUnit.SECONDS);
    }

    @PreDestroy
    public void close() {
        closed = true;
        executor.shutdownNow();
    }
}
