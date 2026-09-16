package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * 租户套餐对象 sys_tenant_package。
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package")
public class SysTenantPackage extends BaseEntity {

    /** 租户套餐主键。 */
    @TableId(value = "package_id")
    private Long packageId;

    /** 套餐名称。 */
    private String packageName;

    /** 关联菜单 ID，逗号分隔。 */
    private String menuIds;

    /** 备注。 */
    private String remark;

    /** 菜单树是否父子联动。 */
    private Boolean menuCheckStrictly;

    /** 状态（0正常 1停用）。 */
    private String status;

    /** 删除标志（0存在 1删除）。 */
    @TableLogic
    private String delFlag;

}
