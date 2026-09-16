package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * SaaS 内置角色模板。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_template")
public class SaasRoleTemplate extends BaseEntity {
    @TableId("template_id")
    private Long templateId;
    private String templateName;
    private String templateKey;
    private Long appId;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private Integer templateVersion;
    private Boolean tenantDeletable;
    @TableLogic
    private String delFlag;
    private String remark;
}
