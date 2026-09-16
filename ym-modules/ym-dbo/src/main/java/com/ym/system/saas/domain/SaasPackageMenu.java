package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 套餐菜单关系。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_tenant_package_menu")
public class SaasPackageMenu {
    private Long packageId;
    private Long menuId;
}
