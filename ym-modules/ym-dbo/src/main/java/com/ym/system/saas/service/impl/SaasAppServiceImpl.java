package com.ym.system.saas.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.ym.system.saas.domain.*;
import com.ym.system.saas.domain.bo.SaasAppBo;
import com.ym.system.saas.domain.vo.SaasAppVo;
import com.ym.system.saas.mapper.*;
import com.ym.system.saas.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * SaaS 应用运营服务实现。
 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasAppServiceImpl implements ISaasAppService {
    private static final String APP_TYPE_CORE = "CORE";
    private static final String APP_TYPE_MICRO = "MICRO";
    private static final String APP_TYPE_COMPOSITE = "COMPOSITE";
    private static final Set<String> APP_TYPES = Set.of(APP_TYPE_CORE, APP_TYPE_MICRO, APP_TYPE_COMPOSITE);
    private final SaasAppMapper appMapper;
    private final SaasMenuMapper menuMapper;
    private final SaasCompositeAppMenuMapper compositeAppMenuMapper;
    private final SaasPackageAppMapper packageAppMapper;
    private final SaasTenantMapper tenantMapper;
    private final SaasControlNotifier controlNotifier;

    @Override
    public PageResult<SaasAppVo> queryPageList(SaasAppBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SaasApp> lqw = QueryBuilder.lambda(SaasApp.class)
            .likeIfText(SaasApp::getAppName, bo.getAppName())
            .likeIfText(SaasApp::getAppKey, bo.getAppKey())
            .eqIfText(SaasApp::getAppType, bo.getAppType())
            .eqIfText(SaasApp::getStatus, bo.getStatus())
            .orderByAsc(SaasApp::getOrderNum).orderByAsc(SaasApp::getAppId).build();
        Page<SaasAppVo> page = appMapper.selectVoPage(pageQuery.build(), lqw);
        page.getRecords().forEach(this::fillCompositeMenus);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public SaasAppVo queryById(Long appId) {
        SaasAppVo vo = appMapper.selectVoById(appId);
        if (vo != null) {
            fillCompositeMenus(vo);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(SaasAppBo bo) {
        if (APP_TYPE_COMPOSITE.equals(bo.getAppType()) && bo.getStatus() == null) {
            bo.setStatus("1");
        }
        validate(bo, null);
        SaasApp entity = MapstructUtils.convert(bo, SaasApp.class);
        entity.setAppId(IdGeneratorUtil.nextLongId());
        entity.setDelFlag("0");
        appMapper.insert(entity);
        replaceCompositeMenus(entity.getAppId(), bo.getMenuIds());
        return entity.getAppId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateByBo(SaasAppBo bo) {
        SaasApp old = requireLockedApp(bo.getAppId());
        boolean replaceMenus = bo.getMenuIds() != null;
        if (!Objects.equals(old.getAppKey(), bo.getAppKey())) {
            throw new ServiceException("应用创建后不能修改应用标识");
        }
        if (bo.getStatus() == null) bo.setStatus(old.getStatus());
        validate(bo, bo.getAppId());
        if (!Objects.equals(old.getAppType(), bo.getAppType())) {
            throw new ServiceException("应用创建后不能修改应用类型");
        }
        SaasApp entity = MapstructUtils.convert(bo, SaasApp.class);
        appMapper.updateById(entity);
        if (replaceMenus) replaceCompositeMenus(entity.getAppId(), bo.getMenuIds());
        publishAuth(bo.getAppId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenus(Long appId, Collection<Long> menuIds) {
        SaasApp app = requireLockedApp(appId);
        if (!APP_TYPE_COMPOSITE.equals(app.getAppType())) {
            throw new ServiceException("仅组合应用可以配置源菜单");
        }
        if (menuIds == null) throw new ServiceException("请提供菜单列表");
        replaceCompositeMenus(appId, validateCompositeMenus(menuIds, app.getStatus()));
        publishAuth(appId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithValidByIds(Collection<Long> appIds) {
        for (Long appId : appIds) {
            SaasApp app = requireApp(appId);
            if ("saas-core".equals(app.getAppKey())) throw new ServiceException("saas-core不能删除");
            if (menuMapper.lambda().eq(SaasMenu::getAppId, appId).exists())
                throw new ServiceException("应用下仍有菜单，不能删除");
            if (packageAppMapper.selectCount(QueryBuilder.lambda(SaasPackageApp.class).eq(SaasPackageApp::getAppId, appId).build()) > 0) {
                throw new ServiceException("应用仍被租户套餐使用，不能删除");
            }
            compositeAppMenuMapper.delete(QueryBuilder.lambda(SaasCompositeAppMenu.class)
                .eq(SaasCompositeAppMenu::getAppId, appId).build());
        }
        appMapper.deleteByIds(appIds);
    }

    private void validate(SaasAppBo bo, Long excludeId) {
        if (!APP_TYPE_CORE.equals(bo.getAppType())) {
            com.ym.common.core.utils.ApplicationEntryPolicy.requireApplicationKey(bo.getAppKey());
            com.ym.common.core.utils.ApplicationEntryPolicy.resolveLoginTheme(bo.getLoginTheme());
        }
        if (!APP_TYPES.contains(bo.getAppType())) throw new ServiceException("应用类型只能为CORE、MICRO或COMPOSITE");
        if (APP_TYPE_MICRO.equals(bo.getAppType())) {
            String entry = bo.getEntry();
            if (entry == null || !entry.startsWith("/micro-apps/" + bo.getAppKey() + "/") || entry.contains("://") || entry.startsWith("//")) {
                throw new ServiceException("微应用入口必须是/micro-apps/{appKey}/下的同域相对地址");
            }
            bo.setMenuIds(List.of());
        } else if (APP_TYPE_COMPOSITE.equals(bo.getAppType())) {
            bo.setEntry(null);
            bo.setInitialPath("/");
            bo.setAlive(true);
            bo.setSync(false);
            Collection<Long> selected = bo.getMenuIds();
            if (selected == null && excludeId != null) {
                selected = compositeAppMenuMapper.selectList(QueryBuilder.lambda(SaasCompositeAppMenu.class)
                    .eq(SaasCompositeAppMenu::getAppId, excludeId).build()).stream()
                    .map(SaasCompositeAppMenu::getMenuId).toList();
            }
            bo.setMenuIds(validateCompositeMenus(selected, bo.getStatus()));
        } else {
            bo.setEntry(null);
            bo.setMenuIds(List.of());
        }
        boolean exists = appMapper.lambda().eq(SaasApp::getAppKey, bo.getAppKey()).neIfPresent(SaasApp::getAppId, excludeId).exists();
        if (exists) throw new ServiceException("应用标识已存在");
    }

    /** 校验选择范围并补齐每个所选菜单在源微应用中的祖先链。 */
    private List<Long> validateCompositeMenus(Collection<Long> selectedMenuIds, String status) {
        Map<Long, SaasMenu> menus = menuMapper.selectList().stream()
            .collect(java.util.stream.Collectors.toMap(SaasMenu::getMenuId, item -> item));
        Map<Long, SaasApp> apps = appMapper.selectList().stream()
            .collect(java.util.stream.Collectors.toMap(SaasApp::getAppId, item -> item));
        return CompositeAppMenuSelection.complete(selectedMenuIds, status, menus, apps);
    }

    private void replaceCompositeMenus(Long appId, Collection<Long> menuIds) {
        compositeAppMenuMapper.delete(QueryBuilder.lambda(SaasCompositeAppMenu.class)
            .eq(SaasCompositeAppMenu::getAppId, appId).build());
        if (menuIds != null) {
            menuIds.forEach(menuId -> compositeAppMenuMapper.insert(new SaasCompositeAppMenu(appId, menuId)));
        }
    }

    private void fillCompositeMenus(SaasAppVo vo) {
        if (!APP_TYPE_COMPOSITE.equals(vo.getAppType())) {
            vo.setMenuIds(List.of());
            return;
        }
        vo.setMenuIds(compositeAppMenuMapper.selectList(QueryBuilder.lambda(SaasCompositeAppMenu.class)
                .eq(SaasCompositeAppMenu::getAppId, vo.getAppId()).build()).stream()
            .map(SaasCompositeAppMenu::getMenuId).toList());
    }

    private SaasApp requireApp(Long id) {
        SaasApp app = appMapper.selectById(id);
        if (app == null) throw new ServiceException("应用不存在");
        return app;
    }

    /** 基本信息与菜单独立提交时，锁住同一应用，避免启用与清空菜单并发穿透校验。 */
    private SaasApp requireLockedApp(Long id) {
        SaasApp app = appMapper.selectOne(QueryBuilder.lambda(SaasApp.class)
            .eq(SaasApp::getAppId, id).last("FOR UPDATE").build());
        if (app == null) throw new ServiceException("应用不存在");
        return app;
    }

    private void publishAuth(Long appId) {
        LinkedHashSet<Long> affectedAppIds = new LinkedHashSet<>();
        affectedAppIds.add(appId);
        List<Long> sourceMenuIds = menuMapper.lambda().eq(SaasMenu::getAppId, appId)
            .select(SaasMenu::getMenuId).list().stream().map(SaasMenu::getMenuId).toList();
        if (!sourceMenuIds.isEmpty()) {
            compositeAppMenuMapper.selectList(QueryBuilder.lambda(SaasCompositeAppMenu.class)
                    .inIfNotEmpty(SaasCompositeAppMenu::getMenuId, sourceMenuIds).build()).stream()
                .map(SaasCompositeAppMenu::getAppId).forEach(affectedAppIds::add);
        }
        List<Long> packageIds = packageAppMapper.selectList(QueryBuilder.lambda(SaasPackageApp.class)
            .inIfNotEmpty(SaasPackageApp::getAppId, affectedAppIds).build()).stream().map(SaasPackageApp::getPackageId).distinct().toList();
        if (packageIds.isEmpty()) return;
        List<String> tenantIds = tenantMapper.lambda().in(SaasTenant::getPackageId, packageIds).list()
            .stream().map(SaasTenant::getTenantId).toList();
        if (!tenantIds.isEmpty()) controlNotifier.invalidateTenantSessionsAfterCommit(tenantIds);
    }
}
