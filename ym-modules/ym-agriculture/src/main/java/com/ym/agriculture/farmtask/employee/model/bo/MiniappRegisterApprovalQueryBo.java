package com.ym.agriculture.farmtask.employee.model.bo;

import lombok.Data;

/**
 * 小程序注册审批列表查询参数。
 */
@Data
public class MiniappRegisterApprovalQueryBo {

    /** 审批状态：PENDING、APPROVED、REJECTED、ALL；默认 PENDING。 */
    private String auditStatus;

    /** 姓名或手机号关键字。 */
    private String keyword;

    /** 申请的小程序岗位编码。 */
    private String applyAppRoleCode;
}
