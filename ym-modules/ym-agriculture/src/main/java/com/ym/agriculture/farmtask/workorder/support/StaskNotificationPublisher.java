package com.ym.agriculture.farmtask.workorder.support;

import com.ym.agriculture.shared.i18n.BilingualContent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * stask 通知事件发布器。
 */
@Component
@RequiredArgsConstructor
public class StaskNotificationPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布一条在事务提交后交付的通知。
     *
     * @param scene 通知场景
     * @param targetId 目标员工 ID
     * @param content 双语正文
     */
    public void publish(String scene, Long targetId, BilingualContent content) {
        StaskNotificationEvent event = new StaskNotificationEvent(scene, targetId, content);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(event);
            }
        });
    }
}
