package com.ym.agriculture.farming.bigscreen.service;

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

import java.util.List;

/**
 * 大屏 BFF 聚合服务。
 */
public interface ISfBigscreenService {

    /**
     * 顶栏与 KPI 聚合。
     */
    SfBigscreenOverviewVo getOverview();

    /**
     * 天气卡片（透传 dashboard + 降雨模式说明）。
     */
    SfBigscreenWeatherVo getWeather();

    /**
     * 地块列表。
     */
    List<SfBigscreenFieldVo> getFields();

    /**
     * 地块弹窗详情。
     *
     * @param fieldId 地块主键
     */
    SfBigscreenFieldDetailVo getFieldDetail(Long fieldId);

    /**
     * 数字孪生地图数据。
     */
    SfBigscreenMapVo getMapData();

    /**
     * 遥感分析轮播。
     *
     * @param fieldId 地块主键
     */
    SfBigscreenRsAnalysisVo getRsAnalysis(Long fieldId);

    /**
     * 最近无人机航拍任务。
     */
    SfBigscreenUavLatestVo getUavLatest();

    /**
     * 传感器实时遥测。
     *
     * @param fieldId 地块主键
     */
    SfBigscreenSensorTelemetryVo getSensorTelemetry(Long fieldId);

    /**
     * 告警信息。
     *
     * @param limit 条数上限
     */
    SfBigscreenAlertVo getAlerts(int limit);

    /**
     * 农事操作时间轴（仅已提交农事记录）。
     *
     * @param limit 条数上限
     */
    List<SfBigscreenTimelineItemVo> getFarmOperations(int limit);

    /**
     * 水肥机状态与历史。
     *
     * @param deviceCode 设备编号
     */
    SfBigscreenFertilizerVo getFertilizer(String deviceCode);

    /**
     * 阀门汇总。
     */
    SfBigscreenValveSummaryVo getValveSummary();
}
