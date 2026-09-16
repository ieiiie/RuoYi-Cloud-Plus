package com.ym.system.event;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.tenant.helper.TenantHelper;

import java.util.Collection;
import java.util.List;

/**
 * 在线用户清理事件。
 *
 * @param tenantId 租户编号
 * @param roleId   角色 ID，存在时清理拥有该角色的在线用户
 * @param userIds  用户 ID 集合，存在时清理指定在线用户
 */
public record OnlineUserCleanEvent(String tenantId, Long roleId, Collection<Long> userIds) {

    public OnlineUserCleanEvent {
        TenantHelper.checkTenantId(tenantId);
        if (CollUtil.isNotEmpty(userIds)) {
            userIds = List.copyOf(userIds);
        }
    }

    /**
     * 创建按角色清理在线用户事件。
     *
     * @param roleId 角色 ID
     * @return 在线用户清理事件
     */
    public static OnlineUserCleanEvent byRole(Long roleId) {
        return new OnlineUserCleanEvent(TenantHelper.getTenantId(), roleId, null);
    }

    /**
     * 创建按用户清理在线用户事件。
     *
     * @param userIds 用户 ID 集合
     * @return 在线用户清理事件
     */
    public static OnlineUserCleanEvent byUsers(Collection<Long> userIds) {
        return new OnlineUserCleanEvent(TenantHelper.getTenantId(), null, userIds);
    }

}
