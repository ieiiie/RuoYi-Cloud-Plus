package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 租户套餐菜单关联。 */
@Data
@TableName("sys_tenant_package_menu")
public class SysTenantPackageMenu {
    private Long packageId;
    private Long menuId;
}
