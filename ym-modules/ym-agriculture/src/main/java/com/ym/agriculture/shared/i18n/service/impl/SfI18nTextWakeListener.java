package com.ym.agriculture.shared.i18n.service.impl;

import com.ym.agriculture.shared.i18n.event.I18nTextsRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 在登记事务提交后合并唤醒翻译 Worker。 */
@Component
@RequiredArgsConstructor
public class SfI18nTextWakeListener {

    private final SfI18nTextWorker worker;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRegistered(I18nTextsRegisteredEvent event) {
        worker.wake();
    }
}
