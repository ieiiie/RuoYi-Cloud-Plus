package com.ym.agriculture.farmtask.employee.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 人员应用角色配置，对应 ym.employee.app-roles。
 */
@Data
@ConfigurationProperties(prefix = "ym.employee")
public class EmployeeAppRoleProperties {

    /**
     * 应用角色列表。
     */
    private List<AppRoleItem> appRoles = new ArrayList<>();

    /**
     * 单个应用角色配置项。
     */
    @Data
    public static class AppRoleItem {

        /**
         * 角色编码，如 stask:production_admin。
         */
        private String code;

        /**
         * 角色显示名称。
         */
        private String name;

        /**
         * 是否允许创建岗位邀请码并通过小程序自选，默认允许。
         */
        private boolean inviteSelectable = true;
    }
}
