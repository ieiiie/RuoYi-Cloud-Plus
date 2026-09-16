package com.ym.agriculture.farmtask.yieldrecord.model.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 批量新增产量。
 */
@Data
public class SfStaskYieldBatchCreateBo {

    @NotNull(message = "请选择收获日期")
    private LocalDate harvestDate;

    @Valid
    @NotEmpty(message = "请至少添加一条产量明细")
    private List<SfStaskYieldDetailBo> details;
}
