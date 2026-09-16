package com.ym.system.dubbo;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import com.ym.common.core.utils.StreamUtils;
import com.ym.system.api.RemoteRoleService;
import com.ym.system.api.domain.vo.RemoteRoleVo;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.system.domain.SysRole;
import com.ym.system.domain.SysRoleMenu;
import com.ym.system.domain.SysMenu;
import com.ym.system.mapper.SysMenuMapper;
import com.ym.system.mapper.SysRoleMapper;
import com.ym.system.mapper.SysRoleMenuMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteRoleServiceImpl implements RemoteRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 根据角色 ID 列表查询角色名称映射关系
     *
     * @param roleIds 角色 ID 列表
     * @return Map，其中 key 为角色 ID，value 为对应的角色名称
     */
    @Override
    public Map<Long, String> selectRoleNamesByIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyMap();
        }
        List<SysRole> list = roleMapper.lambda()
            .select(SysRole::getRoleId, SysRole::getRoleName)
            .in(SysRole::getRoleId, roleIds)
            .list();
        return StreamUtils.toMap(list, SysRole::getRoleId, SysRole::getRoleName);
    }

    @Override
    public List<RemoteRoleVo> listActiveRoles(String tenantId) {
        return roleMapper.lambda()
            .eq(SysRole::getTenantId, tenantId)
            .eq(SysRole::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SysRole::getRoleSort)
            .orderByAsc(SysRole::getRoleId)
            .list().stream()
            .map(row -> MapstructUtils.convert(row, RemoteRoleVo.class))
            .toList();
    }

    @Override
    public List<Long> filterActiveRoleIds(String tenantId, Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return List.of();
        }
        return roleMapper.lambda()
            .select(SysRole::getRoleId)
            .eq(SysRole::getTenantId, tenantId)
            .eq(SysRole::getStatus, SystemConstants.NORMAL)
            .in(SysRole::getRoleId, roleIds)
            .list().stream().map(SysRole::getRoleId).toList();
    }

    @Override
    public Set<String> selectPermissions(String tenantId, Collection<Long> roleIds) {
        List<Long> validRoleIds = filterActiveRoleIds(tenantId, roleIds);
        if (validRoleIds.isEmpty()) {
            return Set.of();
        }
        List<Long> menuIds = roleMenuMapper.lambda()
            .select(SysRoleMenu::getMenuId)
            .in(SysRoleMenu::getRoleId, validRoleIds)
            .list().stream().map(SysRoleMenu::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return Set.of();
        }
        return menuMapper.lambda()
            .select(SysMenu::getPerms)
            .in(SysMenu::getMenuId, menuIds)
            .eq(SysMenu::getStatus, SystemConstants.NORMAL)
            .isNotNull(SysMenu::getPerms)
            .list().stream().map(SysMenu::getPerms)
            .filter(com.ym.common.core.utils.StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

}
