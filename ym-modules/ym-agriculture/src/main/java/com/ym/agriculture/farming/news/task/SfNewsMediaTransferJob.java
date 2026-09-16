package com.ym.agriculture.farming.news.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import com.ym.agriculture.farming.news.service.ISfNewsMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 农业资讯媒体转存队列的 Snail Job 补偿入口。 */
@Slf4j
@Component
@RequiredArgsConstructor
@JobExecutor(name = "sfNewsMediaTransferJob", method = "processPending")
public class SfNewsMediaTransferJob {

    private final ISfNewsMediaService mediaService;
    private final DomainJobExecutorSupport executorSupport;

    public void processPending(JobArgs args) {
        executorSupport.executeGlobal(() -> {
            int rounds = 0;
            int processed = 0;
            int current;
            do {
                current = mediaService.processPending();
                processed += current;
            } while (current > 0 && ++rounds < 100);
            if (processed > 0) {
                log.info("农业资讯媒体转存补偿扫描处理 {} 条", processed);
            }
        });
    }
}
