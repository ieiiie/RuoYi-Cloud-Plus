package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * SaaS OSS 配置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oss_config")
public class SaasOssConfig extends BaseEntity {
    @TableId("oss_config_id")
    private Long ossConfigId;
    private String configKey;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String prefix;
    private String endpoint;
    private String domainUrl;
    private String isHttps;
    private String region;
    private String accessPolicy;
    private String ext1;
    private String remark;
}
