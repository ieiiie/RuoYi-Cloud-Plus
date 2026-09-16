package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.ym.common.core.validate.*;
import com.ym.system.saas.domain.SaasOssConfig;

import java.io.Serializable;

/**
 * SaaS OSS 配置请求对象。
 */
@Data
@AutoMapper(target = SaasOssConfig.class, reverseConvertGenerate = false)
public class SaasOssConfigBo implements Serializable {
    @NotNull(groups = EditGroup.class)
    private Long ossConfigId;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String configKey;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String accessKey;
    @NotBlank(groups = AddGroup.class)
    private String secretKey;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String bucketName;
    private String prefix;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String endpoint;
    private String domainUrl;
    private String isHttps;
    private String region;
    private String accessPolicy;
    private String ext1;
    private String remark;
}
