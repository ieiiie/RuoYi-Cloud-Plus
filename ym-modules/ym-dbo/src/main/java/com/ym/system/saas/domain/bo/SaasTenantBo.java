package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.ym.common.core.constant.RegexConstants;
import com.ym.common.core.validate.*;
import com.ym.system.saas.domain.SaasTenant;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SaaS 租户请求对象。
 */
@Data
@AutoMapper(target = SaasTenant.class, reverseConvertGenerate = false)
public class SaasTenantBo implements Serializable {
    @NotNull(groups = EditGroup.class)
    private Long id;
    private String tenantId;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String contactUserName;
    private String contactPhone;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String companyName;
    @Size(max = 500, groups = {AddGroup.class, EditGroup.class})
    private String logoUrl;
    private String licenseNumber;
    private String address;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String provinceCode;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String cityCode;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String districtCode;
    private String regionName;
    private String domain;
    private String intro;
    @NotNull(groups = {AddGroup.class, EditGroup.class})
    private Long packageId;
    @NotNull(groups = {AddGroup.class, EditGroup.class})
    private Long ossConfigId;
    private LocalDateTime expireTime;
    private Long accountCount;
    private String status;
    private String remark;
    @NotBlank(groups = AddGroup.class)
    private String username;
    @NotBlank(groups = AddGroup.class)
    private String password;
    @NotBlank(groups = AddGroup.class)
    @Pattern(regexp = RegexConstants.MOBILE, message = "管理员手机号格式不正确", groups = AddGroup.class)
    private String adminPhone;
}
