package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasOssConfig;

import java.io.Serializable;

/**
 * SaaS OSS 配置视图，密钥仅返回掩码。
 */
@Data
@AutoMapper(target = SaasOssConfig.class)
public class SaasOssConfigVo implements Serializable {
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
