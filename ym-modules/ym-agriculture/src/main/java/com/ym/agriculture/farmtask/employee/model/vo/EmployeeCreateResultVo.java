package com.ym.agriculture.farmtask.employee.model.vo;

import lombok.Data;

/**
 * 后台录入员工并生成绑定码的返回结果。
 */
@Data
public class EmployeeCreateResultVo {

    /**
     * 员工信息。
     */
    private SysEmployeeVo employee;

    /**
     * 绑定邀请码信息。
     */
    private SysInviteCodeVo inviteCode;
}
