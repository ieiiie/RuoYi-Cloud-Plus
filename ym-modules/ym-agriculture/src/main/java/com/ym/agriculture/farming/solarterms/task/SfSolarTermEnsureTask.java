package com.ym.agriculture.farming.solarterms.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import com.ym.agriculture.farming.solarterms.service.ISfSolarTermService;
import com.ym.agriculture.farming.solarterms.support.SolarTermCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 供后台定时任务调用：预生成当前年及相邻年份节气交节数据。
 * <p>调用目标示例：{@code sfSolarTermEnsureTask.ensureNearbyYears()}
 */
@Slf4j
@Component("sfSolarTermEnsureTask")
@JobExecutor(name = "sfSolarTermEnsureJob", method = "ensureNearbyYears")
@RequiredArgsConstructor
public class SfSolarTermEnsureTask {

    private final ISfSolarTermService solarTermService;
    private final DomainJobExecutorSupport executorSupport;

    public void ensureNearbyYears(JobArgs args) {
        executorSupport.executeAllTenants(args, this::ensureCurrentTenantYears);
    }

    private void ensureCurrentTenantYears() {
        int year = LocalDate.now(SolarTermCalculator.BEIJING).getYear();
        log.info("节气交节数据确保开始，centerYear={}", year);
        solarTermService.ensureYears(year);
        log.info("节气交节数据确保结束，centerYear={}", year);
    }
}
