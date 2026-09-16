package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * stask 组长派工入参。
 */
@Data
public class SfStaskDispatchBo {

    /**
     * 工人员工ID列表。
     */
    @NotEmpty(message = "{" + StaskMessageKeys.VALIDATION_WORKERS_REQUIRED + "}")
    private List<Long> workerIds;
}
