package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 大屏遥感分析轮播。
 */
@Data
public class SfBigscreenRsAnalysisVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 地块主键 */
    private Long fieldId;

    /** 地块名称 */
    private String fieldName;

    /** 种植批次 ID */
    private Long plantingBatchId;

    /** 地块中心经度；未维护时为空 */
    private BigDecimal centerLng;

    /** 地块中心纬度；未维护时为空 */
    private BigDecimal centerLat;

    /** 轮播卡片列表，最多 10 项 */
    private List<SfBigscreenRsAnalysisItemVo> items = new ArrayList<>();
}
