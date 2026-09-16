package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * stask 换人工人入参。
 */
@Data
public class SfStaskReplaceWorkerBo {

    /**
     * 新工人员工ID。
     */
    @NotNull(message = "{" + StaskMessageKeys.VALIDATION_NEW_WORKER_REQUIRED + "}")
    private Long newWorkerId;
}
