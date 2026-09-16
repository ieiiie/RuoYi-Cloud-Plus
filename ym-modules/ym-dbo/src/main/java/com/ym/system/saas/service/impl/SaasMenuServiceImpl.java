package com.ym.system.saas.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.ym.system.saas.domain.*;
import com.ym.system.saas.domain.bo.SaasMenuBo;
import com.ym.system.saas.domain.vo.SaasMenuVo;
import com.ym.system.saas.domain.vo.SaasAppVo;
import com.ym.system.saas.mapper.*;
import com.ym.system.saas.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SaaS 租户菜单运营服务实现。
 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasMenuServiceImpl implements ISaasMenuService {
    private final SaasMenuMapper menuMapper;
    private final SaasAppMapper appMapper;
    private final SaasTenantMapper tenantMapper;
    private final SaasPackageMenuMapper packageMenuMapper;
    private final SaasRoleMenuMapper roleMenuMapper;
    private final SaasRoleTemplateMenuMapper templateMenuMapper;
    private final SaasCompositeAppMenuMapper compositeAppMenuMapper;
    private final SaasControlNotifier controlNotifier;

    @Override
    public List<SaasMenuVo> queryList(SaasMenuBo bo) {
        List<SaasMenuVo> list = menuMapper.selectVoList(QueryBuilder.lambda(SaasMenu.class)
            .eqIfPresent(SaasMenu::getAppId, bo.getAppId()).likeIfText(SaasMenu::getMenuName, bo.getMenuName())
            .eqIfText(SaasMenu::getStatus, bo.getStatus()).eqIfText(SaasMenu::getVisible, bo.getVisible())
            .orderByAsc(SaasMenu::getAppId)
            .orderByAsc(SaasMenu::getParentId).orderByAsc(SaasMenu::getOrderNum).build());
        Map<Long, String> names = appMapper.selectVoList().stream().collect(Collectors.toMap(SaasAppVo::getAppId, SaasAppVo::getAppName));
        list.forEach(vo -> vo.setAppName(names.get(vo.getAppId())));
        return list;
    }

    @Override
    public SaasMenuVo queryById(Long menuId) {
        return menuMapper.selectVoById(menuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(SaasMenuBo bo) {
        validate(bo, null);
        SaasMenu entity = MapstructUtils.convert(bo, SaasMenu.class);
        entity.setMenuId(IdGeneratorUtil.nextLongId());
        menuMapper.insert(entity);
        return entity.getMenuId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateByBo(SaasMenuBo bo) {
        requireMenu(bo.getMenuId());
        validate(bo, bo.getMenuId());
        menuMapper.updateById(MapstructUtils.convert(bo, SaasMenu.class));
        publishAuth(bo.getMenuId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithValidByIds(Collection<Long> menuIds) {
        List<String> tenantIds = affectedTenantIds(menuIds);
        for (Long id : menuIds) {
            if (menuMapper.lambda().eq(SaasMenu::getParentId, id).exists())
                throw new ServiceException("存在子菜单，不能删除");
        }
        roleMenuMapper.delete(QueryBuilder.lambda(SaasRoleMenu.class).inIfNotEmpty(SaasRoleMenu::getMenuId, menuIds).build());
        packageMenuMapper.delete(QueryBuilder.lambda(SaasPackageMenu.class).inIfNotEmpty(SaasPackageMenu::getMenuId, menuIds).build());
        templateMenuMapper.delete(QueryBuilder.lambda(SaasRoleTemplateMenu.class).inIfNotEmpty(SaasRoleTemplateMenu::getMenuId, menuIds).build());
        compositeAppMenuMapper.delete(QueryBuilder.lambda(SaasCompositeAppMenu.class)
            .inIfNotEmpty(SaasCompositeAppMenu::getMenuId, menuIds).build());
        menuMapper.deleteByIds(menuIds);
        if (!tenantIds.isEmpty()) controlNotifier.invalidateTenantSessionsAfterCommit(tenantIds);
    }

    private void validate(SaasMenuBo bo, Long excludeId) {
        SaasApp app = appMapper.selectById(bo.getAppId());
        if (app == null || !"0".equals(app.getStatus())) throw new ServiceException("所属应用不存在或已停用");
        if ("COMPOSITE".equals(app.getAppType())) throw new ServiceException("组合应用不能直接创建菜单");
        long parentId = bo.getParentId() == null ? 0L : bo.getParentId();
        if (Objects.equals(excludeId, parentId)) throw new ServiceException("上级菜单不能选择自己");
        if (parentId != 0L) {
            SaasMenu parent = requireMenu(parentId);
            if (!Objects.equals(parent.getAppId(), bo.getAppId()))
                throw new ServiceException("父子菜单必须属于同一应用");
        } else if ("MICRO".equals(app.getAppType()) && menuMapper.lambda().eq(SaasMenu::getAppId, bo.getAppId())
            .eq(SaasMenu::getParentId, 0L).neIfPresent(SaasMenu::getMenuId, excludeId).exists()) {
            throw new ServiceException("微应用只能有一个顶级目录");
        }
    }

    private SaasMenu requireMenu(Long id) {
        SaasMenu menu = menuMapper.selectById(id);
        if (menu == null) throw new ServiceException("菜单不存在");
        return menu;
    }

    private void publishAuth(Long menuId) {
        List<String> ids = affectedTenantIds(List.of(menuId));
        if (!ids.isEmpty()) controlNotifier.invalidateTenantSessionsAfterCommit(ids);
    }

    private List<String> affectedTenantIds(Collection<Long> menuIds) {
        List<Long> packageIds = packageMenuMapper.selectList(QueryBuilder.lambda(SaasPackageMenu.class)
            .inIfNotEmpty(SaasPackageMenu::getMenuId, menuIds).build()).stream().map(SaasPackageMenu::getPackageId).distinct().toList();
        if (packageIds.isEmpty()) return List.of();
        return tenantMapper.lambda().in(SaasTenant::getPackageId, packageIds).list().stream()
            .map(SaasTenant::getTenantId).distinct().toList();
    }
}
