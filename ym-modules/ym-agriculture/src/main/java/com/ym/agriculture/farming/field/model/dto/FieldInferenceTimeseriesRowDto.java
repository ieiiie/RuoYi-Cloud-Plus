package com.ym.agriculture.farming.field.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * MyBatis 映射：时间桶聚合。
 */
@Data
public class FieldInferenceTimeseriesRowDto {

    private String bucketStart;
    private Long cnt;
    private BigDecimal avgScore;
}
