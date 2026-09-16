package com.ym.agriculture.farmtask.workorder.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 在业务事务提交后调用现有通知扩展点。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaskNotificationListener {

    private final SfStaskNotifyService notifyService;

    /**
     * 交付已提交事务产生的通知；无事务调用使用 fallback 立即交付。
     *
     * @param event 通知事件
     */
    @EventListener
    public void onNotification(StaskNotificationEvent event) {
        try {
            notifyService.deliver(event.scene(), event.targetId(), event.content());
        } catch (RuntimeException e) {
            log.warn("stask提交后通知交付失败 scene={}, targetId={}, errorType={}",
                event.scene(), event.targetId(), e.getClass().getSimpleName());
        }
    }
}
