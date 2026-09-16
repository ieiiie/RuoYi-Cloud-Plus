package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasTenant;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SaaS 租户视图。
 */
@Data
@AutoMapper(target = SaasTenant.class)
public class SaasTenantVo implements Serializable {
    private Long id;
    private String tenantId;
    private String contactUserName;
    private String contactPhone;
    private String companyName;
    private String logoUrl;
    private String licenseNumber;
    private String address;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String regionName;
    private String domain;
    private String intro;
    private Long packageId;
    private String packageName;
    private Long ossConfigId;
    private String ossConfigKey;
    private LocalDateTime expireTime;
    private Long accountCount;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
