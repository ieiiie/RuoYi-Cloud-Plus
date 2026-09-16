package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.ym.common.core.validate.*;
import com.ym.system.saas.domain.SaasTenantPackage;

import java.io.Serializable;
import java.util.*;

/**
 * SaaS 套餐请求对象。
 */
@Data
@AutoMapper(target = SaasTenantPackage.class, reverseConvertGenerate = false)
public class SaasTenantPackageBo implements Serializable {
    @NotNull(groups = EditGroup.class)
    private Long packageId;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String packageName;
    private Boolean menuCheckStrictly;
    private String status;
    private String remark;
    @NotEmpty(groups = {AddGroup.class, EditGroup.class})
    private List<Long> appIds;
    private List<Long> menuIds;
}
