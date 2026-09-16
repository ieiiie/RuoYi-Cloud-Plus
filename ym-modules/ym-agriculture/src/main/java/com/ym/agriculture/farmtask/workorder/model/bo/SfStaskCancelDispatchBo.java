package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * stask 撤销派工入参。
 */
@Data
public class SfStaskCancelDispatchBo {

    /**
     * 撤销原因。
     */
    @NotBlank(message = "{" + StaskMessageKeys.VALIDATION_CANCEL_REASON_REQUIRED + "}")
    @Size(max = 500, message = "{" + StaskMessageKeys.VALIDATION_CANCEL_REASON_MAX + "}")
    private String reason;
}
