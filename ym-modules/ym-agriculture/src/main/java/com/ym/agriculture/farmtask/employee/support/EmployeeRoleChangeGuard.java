package com.ym.agriculture.farmtask.employee.support;

import com.ym.agriculture.farmtask.employee.model.entity.SysEmployee;

/**
 * 人员应用岗位变更前校验扩展点。
 */
public interface EmployeeRoleChangeGuard {

    /**
     * 校验人员是否允许变更为目标应用岗位。
     *
     * @param employee       当前人员记录
     * @param targetRoleCode 目标应用岗位编码
     */
    void validateRoleChange(SysEmployee employee, String targetRoleCode);
}
