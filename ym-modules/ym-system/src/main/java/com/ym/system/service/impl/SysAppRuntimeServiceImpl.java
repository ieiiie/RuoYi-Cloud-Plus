package com.ym.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ym.common.core.constant.Constants;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.core.utils.TreeBuildUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.domain.*;
import com.ym.system.domain.vo.AuthorizedAppSourceVo;
import com.ym.system.domain.vo.AuthorizedAppVo;
import com.ym.system.mapper.*;
import com.ym.system.service.ISysAppRuntimeService;
import com.ym.system.service.ISysMenuService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 组合套餐、角色和源微应用状态，生成工作台应用描述。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAppRuntimeServiceImpl implements ISysAppRuntimeService {
    private static final String APP_TYPE_MICRO = "MICRO";
    private static final String APP_TYPE_COMPOSITE = "COMPOSITE";

    private final SysTenantMapper tenantMapper;
    private final SysAppMapper appMapper;
    private final SysMenuMapper menuMapper;
    private final SysTenantPackageAppMapper packageAppMapper;
    private final SysTenantPackageMenuMapper packageMenuMapper;
    private final SysCompositeAppMenuMapper compositeAppMenuMapper;
    private final ISysMenuService menuService;

    @Override
    public com.ym.system.domain.vo.PublicAppLoginVo selectPublicLogin(String appKey) {
        com.ym.common.core.utils.ApplicationEntryPolicy.requireApplicationKey(appKey);
        SysApp app = appMapper.lambda()
            .select(SysApp::getAppKey, SysApp::getAppName, SysApp::getAppType, SysApp::getLoginTheme)
            .eq(SysApp::getAppKey, appKey)
            .eq(SysApp::getStatus, SystemConstants.NORMAL)
            .in(SysApp::getAppType, APP_TYPE_MICRO, APP_TYPE_COMPOSITE).one();
        if (app == null) {
            throw new com.ym.common.core.exception.ServiceException("应用不存在或已停用");
        }
        String theme = com.ym.common.core.utils.ApplicationEntryPolicy.resolveLoginTheme(app.getLoginTheme());
        return new com.ym.system.domain.vo.PublicAppLoginVo(app.getAppKey(), app.getAppName(), app.getAppType(), theme);
    }

    @Override
    public List<AuthorizedAppVo> selectAuthorizedApps(Long userId) {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            return List.of();
        }
        SysTenant tenant = tenantMapper.lambda().select(SysTenant::getPackageId)
            .eq(SysTenant::getTenantId, tenantId).one();
        boolean platformAdmin = LoginHelper.isCurrentSuperAdmin(userId);
        if (tenant == null || (!platformAdmin && tenant.getPackageId() == null)) {
            log.warn("当前租户未绑定套餐，tenantId={}", tenantId);
            return List.of();
        }
        Long packageId = tenant.getPackageId();
        Set<Long> packageAppIds = platformAdmin ? Set.of() : packageAppMapper.selectList(new LambdaQueryWrapper<SysTenantPackageApp>()
                .select(SysTenantPackageApp::getAppId).eq(SysTenantPackageApp::getPackageId, packageId)).stream()
            .map(SysTenantPackageApp::getAppId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> packageMenuIds = platformAdmin ? Set.of() : packageMenuMapper.selectList(new LambdaQueryWrapper<SysTenantPackageMenu>()
                .select(SysTenantPackageMenu::getMenuId).eq(SysTenantPackageMenu::getPackageId, packageId)).stream()
            .map(SysTenantPackageMenu::getMenuId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!platformAdmin && (packageAppIds.isEmpty() || packageMenuIds.isEmpty())) {
            return List.of();
        }

        List<SysApp> launchApps = appMapper.lambda()
            .in(!platformAdmin, SysApp::getAppId, packageAppIds)
            .in(SysApp::getAppType, APP_TYPE_MICRO, APP_TYPE_COMPOSITE)
            .eq(SysApp::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SysApp::getOrderNum).orderByAsc(SysApp::getAppId).list();
        Map<Long, SysMenu> packageMenus = menuMapper.lambda()
            .in(!platformAdmin, SysMenu::getMenuId, packageMenuIds)
            .eq(SysMenu::getStatus, SystemConstants.NORMAL).list().stream()
            .collect(Collectors.toMap(SysMenu::getMenuId, Function.identity()));
        if (launchApps.isEmpty() || packageMenus.isEmpty()) {
            return List.of();
        }

        Map<Long, Set<Long>> compositeMenus = loadCompositeMenus(launchApps, packageMenus.keySet());
        Map<Long, SysApp> sourceApps = loadSourceApps(packageMenus.values());
        List<AuthorizedAppVo> result = new ArrayList<>();
        for (SysApp app : launchApps) {
            List<AuthorizedAppSourceVo> sources = APP_TYPE_COMPOSITE.equals(app.getAppType())
                ? buildCompositeSources(app, compositeMenus.getOrDefault(app.getAppId(), Set.of()), packageMenus, sourceApps, userId)
                : buildMicroSources(app, packageMenus, userId);
            if (!sources.isEmpty()) {
                result.add(toAuthorizedApp(app, sources));
            }
        }
        return result;
    }

    private Map<Long, Set<Long>> loadCompositeMenus(List<SysApp> apps, Set<Long> packageMenuIds) {
        List<Long> compositeAppIds = apps.stream().filter(app -> APP_TYPE_COMPOSITE.equals(app.getAppType()))
            .map(SysApp::getAppId).toList();
        if (compositeAppIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<Long>> result = new LinkedHashMap<>();
        compositeAppMenuMapper.selectList(new LambdaQueryWrapper<SysCompositeAppMenu>()
                .in(SysCompositeAppMenu::getAppId, compositeAppIds)
                .in(SysCompositeAppMenu::getMenuId, packageMenuIds))
            .forEach(item -> result.computeIfAbsent(item.getAppId(), key -> new LinkedHashSet<>()).add(item.getMenuId()));
        return result;
    }

    private Map<Long, SysApp> loadSourceApps(Collection<SysMenu> menus) {
        Set<Long> sourceIds = menus.stream().map(SysMenu::getAppId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (sourceIds.isEmpty()) {
            return Map.of();
        }
        return appMapper.lambda().in(SysApp::getAppId, sourceIds)
            .eq(SysApp::getAppType, APP_TYPE_MICRO)
            .eq(SysApp::getStatus, SystemConstants.NORMAL).list().stream()
            .collect(Collectors.toMap(SysApp::getAppId, Function.identity()));
    }

    private List<AuthorizedAppSourceVo> buildMicroSources(SysApp app, Map<Long, SysMenu> packageMenus, Long userId) {
        Set<Long> menuIds = packageMenus.values().stream()
            .filter(menu -> Objects.equals(menu.getAppId(), app.getAppId()))
            .map(SysMenu::getMenuId).collect(Collectors.toCollection(LinkedHashSet::new));
        AuthorizedAppSourceVo source = buildSource(app, menuIds, userId, false);
        return source == null ? List.of() : List.of(source);
    }

    private List<AuthorizedAppSourceVo> buildCompositeSources(SysApp compositeApp, Set<Long> selectedMenuIds,
                                                               Map<Long, SysMenu> packageMenus,
                                                               Map<Long, SysApp> sourceApps, Long userId) {
        Map<Long, Set<Long>> bySource = new LinkedHashMap<>();
        for (Long menuId : selectedMenuIds) {
            SysMenu menu = packageMenus.get(menuId);
            if (menu != null && sourceApps.containsKey(menu.getAppId())) {
                bySource.computeIfAbsent(menu.getAppId(), key -> new LinkedHashSet<>()).add(menuId);
            }
        }
        return bySource.entrySet().stream()
            .sorted(Comparator
                .comparing((Map.Entry<Long, Set<Long>> entry) -> sourceApps.get(entry.getKey()).getOrderNum(), Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Map.Entry::getKey))
            .map(entry -> buildSource(sourceApps.get(entry.getKey()), entry.getValue(), userId, true))
            .filter(Objects::nonNull)
            .toList();
    }

    private AuthorizedAppSourceVo buildSource(SysApp sourceApp, Set<Long> candidateMenuIds, Long userId,
                                               boolean compositeMode) {
        if (candidateMenuIds.isEmpty()) {
            return null;
        }
        List<SysMenu> flatMenus = LoginHelper.isCurrentSuperAdmin(userId)
            ? menuMapper.selectMenuTreeAll(candidateMenuIds)
            : menuMapper.selectMenuTreeByUserId(userId, candidateMenuIds);
        boolean hasPage = flatMenus.stream().anyMatch(menu -> SystemConstants.TYPE_MENU.equals(menu.getMenuType()));
        if (!hasPage) {
            return null;
        }
        List<SysMenu> tree = TreeBuildUtils.build(flatMenus, Constants.TOP_PARENT_ID, SysMenu::getParentId,
            (menu, nodeTreeMaps) -> menu.setChildren(nodeTreeMaps.getOrDefault(menu.getMenuId(), Collections.emptyList())));
        if (CollUtil.isEmpty(tree)) {
            return null;
        }
        Set<Long> permissionMenuIds = menuMapper.lambda()
            .select(SysMenu::getMenuId)
            .in(SysMenu::getMenuId, candidateMenuIds)
            .in(SysMenu::getMenuType, SystemConstants.TYPE_MENU, SystemConstants.TYPE_BUTTON)
            .eq(SysMenu::getStatus, SystemConstants.NORMAL)
            .list().stream()
            .filter(Objects::nonNull)
            .map(SysMenu::getMenuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<String> permissions;
        if (permissionMenuIds.isEmpty()) {
            permissions = Set.of();
        } else if (LoginHelper.isCurrentSuperAdmin(userId)) {
            permissions = Set.of("*:*:*");
        } else {
            permissions = new TreeSet<>(menuMapper.selectMenuPermsByUserId(userId, permissionMenuIds));
        }
        AuthorizedAppSourceVo source = new AuthorizedAppSourceVo();
        source.setAppId(sourceApp.getAppId());
        source.setAppKey(sourceApp.getAppKey());
        source.setAppName(sourceApp.getAppName());
        source.setEntry(sourceApp.getEntry());
        source.setInitialPath(StringUtils.blankToDefault(sourceApp.getInitialPath(), "/"));
        source.setAlive(sourceApp.getAlive());
        source.setSync(compositeMode ? false : sourceApp.getSync());
        source.setMenus(menuService.buildMenus(tree));
        source.setPermissions(permissions);
        return source;
    }

    private AuthorizedAppVo toAuthorizedApp(SysApp app, List<AuthorizedAppSourceVo> sources) {
        AuthorizedAppVo vo = new AuthorizedAppVo();
        vo.setAppId(app.getAppId());
        vo.setAppKey(app.getAppKey());
        vo.setAppName(app.getAppName());
        vo.setAppType(app.getAppType());
        vo.setIcon(app.getIcon());
        vo.setOrderNum(app.getOrderNum());
        vo.setSources(sources);
        return vo;
    }
}
