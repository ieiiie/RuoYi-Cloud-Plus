package com.ym.system.service;

import cn.hutool.core.bean.BeanUtil;
import com.ym.system.api.model.LoginUser;
import com.ym.system.api.model.RoleDTO;
import com.ym.system.domain.vo.SysRoleVo;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.satoken.utils.LoginHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 刷新当前租户会话的授权快照，不修改角色或套餐授权关系。 */
@Service
@RequiredArgsConstructor
public class SysLoginPermissionService {

    private final ISysPermissionService permissionService;

    /**
     * 使用服务端查询的有效角色重建菜单、角色和数据权限。
     * 所有查询成功后才替换快照，撤销的权限也必须删除，不能与旧值合并。
     */
    public void refresh(LoginUser loginUser, List<SysRoleVo> roles) {
        if (LoginHelper.isPlatformSuperAdmin(loginUser)) {
            grantPlatformPermissions(loginUser);
            return;
        }
        var menuPermissions = permissionService.getMenuPermission(loginUser.getUserId());
        var rolePermissions = permissionService.getRolePermission(loginUser.getUserId());
        List<RoleDTO> roleDtos = BeanUtil.copyToList(roles, RoleDTO.class);
        var dataScopeRoleMap = permissionService.getDataScopeRoleMap(roleDtos);
        loginUser.setMenuPermission(menuPermissions);
        loginUser.setRolePermission(rolePermissions);
        loginUser.setRoles(roleDtos);
        loginUser.setDataScopeRoleMap(dataScopeRoleMap);
    }

    /** 只可用于已经过全局身份校验的服务端登录对象。 */
    public static void grantPlatformPermissions(LoginUser loginUser) {
        if (!LoginHelper.isPlatformSuperAdmin(loginUser)) {
            throw new IllegalArgumentException("非平台超级管理员");
        }
        RoleDTO role = new RoleDTO();
        role.setRoleId(SystemConstants.SUPER_ADMIN_ROLE_ID);
        role.setRoleKey(SystemConstants.SUPER_ADMIN_ROLE_KEY);
        role.setDataScope("1");
        loginUser.setMenuPermission(java.util.Set.of("*:*:*"));
        loginUser.setRolePermission(java.util.Set.of(SystemConstants.SUPER_ADMIN_ROLE_KEY));
        loginUser.setRoles(List.of(role));
        loginUser.setDataScopeRoleMap(java.util.Map.of());
    }
}
