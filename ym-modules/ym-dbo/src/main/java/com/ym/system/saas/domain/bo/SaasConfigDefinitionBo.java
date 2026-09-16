package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.ym.common.core.validate.*;
import com.ym.system.saas.domain.SaasConfigDefinition;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * SaaS 参数定义请求对象。
 */
@Data
@AutoMapper(target = SaasConfigDefinition.class, reverseConvertGenerate = false)
public class SaasConfigDefinitionBo implements Serializable {
    @NotNull(groups = EditGroup.class)
    private Long definitionId;
    private Long appId;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String configName;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String configKey;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String valueType;
    private String defaultValue;
    private Boolean requiredFlag;
    private Integer minLength;
    private Integer maxLength;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String regexPattern;
    private String enumOptions;
    private Boolean tenantEditable;
    private Integer orderNum;
    private String status;
    private String remark;
}
