package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 大屏地块弹窗详情。
 */
@Data
public class SfBigscreenFieldDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 地块主键 */
    private Long fieldId;

    /** 地块名称 */
    private String fieldName;

    /** 面积，单位：亩 */
    private BigDecimal areaMu;

    /** 作物物种名称 */
    private String cropSpeciesName;

    /** 作物品种名称 */
    private String cropVarietyName;

    /** 传感器遥测 */
    private SfBigscreenSensorTelemetryVo telemetry;

    /** 最近一条农事记录 */
    private SfBigscreenTimelineItemVo recentWork;
}
