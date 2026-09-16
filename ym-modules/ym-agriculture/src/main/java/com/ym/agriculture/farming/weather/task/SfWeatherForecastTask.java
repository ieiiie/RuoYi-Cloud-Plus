package com.ym.agriculture.farming.weather.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.weather.config.AmapWeatherProperties;
import com.ym.agriculture.farming.weather.service.ISfWeatherService;
import com.ym.system.api.RemoteTenantService;
import com.ym.system.api.domain.vo.RemoteTenantInfoVo;
import org.apache.dubbo.config.annotation.DubboReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * 供 Snail Job 调用：按租户行政区划拉取高德预报并落库；同一 {@code adcode} 每次成功同步前删除旧行。
 * <p>
 * 调用目标示例：{@code sfWeatherForecastTask.syncAllTenantRegions()}（与 {@link com.ym.system.job.support.JobInvoker} 约定一致）。
 * adcode：优先 {@code districtCode}，其次 {@code cityCode}，再次 {@code provinceCode}；仅处理租户状态为正常的记录。
 * 多个租户若省/市/区（规范化后）一致，视为同一「租户区域」，只请求一次高德；最终请求列表再按 adcode 去重，避免同区域重复拉取。
 * 批量同步时相邻 adcode 之间间隔 1 秒，降低高德 {@code CUQPS_HAS_EXCEEDED_THE_LIMIT} 触发概率。
 *
 * @author ym-cloud
 */
@Slf4j
@Component("sfWeatherForecastTask")
@RequiredArgsConstructor
@JobExecutor(name = "sfWeatherForecastSyncJob", method = "syncTenantRegion")
public class SfWeatherForecastTask {

    private final AmapWeatherProperties amapWeatherProperties;
    private final ISfWeatherService weatherService;
    private final DomainJobExecutorSupport executorSupport;

    @DubboReference
    private RemoteTenantService tenantService;

    /**
     * Snail Job 入口；开关与 Key 见 {@code amap-weather}。
     */
    public void syncTenantRegion(JobArgs args) {
        executorSupport.executeAllTenants(args, this::syncCurrentTenantRegion);
    }

    private void syncCurrentTenantRegion() {
        if (!amapWeatherProperties.isEnabled()) {
            log.debug("跳过天气定时同步：amap-weather.enabled=false");
            return;
        }
        if (StringUtils.isBlank(amapWeatherProperties.getKey())) {
            log.warn("跳过天气定时同步：未配置 amap-weather.key");
            return;
        }
        RemoteTenantInfoVo tenant = tenantService.getTenant(TenantHelper.getTenantId());
        String adcode = tenant == null ? null : normalizeWeatherAdcode(resolveWeatherAdcode(tenant));
        if (StringUtils.isBlank(adcode)) {
            log.info("天气定时同步：当前租户无可用区划 adcode，跳过");
            return;
        }
        weatherService.syncFromAmap(adcode);
        log.info("天气定时同步结束 tenantId={} adcode={}", TenantHelper.getTenantId(), adcode);
    }

    /**
     * 租户行政区划维度键（规范化后的省、市、区拼接），用于多租户共享同一区域时合并为一次同步。
     */
    private static String resolveWeatherAdcode(RemoteTenantInfoVo t) {
        String d = normalizeRegionCode(t.getDistrictCode());
        if (StringUtils.isNotBlank(d)) {
            return d;
        }
        String c = normalizeRegionCode(t.getCityCode());
        if (StringUtils.isNotBlank(c)) {
            return c;
        }
        return normalizeRegionCode(t.getProvinceCode());
    }

    /**
     * 区划/adcode 字符串：去空白；纯数字则去掉前导零，避免同一区域多种写法导致重复请求。
     */
    private static String normalizeRegionCode(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replaceAll("\\s+", "");
        if (s.isEmpty()) {
            return null;
        }
        if (s.chars().allMatch(Character::isDigit)) {
            int i = 0;
            while (i < s.length() - 1 && s.charAt(i) == '0') {
                i++;
            }
            return s.substring(i);
        }
        return s;
    }

    private static String normalizeWeatherAdcode(String ad) {
        return normalizeRegionCode(ad);
    }

}
