package com.ym.system.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色菜单权限请求。
 */
@Data
public class SysRoleMenuBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @NotNull(message = "菜单权限不能为空")
    private Long[] menuIds;

    @NotNull(message = "菜单关联方式不能为空")
    private Boolean menuCheckStrictly;
}
