package com.ym.agriculture.farmtask.employee.model.vo;

import lombok.Data;

/**
 * 人员应用角色视图对象。
 */
@Data
public class EmployeeAppRoleVo {

    /**
     * 角色编码，对应 ym.employee.app-roles 配置中的 code。
     */
    private String code;

    /**
     * 角色显示名称，对应 ym.employee.app-roles 配置中的 name。
     */
    private String name;

    /**
     * 是否可用于小程序注册或岗位邀请码；false 的岗位仍可由后台授予。
     */
    private boolean inviteSelectable;
}
