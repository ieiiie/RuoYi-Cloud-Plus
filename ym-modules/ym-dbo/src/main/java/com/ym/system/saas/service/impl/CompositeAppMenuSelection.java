package com.ym.system.saas.service.impl;

import com.ym.common.core.exception.ServiceException;
import com.ym.system.saas.domain.SaasApp;
import com.ym.system.saas.domain.SaasMenu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 组合应用菜单范围校验。保留所选按钮，并补齐它们在同一源微应用内的祖先目录。
 */
final class CompositeAppMenuSelection {

    private CompositeAppMenuSelection() {
    }

    /** 停用组合应用允许暂不配置菜单；启用时仍须至少包含一个页面。 */
    static List<Long> complete(Collection<Long> selectedMenuIds, String status,
                               Map<Long, SaasMenu> menus, Map<Long, SaasApp> apps) {
        if ("1".equals(status) && (selectedMenuIds == null || selectedMenuIds.isEmpty())) {
            return List.of();
        }
        return complete(selectedMenuIds, menus, apps);
    }

    static List<Long> complete(Collection<Long> selectedMenuIds,
                               Map<Long, SaasMenu> menus,
                               Map<Long, SaasApp> apps) {
        if (selectedMenuIds == null || selectedMenuIds.isEmpty()) {
            throw new ServiceException("组合应用至少需要选择一个页面菜单");
        }
        LinkedHashSet<Long> completed = new LinkedHashSet<>();
        boolean hasPage = false;
        for (Long menuId : selectedMenuIds) {
            SaasMenu menu = menus.get(menuId);
            if (menu == null) {
                throw new ServiceException("组合应用包含不存在的菜单");
            }
            SaasApp sourceApp = apps.get(menu.getAppId());
            if (sourceApp == null || !"MICRO".equals(sourceApp.getAppType())) {
                throw new ServiceException("组合应用只能选择微应用菜单");
            }
            hasPage |= "C".equals(menu.getMenuType());
            completed.add(menuId);
            Long parentId = menu.getParentId();
            LinkedHashSet<Long> ancestors = new LinkedHashSet<>(List.of(menuId));
            while (parentId != null && parentId != 0L) {
                if (!ancestors.add(parentId)) {
                    throw new ServiceException("组合应用菜单祖先链存在循环");
                }
                SaasMenu parent = menus.get(parentId);
                if (parent == null || !Objects.equals(parent.getAppId(), menu.getAppId())) {
                    throw new ServiceException("组合应用菜单祖先链不完整或跨应用");
                }
                completed.add(parentId);
                parentId = parent.getParentId();
            }
        }
        if (!hasPage) {
            throw new ServiceException("组合应用至少需要选择一个页面菜单");
        }
        return new ArrayList<>(completed);
    }
}
