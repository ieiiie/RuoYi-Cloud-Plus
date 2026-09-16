package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 遥感分析等级面积项。
 */
@Data
public class SfBigscreenRsLevelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 等级序号，1–5 */
    private Integer levelIndex;

    /** 等级名称 */
    private String levelName;

    /** 面积，单位：亩 */
    private BigDecimal area;

    /** 是否有数据 */
    private Boolean hasData;
}
