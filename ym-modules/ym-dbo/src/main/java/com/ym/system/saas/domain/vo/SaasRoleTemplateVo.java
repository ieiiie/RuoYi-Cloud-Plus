package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasRoleTemplate;

import java.io.Serializable;
import java.util.List;

/**
 * SaaS 角色模板视图。
 */
@Data
@AutoMapper(target = SaasRoleTemplate.class)
public class SaasRoleTemplateVo implements Serializable {
    private Long templateId;
    private String templateName;
    private String templateKey;
    private Long appId;
    private String appName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private Integer templateVersion;
    private Boolean tenantDeletable;
    private String remark;
    private List<Long> menuIds;
}
