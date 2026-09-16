package com.ym.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.domain.BaseEntity;
import com.ym.system.domain.SysTenantPackage;

/**
 * 租户套餐业务对象。
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysTenantPackage.class, reverseConvertGenerate = false)
public class SysTenantPackageBo extends BaseEntity {

    @NotNull(message = "租户套餐主键不能为空", groups = EditGroup.class)
    private Long packageId;

    @NotBlank(message = "套餐名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String packageName;

    /** 关联菜单 ID。 */
    @AutoMapping(target = "menuIds", ignore = true)
    private Long[] menuIds;

    private String remark;

    private Boolean menuCheckStrictly;

    private String status;

}
