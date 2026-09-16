package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 遥感分析单类型卡片。
 */
@Data
public class SfBigscreenRsAnalysisItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析类型编码，如 growth */
    private String taskType;

    /** 分析类型中文名 */
    private String taskTypeDesc;

    /** 影像日期 */
    private String imageDate;

    /** 预览图 OSS URL */
    private String imageUrl;

    /** 有效覆盖总面积，单位：亩 */
    private BigDecimal totalArea;

    /** 等级面积列表 */
    private List<SfBigscreenRsLevelVo> levels = new ArrayList<>();
}
