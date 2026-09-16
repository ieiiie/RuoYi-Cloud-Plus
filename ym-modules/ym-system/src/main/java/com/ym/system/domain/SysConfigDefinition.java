package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;

/** SaaS 租户参数定义。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config_definition")
public class SysConfigDefinition extends BaseEntity {
    @TableId("definition_id")
    private Long definitionId;
    private Long appId;
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
    @TableLogic
    private String delFlag;
    private String remark;
}
