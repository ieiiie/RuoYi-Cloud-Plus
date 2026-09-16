package com.ym.agriculture.farmtask.inspectionaichat.service.impl;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.farmtask.inspectionaichat.config.SfInspectionAiProperties;
import com.ym.agriculture.farmtask.inspectionaichat.service.ISfInspectionAiChatService;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 标记应用重启或网络故障遗留的 AI 超时任务。 */
@Slf4j
@Component
@RequiredArgsConstructor
@JobExecutor(name = "sfInspectionAiRecoveryJob", method = "cleanup")
public class SfInspectionAiRecoveryWorker {
    private final ISfInspectionAiChatService chatService;
    private final SfInspectionAiProperties properties;
    private final DomainJobExecutorSupport executorSupport;

    /** 由 Snail Job 遍历有效租户执行，不再在服务启动或本地定时器中扫描。 */
    public void cleanup(JobArgs args) {
        executorSupport.executeAllTenants(args, this::cleanupInTenant);
    }

    private void cleanupInTenant() {
        if (!properties.isEnabled()) return;
        int changed = chatService.failStaleGenerations();
        if (changed > 0) log.warn("已标记 {} 条超时巡查 AI 生成任务", changed);
    }
}
