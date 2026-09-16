package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/**
 * SaaS 租户。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SaasTenant extends BaseEntity {
    @TableId("id")
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
    private Long ossConfigId;
    private LocalDateTime expireTime;
    private Long accountCount;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
