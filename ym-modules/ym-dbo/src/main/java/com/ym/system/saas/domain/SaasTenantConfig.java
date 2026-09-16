package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * 租户参数值。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SaasTenantConfig extends BaseEntity {
    @TableId("config_id")
    private Long configId;
    private String tenantId;
    private Long definitionId;
    private String configName;
    private String configKey;
    private String configValue;
    private String configType;
    private String remark;
}
