package com.ym.agriculture.shared.i18n.service.impl;

import com.ym.agriculture.shared.i18n.config.SmartFarmingTranslationProperties;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/** 业务文本翻译后台 Worker。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SfI18nTextWorker {

    private final ISfI18nTextService textService;
    private final SmartFarmingTranslationProperties properties;

    private volatile ExecutorService executor;
    private final AtomicBoolean wakeRequested = new AtomicBoolean();
    private final AtomicBoolean wakeRunnerActive = new AtomicBoolean();

    /** 启动仅用于事务提交后即时处理的单线程执行器。 */
    @PostConstruct
    public void start() {
        if (!properties.isEnabled() || !properties.getWorker().isEnabled()) {
            log.info("业务文本翻译Worker未启动 translationEnabled={}, workerEnabled={}",
                properties.isEnabled(), properties.getWorker().isEnabled());
            return;
        }
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "sf-i18n-translation-worker");
            thread.setDaemon(true);
            return thread;
        });
        log.info("业务文本翻译即时处理器已启动 batchSize={}",
            Math.max(1, properties.getWorker().getBatchSize()));
    }

    /** 停止即时处理执行器。 */
    @PreDestroy
    public void stop() {
        ExecutorService current = executor;
        executor = null;
        if (current != null) {
            current.shutdownNow();
        }
    }

    /**
     * 合并登记提交产生的并发唤醒。Worker 未启用或应用正在关闭时，由 Snail Job 扫描兜底。
     */
    public void wake() {
        ExecutorService current = executor;
        if (current == null || current.isShutdown()) {
            return;
        }
        wakeRequested.set(true);
        if (wakeRunnerActive.compareAndSet(false, true)) {
            try {
                current.execute(this::drainWakeRequests);
            } catch (RejectedExecutionException ignored) {
                wakeRunnerActive.set(false);
                // 应用正在关闭；已提交数据由 Snail Job 扫描兜底。
            }
        }
    }

    private void drainWakeRequests() {
        try {
            do {
                wakeRequested.set(false);
                runOnce();
            } while (wakeRequested.get());
        } finally {
            wakeRunnerActive.set(false);
            if (wakeRequested.get()) {
                wake();
            }
        }
    }

    private void runOnce() {
        try {
            textService.processPending();
        } catch (Exception e) {
            log.warn("业务文本翻译Worker扫描失败: {}", e.getClass().getSimpleName(), e);
        }
    }
}
