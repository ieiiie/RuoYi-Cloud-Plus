package com.ym.system.helper;

import com.ym.system.domain.SysApp;
import com.ym.system.domain.SysCompositeAppMenu;
import com.ym.system.domain.SysMenu;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** 套餐菜单上界：直接应用菜单，或已开通组合应用明确选中的有效源菜单。 */
public final class TenantPackageMenuScope {
    private TenantPackageMenuScope() { }

    public static Set<Long> resolve(Collection<SysApp> launchApps, Collection<SysMenu> packageMenus,
                                    Collection<SysCompositeAppMenu> bindings, Collection<SysApp> sourceApps) {
        Set<Long> directApps = launchApps.stream().filter(app -> "0".equals(app.getStatus()))
            .filter(app -> !"COMPOSITE".equals(app.getAppType())).map(SysApp::getAppId).collect(Collectors.toSet());
        Set<Long> compositeApps = launchApps.stream().filter(app -> "0".equals(app.getStatus()))
            .filter(app -> "COMPOSITE".equals(app.getAppType())).map(SysApp::getAppId).collect(Collectors.toSet());
        Set<Long> enabledSources = sourceApps.stream().filter(app -> "0".equals(app.getStatus()))
            .filter(app -> "MICRO".equals(app.getAppType())).map(SysApp::getAppId).collect(Collectors.toSet());
        Set<Long> selected = bindings.stream().filter(binding -> compositeApps.contains(binding.getAppId()))
            .map(SysCompositeAppMenu::getMenuId).collect(Collectors.toSet());
        Set<Long> result = new HashSet<>();
        for (SysMenu menu : packageMenus) {
            if ("0".equals(menu.getStatus()) && (directApps.contains(menu.getAppId())
                || (selected.contains(menu.getMenuId()) && enabledSources.contains(menu.getAppId())))) {
                result.add(menu.getMenuId());
            }
        }
        return result;
    }
}
