package com.ym.agriculture.farming.bigscreen.controller;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.bigscreen.config.BigscreenCaliber;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenAlertVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenFertilizerVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenFieldDetailVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenFieldVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenMapVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenOverviewVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenRsAnalysisVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenSensorTelemetryVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenTimelineItemVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenUavLatestVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenValveSummaryVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenWeatherVo;
import com.ym.agriculture.farming.bigscreen.service.ISfBigscreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智慧农业数据大屏 BFF。
 * <p>路径前缀 {@code /smart-farming/bigscreen}，需登录。</p>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/bigscreen")
public class SfBigscreenController extends BaseController {

    private final ISfBigscreenService bigscreenService;

    /**
     * 顶栏与 KPI 聚合（M01 + 顶栏）。
     */
    @GetMapping("/overview")
    public R<SfBigscreenOverviewVo> overview() {
        return R.ok(bigscreenService.getOverview());
    }

    /**
     * 天气卡片，附加降雨展示模式说明。
     */
    @GetMapping("/weather")
    public R<SfBigscreenWeatherVo> weather() {
        return R.ok(bigscreenService.getWeather());
    }

    /**
     * 地块列表。
     */
    @GetMapping("/fields")
    public R<List<SfBigscreenFieldVo>> fields() {
        return R.ok(bigscreenService.getFields());
    }

    /**
     * 地块弹窗详情。
     *
     * @param fieldId 地块主键
     */
    @GetMapping("/fields/{fieldId}")
    public R<SfBigscreenFieldDetailVo> fieldDetail(@PathVariable Long fieldId) {
        return R.ok(bigscreenService.getFieldDetail(fieldId));
    }

    /**
     * 数字孪生地图（地块 + 全租户设备图层）。
     */
    @GetMapping("/map")
    public R<SfBigscreenMapVo> map() {
        return R.ok(bigscreenService.getMapData());
    }

    /**
     * 遥感分析轮播（M02）。
     *
     * @param fieldId 地块主键
     */
    @GetMapping("/rs-analysis")
    public R<SfBigscreenRsAnalysisVo> rsAnalysis(@RequestParam Long fieldId) {
        return R.ok(bigscreenService.getRsAnalysis(fieldId));
    }

    /**
     * 最近无人机航拍任务（M03）。
     */
    @GetMapping("/uav/latest")
    public R<SfBigscreenUavLatestVo> uavLatest() {
        return R.ok(bigscreenService.getUavLatest());
    }

    /**
     * 传感器实时遥测（M05），按 AG_SENSOR + product_key 子类型输出。
     *
     * @param fieldId 地块主键
     */
    @GetMapping("/sensors/telemetry")
    public R<SfBigscreenSensorTelemetryVo> sensorTelemetry(@RequestParam Long fieldId) {
        return R.ok(bigscreenService.getSensorTelemetry(fieldId));
    }

    /**
     * 报警信息（M06）。
     *
     * @param limit 条数上限，默认 20
     */
    @GetMapping("/alerts")
    public R<SfBigscreenAlertVo> alerts(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(bigscreenService.getAlerts(limit));
    }

    /**
     * 农事操作时间轴（M07），仅已提交农事记录。
     *
     * @param limit 条数上限，默认 20
     */
    @GetMapping("/farm-operations")
    public R<List<SfBigscreenTimelineItemVo>> farmOperations(
        @RequestParam(defaultValue = "" + BigscreenCaliber.TIMELINE_DEFAULT_LIMIT) int limit) {
        return R.ok(bigscreenService.getFarmOperations(limit));
    }

    /**
     * 水肥机状态与历史（M08）。
     *
     * @param deviceCode 设备编号
     */
    @GetMapping("/fertilizers/{deviceCode}")
    public R<SfBigscreenFertilizerVo> fertilizer(@PathVariable String deviceCode) {
        return R.ok(bigscreenService.getFertilizer(deviceCode));
    }

    /**
     * 阀门汇总（M09）。
     */
    @GetMapping("/valves/summary")
    public R<SfBigscreenValveSummaryVo> valveSummary() {
        return R.ok(bigscreenService.getValveSummary());
    }
}
