package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * stask 调整需求工人数入参。
 */
@Data
public class SfStaskAdjustWorkersBo {

    /**
     * 调整后的需求工人数。
     */
    @NotNull(message = "{" + StaskMessageKeys.VALIDATION_REQUIRED_WORKER_COUNT_REQUIRED + "}")
    @DecimalMin(value = "0.0", message = "{" + StaskMessageKeys.VALIDATION_REQUIRED_WORKER_COUNT_MIN + "}")
    @Digits(integer = 8, fraction = 2, message = "{" + StaskMessageKeys.VALIDATION_REQUIRED_WORKER_COUNT_MIN + "}")
    private Double requiredWorkerCount;
}
