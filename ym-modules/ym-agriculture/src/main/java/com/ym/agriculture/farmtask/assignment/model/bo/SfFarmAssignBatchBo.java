package com.ym.agriculture.farmtask.assignment.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * stask 农事批量分配入参。
 */
@Data
public class SfFarmAssignBatchBo {

    /**
     * 大棚ID，对应 {@code sf_field.field_id}。
     */
    @NotNull(message = "{" + StaskMessageKeys.VALIDATION_GREENHOUSE_ID_REQUIRED + "}")
    private Long greenhouseId;

    /**
     * 农事项目ID列表，对应 {@code sf_farm_work_dict.dict_id}。
     */
    @NotEmpty(message = "{" + StaskMessageKeys.VALIDATION_ASSIGNMENT_WORK_ITEMS_REQUIRED + "}")
    private List<Long> workItemIds;

    /**
     * 组长人员ID，对应 {@code sys_employee.employee_id}。
     */
    @NotNull(message = "{" + StaskMessageKeys.VALIDATION_LEADER_ID_REQUIRED + "}")
    private Long leaderId;
}
