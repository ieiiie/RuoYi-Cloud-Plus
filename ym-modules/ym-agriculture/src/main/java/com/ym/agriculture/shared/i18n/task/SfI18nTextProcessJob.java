package com.ym.agriculture.shared.i18n.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.shared.i18n.config.SmartFarmingTranslationProperties;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 业务文本翻译队列的 Snail Job 补偿扫描入口。 */
@Slf4j
@Component
@RequiredArgsConstructor
@JobExecutor(name = "sfI18nTextProcessJob", method = "processPending")
public class SfI18nTextProcessJob {

    private final ISfI18nTextService textService;
    private final SmartFarmingTranslationProperties properties;
    private final DomainJobExecutorSupport executorSupport;

    public void processPending(JobArgs args) {
        executorSupport.executeGlobal(() -> {
            if (!properties.isEnabled() || !properties.getWorker().isEnabled()) {
                return;
            }
            int claimed = textService.processPending();
            if (claimed > 0) {
                log.info("业务文本翻译补偿扫描领取 {} 条", claimed);
            }
        });
    }
}
