package com.ym.agriculture.farming.batch.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

/**
 * 种植批次状态流转请求。
 */
@Data
public class SfPlantingBatchTransitionBo {

    @NotBlank(message = "目标状态不能为空")
    private String toStatus;

    @Size(max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;

    private Date actualHarvestDate;
}
