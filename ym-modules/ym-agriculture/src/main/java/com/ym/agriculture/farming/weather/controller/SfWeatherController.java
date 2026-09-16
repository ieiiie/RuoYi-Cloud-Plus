package com.ym.agriculture.farming.weather.controller;

import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherLatestVo;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherSyncResultVo;
import com.ym.agriculture.farming.weather.service.ISfWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 高德天气：预报由定时任务同步（{@code extensions=all}）；查询时预报读库、实况按日时段按需拉取（{@code extensions=base}）。
 * <p>
 * 官方文档：<a href="https://lbs.amap.com/api/webservice/guide/api-advanced/weatherinfo">天气查询 Web 服务 API</a>。
 * 请求参数 {@code city} 使用行政区划 adcode。返回体 {@code forecasts[0].casts} 中：<b>第 1 条为当天</b>，其后为<b>未来数日</b>（常见为再 3 天，共 4 条）。
 * <p>
 * <b>基础路径</b> {@code /smart-farming/weather}，需登录（与地块等接口一致）；天气表按 adcode 共享，无 tenant_id。
 * 预报每次同步成功写入前会 <b>删除该 adcode 下 {@code cast_date >= 今天} 的预报行再插入</b>，今天之前的历史行保留。
 * 实况按默认 <b>6/12/18/0 点</b> 划分日时段，{@code pull_time} 在当前时段内则读库，否则查询时自动拉高德并落库。
 * <p>
 * <b>1. 同步预报（供定时任务 / 管理端）</b>
 * <ul>
 *     <li>{@code POST /smart-farming/weather/sync?adcode=500157}</li>
 *     <li>Query {@code adcode}：必填，高德区域编码。</li>
 *     <li>成功响应 {@link R}{@code <}{@link SfWeatherSyncResultVo}{@code >}：{@code syncId}、{@code adcode}、{@code forecastRows}</li>
 * </ul>
 * <p>
 * <b>定时同步预报</b>：由 Snail Job 维护，执行器
 * {@code sfWeatherForecastSyncJob}；按各有效租户 {@code sys_tenant} 区县/市/省编码拉取。
 * <p>
 * <b>2. 查询最新</b>
 * <ul>
 *     <li>{@code GET /smart-farming/weather/latest}</li>
 *     <li>按当前登录用户租户 {@code sys_tenant.district_code} 解析 adcode；返回 {@link SfWeatherLatestVo}（{@code forecasts} + {@code live}）</li>
 * </ul>
 * <p>
 * <b>配置（ym-app）</b>：根节点 {@code amap-weather}，至少配置 {@code key}；可选 {@code live-refresh-hours: [6,12,18,0]}。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/weather")
public class SfWeatherController extends BaseController {

    private final ISfWeatherService weatherService;

    /**
     * 从高德拉取预报并写入 {@code sf_weather_forecast}（单次请求 {@code extensions=all}，供定时任务使用）
     *
     * @param adcode 高德行政区划 adcode
     * @return 统一响应，{@code data} 含同步批次、行数等，见 {@link SfWeatherSyncResultVo}
     */
    @Log(title = "高德天气预报同步", businessType = BusinessType.OTHER)
    @PostMapping("/sync")
    public R<SfWeatherSyncResultVo> sync(@RequestParam String adcode) {
        return R.ok(weatherService.syncFromAmap(adcode));
    }

    /**
     * 查询当前租户区县对应 adcode 的天气：预报读库，实况按日时段边界按需拉取
     *
     * @return 统一响应，{@code data} 为最新天气视图，见 {@link SfWeatherLatestVo}
     */
    @GetMapping("/latest")
    public R<SfWeatherLatestVo> latest() {
        return R.ok(weatherService.getLatest());
    }
}
