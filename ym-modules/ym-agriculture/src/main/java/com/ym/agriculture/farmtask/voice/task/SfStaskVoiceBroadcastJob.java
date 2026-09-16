package com.ym.agriculture.farmtask.voice.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import com.ym.agriculture.farmtask.voice.config.YmStaskVoiceProperties;
import com.ym.agriculture.farmtask.voice.service.ISfStaskVoiceBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** stask 语音播报持久化生成的 Snail Job 入口。 */
@Slf4j
@Component
@RequiredArgsConstructor
@JobExecutor(name = "sfStaskVoiceBroadcastJob", method = "processPending")
public class SfStaskVoiceBroadcastJob {

    private final ISfStaskVoiceBroadcastService voiceBroadcastService;
    private final YmStaskVoiceProperties properties;
    private final DomainJobExecutorSupport executorSupport;

    public void processPending(JobArgs args) {
        executorSupport.executeGlobal(() -> {
            if (!properties.isEnabled() || !properties.getWorker().isEnabled()) {
                return;
            }
            int processed = voiceBroadcastService.processPendingBroadcasts();
            if (processed > 0) {
                log.info("stask 语音播报扫描领取 {} 条", processed);
            }
        });
    }
}
