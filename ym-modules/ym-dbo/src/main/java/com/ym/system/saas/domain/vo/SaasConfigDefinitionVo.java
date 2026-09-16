package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasConfigDefinition;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SaaS 参数定义视图。
 */
@Data
@AutoMapper(target = SaasConfigDefinition.class)
public class SaasConfigDefinitionVo implements Serializable {
    private Long definitionId;
    private Long appId;
    private String appName;
    private String configName;
    private String configKey;
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
    private Boolean issuedFlag;
    private String remark;
    private LocalDateTime createTime;
}
