package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.system.api.domain.vo.RemoteRoleVo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.ym.common.tenant.core.TenantEntity;

/**
 * 角色表 sys_role
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
@AutoMapper(target = RemoteRoleVo.class, reverseConvertGenerate = false)
public class SysRole extends TenantEntity {

    /**
     * 角色ID
     */
    @TableId(value = "role_id")
    private Long roleId;

    /** 角色模板ID。 */
    private Long templateId;

    /** 已同步模板版本。 */
    private Integer templateVersion;

    /** 是否由运营模板下发的内置角色。 */
    private Boolean isBuiltin;

    /** 模板角色是否允许租户删除。 */
    private Boolean tenantDeletable;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色权限
     */
    private String roleKey;

    /**
     * 角色排序
     */
    private Integer roleSort;

    /**
     * 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）
     */
    private String dataScope;

    /**
     * 菜单树选择项是否关联显示（ 0：父子不互相关联显示 1：父子互相关联显示）
     */
    private Boolean menuCheckStrictly;

    /**
     * 部门树选择项是否关联显示（0：父子不互相关联显示 1：父子互相关联显示 ）
     */
    private Boolean deptCheckStrictly;

    /**
     * 角色状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

}
