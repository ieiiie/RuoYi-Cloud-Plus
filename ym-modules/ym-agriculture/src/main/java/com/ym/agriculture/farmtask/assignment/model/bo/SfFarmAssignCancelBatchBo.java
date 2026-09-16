package com.ym.agriculture.farmtask.assignment.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * stask 农事批量取消分配入参。
 */
@Data
public class SfFarmAssignCancelBatchBo {

    /**
     * 大棚ID，用于校验取消范围。
     */
    @NotNull(message = "{" + StaskMessageKeys.VALIDATION_GREENHOUSE_ID_REQUIRED + "}")
    private Long greenhouseId;

    /**
     * 组长人员ID，用于校验取消范围。
     */
    @NotNull(message = "{" + StaskMessageKeys.VALIDATION_LEADER_ID_REQUIRED + "}")
    private Long leaderId;

    /**
     * 分配记录ID列表。
     */
    @NotEmpty(message = "{" + StaskMessageKeys.VALIDATION_CANCEL_ASSIGNMENT_WORK_ITEMS_REQUIRED + "}")
    private List<Long> assignmentIds;
}
