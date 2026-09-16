package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * stask 组长接单入参。
 */
@Data
public class SfStaskLeaderAcceptBo {

    /**
     * 工人数量，不包含组长本人，作为后续组长费用结算的人工依据。
     */
    @DecimalMin(value = "0.0", message = "{" + StaskMessageKeys.VALIDATION_REQUIRED_WORKER_COUNT_MIN + "}")
    @Digits(integer = 8, fraction = 2, message = "{" + StaskMessageKeys.VALIDATION_REQUIRED_WORKER_COUNT_MIN + "}")
    private Double requiredWorkerCount;

    /** 正式计划日用工人数，兼容字段 requiredWorkerCount 的替代字段。 */
    @DecimalMin(value = "0.0", message = "{" + StaskMessageKeys.VALIDATION_REQUIRED_WORKER_COUNT_MIN + "}")
    @Digits(integer = 8, fraction = 2, message = "{" + StaskMessageKeys.VALIDATION_REQUIRED_WORKER_COUNT_MIN + "}")
    private BigDecimal dailyLaborCount;

    /** 已读取用工记录的乐观锁版本；首次写入时为空。 */
    private Long laborRecordVersion;

    /** 客户端幂等键。 */
    private String idempotencyKey;
}
