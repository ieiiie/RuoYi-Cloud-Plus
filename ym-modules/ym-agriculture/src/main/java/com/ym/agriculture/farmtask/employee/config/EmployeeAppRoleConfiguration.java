package com.ym.agriculture.farmtask.employee.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 人员应用角色配置注册。
 */
@Configuration
@EnableConfigurationProperties(EmployeeAppRoleProperties.class)
public class EmployeeAppRoleConfiguration {
}
