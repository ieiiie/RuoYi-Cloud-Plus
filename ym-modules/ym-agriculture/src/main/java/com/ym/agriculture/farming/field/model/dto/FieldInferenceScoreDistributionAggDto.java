package com.ym.agriculture.farming.field.model.dto;

import lombok.Data;

/**
 * MyBatis 单行映射：置信度在 [0,1] 五分桶 + 桶外（可解析但落在 [0,1] 外）条数。
 */
@Data
public class FieldInferenceScoreDistributionAggDto {

    private Long b00;
    private Long b02;
    private Long b04;
    private Long b06;
    private Long b08;
    private Long bOther;
}
