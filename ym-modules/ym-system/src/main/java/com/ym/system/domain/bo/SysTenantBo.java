package com.ym.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.domain.BaseEntity;
import com.ym.system.domain.SysTenant;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 租户业务对象。
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysTenant.class, reverseConvertGenerate = false)
public class SysTenantBo extends BaseEntity {

    @NotNull(message = "主键不能为空", groups = EditGroup.class)
    private Long id;

    /** 已存在租户的编号仅用于定位和校验，修改时不会被更新。 */
    private String tenantId;

    @NotBlank(message = "联系人不能为空", groups = {AddGroup.class, EditGroup.class})
    private String contactUserName;

    @NotBlank(message = "联系电话不能为空", groups = {AddGroup.class, EditGroup.class})
    private String contactPhone;

    @NotBlank(message = "企业名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String companyName;

    /** 新建租户时初始化租户管理员账号。 */
    @NotBlank(message = "用户名不能为空", groups = AddGroup.class)
    private String username;

    /** 新建租户时初始化租户管理员密码。 */
    @NotBlank(message = "密码不能为空", groups = AddGroup.class)
    private String password;

    private String licenseNumber;

    private String address;

    private String provinceCode;

    private String cityCode;

    private String districtCode;

    private String regionName;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String domain;

    private String intro;

    private String remark;

    @NotNull(message = "租户套餐不能为空", groups = AddGroup.class)
    private Long packageId;

    @NotNull(message = "OSS配置不能为空", groups = AddGroup.class)
    private Long ossConfigId;

    private LocalDateTime expireTime;

    @Min(value = -1, message = "用户数量上限不能小于-1")
    private Long accountCount;

    private String status;

}
