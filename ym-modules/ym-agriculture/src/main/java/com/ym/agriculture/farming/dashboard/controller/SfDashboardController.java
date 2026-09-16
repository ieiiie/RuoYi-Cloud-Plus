package com.ym.agriculture.farming.dashboard.controller;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.iot.api.domain.vo.RemoteSeriesVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchDashboardStatsVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardMapDataVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardSensorSummaryVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardWeatherCardVo;
import com.ym.agriculture.farming.dashboard.service.ISfDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 智慧农业首页仪表盘
 * <p>
 * 路径前缀 {@code /smart-farming/dashboard}，需登录；数据范围与地块/批次列表一致（非超管仅本人负责地块）。
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/dashboard")
public class SfDashboardController extends BaseController {

    private final ISfDashboardService dashboardService;

    /**
     * 农田地图数据：地块边界、{@code mapDisplayStatus}、进行中批次 ID、传感器锚点
     *
     * @return 统一响应，{@code data} 为 {@link SfDashboardMapDataVo}
     */
    @GetMapping("/map-data")
    public R<SfDashboardMapDataVo> mapData() {
        return R.ok(dashboardService.getMapData());
    }

    /**
     * 天气概况（读库；无数据时需先由定时任务或 {@code POST /smart-farming/weather/sync} 完成同步）
     *
     * @return 统一响应，{@code data} 含当天与后续预报列表（adcode 来自当前租户区县编码）
     */
    @GetMapping("/weather")
    public R<SfDashboardWeatherCardVo> weather() {
        return R.ok(dashboardService.getWeatherCard());
    }

    /**
     * 传感器状态树形汇总。
     * <p>
     * 返回两级树形结构：一级为设备大类（从字典 {@code iot_device_type} 动态获取展示名），
     * 二级为产品（按 IoT 产品表分组），每种产品下展示具体设备明细及在线/离线数量。
     * </p>
     * <p>
     * 任一设备离线，对应产品及大类节点均标记红灯（{@code hasOffline=true}）。
     * 无人机为虚拟设备，不在物联网表中，后端归入"其它设备"大类。
     * </p>
     *
     * @return 统一响应，{@code data} 为 {@link SfDashboardSensorSummaryVo}（{@code categories} 树形结构）
     */
    @GetMapping("/sensor-summary")
    public R<SfDashboardSensorSummaryVo> sensorSummary() {
        return R.ok(dashboardService.getSensorSummary());
    }

    /**
     * 地块状态概览：各种植批次状态数量
     *
     * @return 统一响应，{@code data} 为 {@link SfPlantingBatchDashboardStatsVo}
     */
    @GetMapping("/batch-status-stats")
    public R<SfPlantingBatchDashboardStatsVo> batchStatusStats() {
        return R.ok(dashboardService.getBatchStatusStats());
    }

    /**
     * 传感器历史曲线（弹窗折线图）；设备须绑定在当前用户权限内地块。
     * <p>
     * 测点编码与 IoT 产品物模型一致，同 {@code GET /iot/device/{deviceId}/series}。
     *
     * @param deviceId    物联网设备主键（路径中 {@code sensorId} 与设备 ID 一致）
     * @param metric_code 指标编码，必填
     * @param from        起始时间，可选
     * @param to          结束时间，可选
     * @param step        聚合步长（秒），可选
     * @return 统一响应，{@code data} 为时序点列表
     */
    @GetMapping("/sensor/{deviceId}/series")
    public R<RemoteSeriesVo> sensorSeries(
        @PathVariable("deviceId") Long deviceId,
        @RequestParam("metric_code") String metricCode,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date from,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date to,
        @RequestParam(required = false) Integer step) {
        return R.ok(dashboardService.getSensorSeries(deviceId, metricCode, from, to, step));
    }
}
