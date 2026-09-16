package com.ym.agriculture.farming.news.service.impl;

import com.ym.agriculture.farming.news.config.SfNewsMediaProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SfNewsMediaWorker {
    private final ObjectProvider<SfNewsMediaServiceImpl> mediaService;
    private final SfNewsMediaProperties properties;
    private final AtomicBoolean requested = new AtomicBoolean();
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile ExecutorService executor;

    @PostConstruct public void start() {
        if (!properties.isEnabled()) return;
        executor = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "sf-news-media-worker"); t.setDaemon(true); return t; });
    }
    @PreDestroy public void stop() { if (executor != null) executor.shutdownNow(); }
    public void wake() {
        if (executor == null || executor.isShutdown()) return;
        requested.set(true);
        if (running.compareAndSet(false, true)) try { executor.execute(this::drain); }
        catch (RejectedExecutionException e) { running.set(false); }
    }
    private void drain() { try { do { requested.set(false); runOnce(); } while (requested.get()); } finally { running.set(false); if (requested.get()) wake(); } }
    private void runOnce() {
        try {
            int rounds = 0;
            while (mediaService.getObject().processPending() > 0 && ++rounds < 100) { /* drain */ }
        } catch (Exception e) { log.warn("农业资讯媒体Worker执行失败", e); }
    }
}
