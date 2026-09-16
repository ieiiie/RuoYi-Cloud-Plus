package com.ym.agriculture.farmtask.leaderlabor.model.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 日用工人数修改请求。 */
@Data
public class SfStaskLaborRecordUpdateBo {
    /** 新日用工人数，不含组长。 */
    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal dailyLaborCount;
    /** 当前详情返回的乐观锁版本。 */
    @NotNull
    private Long version;
}
