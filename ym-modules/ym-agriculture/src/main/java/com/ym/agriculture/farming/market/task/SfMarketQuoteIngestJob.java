package com.ym.agriculture.farming.market.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestProcessVo;
import com.ym.agriculture.farming.market.service.ISfMarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 供任务中心调用的农业行情采集处理入口。 */
@Slf4j
@Component("sfMarketQuoteIngestJob")
@JobExecutor(name = "sfMarketQuoteIngestJob", method = "processPending")
@RequiredArgsConstructor
public class SfMarketQuoteIngestJob {

    private final ISfMarketService marketService;
    private final DomainJobExecutorSupport executorSupport;

    /** 每次最多处理50条待处理行情。 */
    public void processPending(JobArgs args) {
        executorSupport.executeGlobal(this::processPendingRows);
    }

    private void processPendingRows() {
        SfMarketIngestProcessVo result = marketService.processPending(50);
        log.info("农业行情暂存处理完成 claimed={}, created={}, updated={}, unchanged={}, rejected={}, failed={}, warnings={}",
            result.getClaimed(), result.getCreated(), result.getUpdated(), result.getUnchanged(),
            result.getRejected(), result.getFailed(), result.getWarnings());
    }
}
