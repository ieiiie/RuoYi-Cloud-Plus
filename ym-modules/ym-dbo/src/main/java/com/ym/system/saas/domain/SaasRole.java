package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * 租户角色，仅供运营初始化和模板同步。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SaasRole extends BaseEntity {
    @TableId("role_id")
    private Long roleId;
    private String tenantId;
    private Long templateId;
    private Integer templateVersion;
    private Boolean isBuiltin;
    private Boolean tenantDeletable;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private Boolean menuCheckStrictly;
    private Boolean deptCheckStrictly;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
