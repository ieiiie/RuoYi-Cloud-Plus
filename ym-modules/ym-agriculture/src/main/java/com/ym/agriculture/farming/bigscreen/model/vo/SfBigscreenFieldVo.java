package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 大屏地块列表项。
 */
@Data
public class SfBigscreenFieldVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 地块主键 */
    private Long fieldId;

    /** 地块名称 */
    private String fieldName;

    /** 面积，单位：亩 */
    private BigDecimal areaMu;

    /** 地图展示状态 */
    private String mapDisplayStatus;

    /** 活跃种植批次 ID */
    private Long activeBatchId;

    /** 作物物种名称 */
    private String cropSpeciesName;

    /** 作物品种名称 */
    private String cropVarietyName;
}
