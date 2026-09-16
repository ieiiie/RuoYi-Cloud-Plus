package com.ym.system.saas.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.ym.system.saas.domain.*;
import com.ym.system.saas.domain.bo.SaasTenantPackageBo;
import com.ym.system.saas.domain.vo.SaasTenantPackageVo;
import com.ym.system.saas.mapper.*;
import com.ym.system.saas.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * SaaS 租户套餐运营服务实现。
 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasTenantPackageServiceImpl implements ISaasTenantPackageService {
    private final SaasTenantPackageMapper packageMapper;
    private final SaasAppMapper appMapper;
    private final SaasMenuMapper menuMapper;
    private final SaasTenantMapper tenantMapper;
    private final SaasPackageAppMapper packageAppMapper;
    private final SaasPackageMenuMapper packageMenuMapper;
    private final SaasCompositeAppMenuMapper compositeAppMenuMapper;
    private final SaasTenantProvisionService provisionService;
    private final SaasControlNotifier controlNotifier;

    @Override
    public PageResult<SaasTenantPackageVo> queryPageList(SaasTenantPackageBo bo, PageQuery query) {
        Page<SaasTenantPackageVo> page = packageMapper.selectVoPage(query.build(), QueryBuilder.lambda(SaasTenantPackage.class)
            .likeIfText(SaasTenantPackage::getPackageName, bo.getPackageName()).eqIfText(SaasTenantPackage::getStatus, bo.getStatus())
            .orderByAsc(SaasTenantPackage::getPackageId).build());
        page.getRecords().forEach(this::fillRelations);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public SaasTenantPackageVo queryById(Long id) {
        SaasTenantPackageVo vo = packageMapper.selectVoById(id);
        if (vo != null) fillRelations(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(SaasTenantPackageBo bo) {
        validateAndCompleteMenus(bo);
        SaasTenantPackage entity = MapstructUtils.convert(bo, SaasTenantPackage.class);
        entity.setPackageId(IdGeneratorUtil.nextLongId());
        entity.setDelFlag("0");
        packageMapper.insert(entity);
        replaceRelations(entity.getPackageId(), bo.getAppIds(), bo.getMenuIds());
        return entity.getPackageId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateByBo(SaasTenantPackageBo bo) {
        if (packageMapper.selectById(bo.getPackageId()) == null) throw new ServiceException("租户套餐不存在");
        List<Long> oldApps = appIds(bo.getPackageId());
        validateAndCompleteMenus(bo);
        packageMapper.updateById(MapstructUtils.convert(bo, SaasTenantPackage.class));
        replaceRelations(bo.getPackageId(), bo.getAppIds(), bo.getMenuIds());
        List<String> tenantIds = provisionService.reconcilePackage(bo.getPackageId(), bo.getAppIds(), oldApps);
        if (!tenantIds.isEmpty()) controlNotifier.invalidateTenantSessionsAfterCommit(tenantIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(SaasTenantPackageBo bo) {
        if (bo.getPackageId() == null || !Set.of("0", "1").contains(bo.getStatus())) {
            throw new ServiceException("套餐状态参数不正确");
        }
        if (packageMapper.selectById(bo.getPackageId()) == null) {
            throw new ServiceException("租户套餐不存在");
        }
        SaasTenantPackage entity = new SaasTenantPackage();
        entity.setPackageId(bo.getPackageId());
        entity.setStatus(bo.getStatus());
        packageMapper.updateById(entity);
        List<String> tenantIds = tenantMapper.lambda()
            .eq(SaasTenant::getPackageId, bo.getPackageId())
            .select(SaasTenant::getTenantId)
            .list()
            .stream()
            .map(SaasTenant::getTenantId)
            .toList();
        if (!tenantIds.isEmpty()) {
            controlNotifier.invalidateTenantSessionsAfterCommit(tenantIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithValidByIds(Collection<Long> ids) {
        if (tenantMapper.lambda().in(SaasTenant::getPackageId, ids).exists())
            throw new ServiceException("套餐正在被租户使用");
        packageAppMapper.delete(QueryBuilder.lambda(SaasPackageApp.class).inIfNotEmpty(SaasPackageApp::getPackageId, ids).build());
        packageMenuMapper.delete(QueryBuilder.lambda(SaasPackageMenu.class).inIfNotEmpty(SaasPackageMenu::getPackageId, ids).build());
        packageMapper.deleteByIds(ids);
    }

    private void validateAndCompleteMenus(SaasTenantPackageBo bo) {
        Set<Long> appIds = new LinkedHashSet<>(bo.getAppIds());
        List<SaasApp> selectedApps = appMapper.lambda().in(SaasApp::getAppId, appIds).list();
        if (selectedApps.size() != appIds.size())
            throw new ServiceException("套餐包含不存在的应用");
        Map<Long, SaasMenu> allMenus = menuMapper.selectList().stream().collect(java.util.stream.Collectors.toMap(SaasMenu::getMenuId, item -> item));
        Set<Long> compositeAppIds = selectedApps.stream()
            .filter(app -> "COMPOSITE".equals(app.getAppType()))
            .map(SaasApp::getAppId)
            .collect(java.util.stream.Collectors.toSet());
        Set<Long> directlyAvailableAppIds = selectedApps.stream()
            .filter(app -> !"COMPOSITE".equals(app.getAppType()))
            .map(SaasApp::getAppId)
            .collect(java.util.stream.Collectors.toSet());
        Set<Long> compositeMenuIds = compositeAppIds.isEmpty() ? Set.of() : compositeAppMenuMapper.selectList(
                QueryBuilder.lambda(SaasCompositeAppMenu.class)
                    .inIfNotEmpty(SaasCompositeAppMenu::getAppId, compositeAppIds).build()).stream()
            .map(SaasCompositeAppMenu::getMenuId)
            .collect(java.util.stream.Collectors.toSet());
        bo.setMenuIds(PackageMenuSelection.complete(
            bo.getMenuIds(), allMenus, directlyAvailableAppIds, compositeMenuIds));
    }

    private void replaceRelations(Long packageId, Collection<Long> apps, Collection<Long> menus) {
        packageAppMapper.delete(QueryBuilder.lambda(SaasPackageApp.class).eq(SaasPackageApp::getPackageId, packageId).build());
        packageMenuMapper.delete(QueryBuilder.lambda(SaasPackageMenu.class).eq(SaasPackageMenu::getPackageId, packageId).build());
        apps.forEach(appId -> packageAppMapper.insert(new SaasPackageApp(packageId, appId)));
        menus.forEach(menuId -> packageMenuMapper.insert(new SaasPackageMenu(packageId, menuId)));
    }

    private void fillRelations(SaasTenantPackageVo vo) {
        vo.setAppIds(appIds(vo.getPackageId()));
        vo.setMenuIds(menuIds(vo.getPackageId()));
    }

    private List<Long> appIds(Long id) {
        return packageAppMapper.selectList(QueryBuilder.lambda(SaasPackageApp.class).eq(SaasPackageApp::getPackageId, id).build()).stream().map(SaasPackageApp::getAppId).toList();
    }

    private List<Long> menuIds(Long id) {
        List<Long> ids = packageMenuMapper.selectList(QueryBuilder.lambda(SaasPackageMenu.class)
                .eq(SaasPackageMenu::getPackageId, id).build()).stream()
            .map(SaasPackageMenu::getMenuId)
            .toList();
        if (ids.isEmpty()) {
            return ids;
        }
        Set<Long> existingIds = menuMapper.selectList(QueryBuilder.lambda(SaasMenu.class)
                .inIfNotEmpty(SaasMenu::getMenuId, ids).build()).stream()
            .map(SaasMenu::getMenuId)
            .collect(java.util.stream.Collectors.toSet());
        return ids.stream().filter(existingIds::contains).toList();
    }
}
