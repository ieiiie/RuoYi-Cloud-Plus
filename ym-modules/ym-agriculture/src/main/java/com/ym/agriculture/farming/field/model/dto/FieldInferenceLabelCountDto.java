package com.ym.agriculture.farming.field.model.dto;

import lombok.Data;

/**
 * MyBatis 映射：按 label 计数。
 */
@Data
public class FieldInferenceLabelCountDto {

    private String label;
    private Long cnt;
}
