package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 租户套餐应用关联。 */
@Data
@TableName("sys_tenant_package_app")
public class SysTenantPackageApp {
    private Long packageId;
    private Long appId;
}
