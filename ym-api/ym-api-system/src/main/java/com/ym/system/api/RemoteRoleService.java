package com.ym.system.api;

import com.ym.system.api.domain.vo.RemoteRoleVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色服务
 *
 * @author Lion Li
 */
public interface RemoteRoleService {

    /**
     * 根据角色 ID 列表查询角色名称映射关系
     *
     * @param roleIds 角色 ID 列表
     * @return Map，其中 key 为角色 ID，value 为对应的角色名称
     */
    Map<Long, String> selectRoleNamesByIds(Collection<Long> roleIds);

    /** 查询指定租户可绑定的正常角色。 */
    List<RemoteRoleVo> listActiveRoles(String tenantId);

    /** 过滤出指定租户的正常角色 ID。 */
    List<Long> filterActiveRoleIds(String tenantId, Collection<Long> roleIds);

    /** 查询角色在指定租户中有效的权限码。 */
    Set<String> selectPermissions(String tenantId, Collection<Long> roleIds);

}
