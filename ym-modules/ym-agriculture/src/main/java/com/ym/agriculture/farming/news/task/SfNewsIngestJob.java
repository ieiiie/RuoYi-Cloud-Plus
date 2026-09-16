package com.ym.agriculture.farming.news.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import com.ym.agriculture.farming.news.model.vo.SfNewsIngestProcessVo;
import com.ym.agriculture.farming.news.service.ISfNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 供后台定时任务调用的农业资讯暂存处理入口。 */
@Slf4j
@Component("sfNewsIngestJob")
@JobExecutor(name = "sfNewsIngestJob", method = "processPending")
@RequiredArgsConstructor
public class SfNewsIngestJob {

    private final ISfNewsService newsService;
    private final DomainJobExecutorSupport executorSupport;

    /**
     * 每次最多处理 50 条。
     */
    public void processPending(JobArgs args) {
        executorSupport.executeGlobal(this::processPendingRows);
    }

    private void processPendingRows() {
        SfNewsIngestProcessVo result = newsService.processPending(50);
        log.info("农业资讯暂存处理完成 claimed={}, succeeded={}, rejected={}, failed={}",
            result.getClaimed(), result.getSucceeded(), result.getRejected(), result.getFailed());
    }
}
