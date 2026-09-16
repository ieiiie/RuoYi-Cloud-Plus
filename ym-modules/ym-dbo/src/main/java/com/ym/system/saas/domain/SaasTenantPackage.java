package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * SaaS 租户套餐。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package")
public class SaasTenantPackage extends BaseEntity {
    @TableId("package_id")
    private Long packageId;
    private String packageName;
    private Boolean menuCheckStrictly;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
