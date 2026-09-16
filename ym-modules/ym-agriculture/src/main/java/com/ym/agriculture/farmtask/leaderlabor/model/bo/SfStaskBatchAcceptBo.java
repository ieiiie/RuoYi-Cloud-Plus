package com.ym.agriculture.farmtask.leaderlabor.model.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 批量接单请求。 */
@Data
public class SfStaskBatchAcceptBo {
    /** 任务计划日期，格式 yyyy-MM-dd。 */
    @NotNull
    private LocalDate planDate;
    /** 待接单工单ID集合。 */
    @NotEmpty
    private List<Long> orderIds;
    /** 该计划日总用工人数，不含组长。 */
    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal dailyLaborCount;
    /** 已有日用工记录的乐观锁版本；首次创建时为空。 */
    private Long laborRecordVersion;
    /** 客户端幂等键。 */
    @NotBlank
    private String idempotencyKey;
}
