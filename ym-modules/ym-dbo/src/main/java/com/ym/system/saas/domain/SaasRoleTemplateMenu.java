package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 角色模板菜单关系。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_template_menu")
public class SaasRoleTemplateMenu {
    private Long templateId;
    private Long menuId;
}
