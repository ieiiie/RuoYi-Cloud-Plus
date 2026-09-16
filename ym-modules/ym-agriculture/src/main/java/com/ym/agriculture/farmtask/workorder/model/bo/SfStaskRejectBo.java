package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * stask 退回/拒绝原因入参。
 */
@Data
public class SfStaskRejectBo {

    /**
     * 原因说明。
     */
    @NotBlank(message = "{" + StaskMessageKeys.VALIDATION_REASON_REQUIRED + "}")
    @Size(max = 500, message = "{" + StaskMessageKeys.VALIDATION_REASON_MAX + "}")
    private String reason;
}
