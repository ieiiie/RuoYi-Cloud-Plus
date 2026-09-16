package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 套餐应用关系。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_tenant_package_app")
public class SaasPackageApp {
    private Long packageId;
    private Long appId;
}
