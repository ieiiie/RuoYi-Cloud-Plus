package com.ym.agriculture.farmtask.employee.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 小程序注册审批驳回参数。
 */
@Data
public class MiniappRegisterApprovalRejectBo {

    /** 驳回原因，1-200 个字符。 */
    @NotBlank(message = "请填写驳回原因")
    @Size(max = 200, message = "审核备注不能超过{max}个字符")
    private String reason;
}
