package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.ym.common.core.validate.*;
import com.ym.system.saas.domain.SaasRoleTemplate;

import java.io.Serializable;
import java.util.List;

/**
 * SaaS 角色模板请求对象。
 */
@Data
@AutoMapper(target = SaasRoleTemplate.class, reverseConvertGenerate = false)
public class SaasRoleTemplateBo implements Serializable {
    @NotNull(groups = EditGroup.class)
    private Long templateId;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String templateName;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String templateKey;
    private Long appId;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private Boolean tenantDeletable;
    private String remark;
    private List<Long> menuIds;
}
