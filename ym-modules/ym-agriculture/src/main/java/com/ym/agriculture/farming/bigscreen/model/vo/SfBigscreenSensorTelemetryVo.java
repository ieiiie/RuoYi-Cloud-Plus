package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 大屏 M05 传感器实时遥测。
 */
@Data
public class SfBigscreenSensorTelemetryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 地块主键 */
    private Long fieldId;

    /** 农业传感器在线数 */
    private Integer sensorOnlineCount;

    private Integer sensorOfflineCount;

    private Integer sensorUnknownCount;

    /** 农业传感器总数 */
    private Integer sensorTotalCount;

    /** 气象仪分组 */
    private SfBigscreenSensorGroupVo weather;

    /** 土壤墒情分组 */
    private SfBigscreenSensorGroupVo soil;

    /** 虫情分组 */
    private SfBigscreenSensorGroupVo pest;
}
