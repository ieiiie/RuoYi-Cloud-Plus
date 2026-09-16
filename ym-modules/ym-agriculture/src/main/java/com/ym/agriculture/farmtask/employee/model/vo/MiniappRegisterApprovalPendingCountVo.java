package com.ym.agriculture.farmtask.employee.model.vo;

import lombok.Data;

/**
 * 小程序注册待审批数量。
 */
@Data
public class MiniappRegisterApprovalPendingCountVo {

    /** 当前租户待审批注册数量。 */
    private long pendingCount;
}
