package com.ym.agriculture.farming.field.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * MyBatis 映射：按 label 统计（含分数聚合）。
 */
@Data
public class FieldInferenceLabelStatRowDto {

    private String label;
    private Long cnt;
    private BigDecimal avgScore;
    private BigDecimal maxScore;
}
