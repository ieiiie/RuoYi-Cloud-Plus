package com.ym.agriculture.farmtask.yieldrecord.model.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单条产量明细。
 */
@Data
public class SfStaskYieldDetailBo {

    @NotNull(message = "请选择物种")
    private Long speciesId;

    @NotNull(message = "请选择品种")
    private Long varietyId;

    @NotNull(message = "请输入产量")
    @DecimalMin(value = "0.01", message = "产量必须大于0")
    @Digits(integer = 12, fraction = 2, message = "产量最多保留两位小数")
    private BigDecimal yieldKg;
}
