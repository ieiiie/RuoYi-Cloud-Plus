package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 租户角色菜单关系。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_menu")
public class SaasRoleMenu {
    private Long roleId;
    private Long menuId;
}
