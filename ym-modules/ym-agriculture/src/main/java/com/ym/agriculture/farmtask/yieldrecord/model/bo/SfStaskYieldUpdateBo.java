package com.ym.agriculture.farmtask.yieldrecord.model.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 编辑单条产量记录。
 */
@Data
public class SfStaskYieldUpdateBo {

    @NotNull(message = "请选择收获日期")
    private LocalDate harvestDate;

    @Valid
    @NotNull(message = "请填写产量明细")
    private SfStaskYieldDetailBo detail;
}
