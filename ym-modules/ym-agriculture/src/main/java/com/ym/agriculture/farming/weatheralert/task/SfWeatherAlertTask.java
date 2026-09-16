package com.ym.agriculture.farming.weatheralert.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import com.ym.agriculture.farming.weatheralert.service.ISfWeatherAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 供 Snail Job 调用：按启用租户省份同步中央气象台公开预警。
 * <p>调用目标：{@code sfWeatherAlertTask.syncEnabledProvinces()}
 */
@Slf4j
@Component("sfWeatherAlertTask")
@RequiredArgsConstructor
@JobExecutor(name = "sfWeatherAlertSyncJob", method = "syncEnabledProvinces")
public class SfWeatherAlertTask {

    private final ISfWeatherAlertService weatherAlertService;
    private final DomainJobExecutorSupport executorSupport;

    public void syncEnabledProvinces(JobArgs args) {
        executorSupport.executeGlobal(() -> {
            log.info("气象预警定时同步开始");
            weatherAlertService.syncEnabledProvinces();
            log.info("气象预警定时同步结束");
        });
    }
}
