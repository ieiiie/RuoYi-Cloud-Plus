package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 大屏地块 KPI 汇总。
 */
@Data
public class SfBigscreenFieldStatsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 地块数量 */
    private Integer fieldCount;

    /** 总面积，单位：亩 */
    private BigDecimal totalAreaMu;

    /** 种植批次总数 */
    private Long batchCount;

    /** 主力作物名称 */
    private String primaryCropName;

    /** 主力作物品种名称 */
    private String primaryVarietyName;
}
