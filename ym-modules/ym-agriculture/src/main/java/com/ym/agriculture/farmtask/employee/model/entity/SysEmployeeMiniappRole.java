package com.ym.agriculture.farmtask.employee.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 人员绑定的小程序系统角色 sys_employee_miniapp_role。
 */
@Data
@TableName("sys_employee_miniapp_role")
public class SysEmployeeMiniappRole {

    /**
     * 人员ID，和角色ID共同组成主键。
     */
    @TableId(value = "employee_id", type = IdType.INPUT)
    private Long employeeId;

    /**
     * 系统角色ID。
     */
    private Long roleId;

    /**
     * 关联所属租户编号，用于租户隔离校验。
     */
    private String tenantId;
}
