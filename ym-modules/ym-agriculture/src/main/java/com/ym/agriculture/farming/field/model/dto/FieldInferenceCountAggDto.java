package com.ym.agriculture.farming.field.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * MyBatis 映射：地块推理计数聚合行（含时间范围内可解析置信度统计）。
 */
@Data
public class FieldInferenceCountAggDto {

    private Long cnt;
    private Long distinctTaskNo;
    private Long distinctUavJobId;

    /** {@code cls_score_value} 可解析为数值的条数 */
    private Long scoreParsedCount;

    /** 可解析样本上的平均置信度 */
    private BigDecimal rangeAvgScore;

    private BigDecimal rangeMaxScore;

    private BigDecimal rangeMinScore;
}
