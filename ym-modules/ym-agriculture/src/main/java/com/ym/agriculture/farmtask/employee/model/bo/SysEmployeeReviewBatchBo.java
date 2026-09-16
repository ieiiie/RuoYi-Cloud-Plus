package com.ym.agriculture.farmtask.employee.model.bo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 人员批量审核业务对象。
 */
@Data
public class SysEmployeeReviewBatchBo {

    /**
     * 待审核人员ID列表。
     */
    @NotEmpty(message = "请选择待审核人员")
    private List<Long> employeeIds;

    /**
     * 审核备注，最多 200 字；批量拒绝时可选填，批量通过时通常为空。
     */
    @Size(max = 200, message = "审核备注不能超过{max}个字符")
    private String reviewRemark;
}
