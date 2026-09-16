package com.ym.system.saas.service.impl;

import com.ym.common.core.exception.ServiceException;
import com.ym.system.saas.domain.SaasMenu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 套餐菜单范围校验，支持组合应用菜单而无需开放源微应用入口。 */
final class PackageMenuSelection {

    private PackageMenuSelection() {
    }

    static List<Long> complete(Collection<Long> selectedMenuIds,
                               Map<Long, SaasMenu> allMenus,
                               Set<Long> directlyAvailableAppIds,
                               Set<Long> compositeMenuIds) {
        LinkedHashSet<Long> menuIds = new LinkedHashSet<>(
            selectedMenuIds == null ? List.of() : selectedMenuIds);
        for (Long menuId : List.copyOf(menuIds)) {
            SaasMenu menu = allMenus.get(menuId);
            if (menu == null || (!directlyAvailableAppIds.contains(menu.getAppId())
                && !compositeMenuIds.contains(menuId))) {
                throw new ServiceException("套餐菜单必须属于已选择应用或组合应用");
            }
            Long parentId = menu.getParentId();
            while (parentId != null && parentId != 0L) {
                SaasMenu parent = allMenus.get(parentId);
                if (parent == null || !Objects.equals(parent.getAppId(), menu.getAppId())) {
                    throw new ServiceException("菜单祖先链不完整或跨应用");
                }
                if (!directlyAvailableAppIds.contains(parent.getAppId())
                    && !compositeMenuIds.contains(parentId)) {
                    throw new ServiceException("组合应用未包含所选菜单的完整祖先链");
                }
                menuIds.add(parentId);
                parentId = parent.getParentId();
            }
        }
        return new ArrayList<>(menuIds);
    }
}
