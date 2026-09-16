package com.ym.agriculture.farmtask.employee.event;

/**
 * stask 人员姓名发生变化事件。
 *
 * @param tenantId 租户编号
 * @param employeeId 员工主键
 */
public record SysEmployeeTextChangedEvent(String tenantId, Long employeeId) {
}
