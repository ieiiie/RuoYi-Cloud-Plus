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
import com.ym.system.saas.domain.bo.SaasRoleTemplateBo;
import com.ym.system.saas.domain.vo.*;
import com.ym.system.saas.mapper.*;
import com.ym.system.saas.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SaaS 角色模板运营服务实现。
 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasRoleTemplateServiceImpl implements ISaasRoleTemplateService {
    private final SaasRoleTemplateMapper templateMapper;
    private final SaasAppMapper appMapper;
    private final SaasMenuMapper menuMapper;
    private final SaasRoleTemplateMenuMapper templateMenuMapper;
    private final SaasRoleMapper roleMapper;
    private final SaasTenantProvisionService provisionService;
    private final SaasControlNotifier controlNotifier;

    @Override
    public PageResult<SaasRoleTemplateVo> queryPageList(SaasRoleTemplateBo bo, PageQuery query) {
        Page<SaasRoleTemplateVo> page = templateMapper.selectVoPage(query.build(), QueryBuilder.lambda(SaasRoleTemplate.class)
            .likeIfText(SaasRoleTemplate::getTemplateName, bo.getTemplateName()).likeIfText(SaasRoleTemplate::getTemplateKey, bo.getTemplateKey())
            .eqIfPresent(SaasRoleTemplate::getAppId, bo.getAppId()).eqIfText(SaasRoleTemplate::getStatus, bo.getStatus())
            .orderByAsc(SaasRoleTemplate::getAppId).orderByAsc(SaasRoleTemplate::getRoleSort).build());
        Map<Long, String> apps = appMapper.selectVoList().stream().collect(Collectors.toMap(SaasAppVo::getAppId, SaasAppVo::getAppName));
        page.getRecords().forEach(vo -> {
            vo.setAppName(apps.get(vo.getAppId()));
            vo.setMenuIds(menuIds(vo.getTemplateId()));
        });
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public SaasRoleTemplateVo queryById(Long id) {
        SaasRoleTemplateVo vo = templateMapper.selectVoById(id);
        if (vo != null) vo.setMenuIds(menuIds(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(SaasRoleTemplateBo bo) {
        validate(bo, null);
        SaasRoleTemplate entity = MapstructUtils.convert(bo, SaasRoleTemplate.class);
        entity.setTemplateId(IdGeneratorUtil.nextLongId());
        entity.setTemplateVersion(1);
        entity.setTenantDeletable(Boolean.TRUE.equals(bo.getTenantDeletable()));
        entity.setDelFlag("0");
        templateMapper.insert(entity);
        replaceMenus(entity.getTemplateId(), bo.getMenuIds());
        return entity.getTemplateId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateByBo(SaasRoleTemplateBo bo) {
        SaasRoleTemplate old = templateMapper.selectById(bo.getTemplateId());
        if (old == null) throw new ServiceException("角色模板不存在");
        validate(bo, bo.getTemplateId());
        SaasRoleTemplate entity = MapstructUtils.convert(bo, SaasRoleTemplate.class);
        entity.setTemplateVersion(old.getTemplateVersion() + 1);
        entity.setTenantDeletable(Boolean.TRUE.equals(bo.getTenantDeletable()));
        templateMapper.updateById(entity);
        replaceMenus(entity.getTemplateId(), bo.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithValidByIds(Collection<Long> ids) {
        if (roleMapper.selectCount(QueryBuilder.lambda(SaasRole.class).inIfNotEmpty(SaasRole::getTemplateId, ids).build()) > 0) {
            throw new ServiceException("模板已下发到租户，只能停用");
        }
        templateMenuMapper.delete(QueryBuilder.lambda(SaasRoleTemplateMenu.class).inIfNotEmpty(SaasRoleTemplateMenu::getTemplateId, ids).build());
        templateMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sync(Long templateId, Collection<String> tenantIds) {
        provisionService.syncRoleTemplate(templateId, tenantIds);
        controlNotifier.invalidateTenantSessionsAfterCommit(tenantIds);
    }

    private void validate(SaasRoleTemplateBo bo, Long id) {
        if ("2".equals(bo.getDataScope())) throw new ServiceException("角色模板不支持自定义部门数据权限");
        if (Objects.equals(id, SaasTenantProvisionService.TENANT_ADMIN_TEMPLATE_ID)
            && Boolean.TRUE.equals(bo.getTenantDeletable())) {
            throw new ServiceException("租户管理员角色不允许租户删除");
        }
        if (templateMapper.lambda().eq(SaasRoleTemplate::getTemplateKey, bo.getTemplateKey())
            .neIfPresent(SaasRoleTemplate::getTemplateId, id).exists()) throw new ServiceException("模板键已存在");
        if (bo.getAppId() != null) {
            SaasApp app = appMapper.selectById(bo.getAppId());
            if (app == null) throw new ServiceException("所属应用不存在");
            if ("COMPOSITE".equals(app.getAppType())) throw new ServiceException("组合应用不能绑定角色模板");
            List<Long> ids = bo.getMenuIds() == null ? List.of() : bo.getMenuIds();
            if (!ids.isEmpty() && menuMapper.lambda().in(SaasMenu::getMenuId, ids).eq(SaasMenu::getAppId, bo.getAppId()).count() != ids.size()) {
                throw new ServiceException("模板菜单必须属于模板应用");
            }
        }
    }

    private void replaceMenus(Long id, Collection<Long> ids) {
        templateMenuMapper.delete(QueryBuilder.lambda(SaasRoleTemplateMenu.class).eq(SaasRoleTemplateMenu::getTemplateId, id).build());
        if (ids != null) ids.forEach(menuId -> templateMenuMapper.insert(new SaasRoleTemplateMenu(id, menuId)));
    }

    private List<Long> menuIds(Long id) {
        return templateMenuMapper.selectList(QueryBuilder.lambda(SaasRoleTemplateMenu.class)
            .eq(SaasRoleTemplateMenu::getTemplateId, id).build()).stream().map(SaasRoleTemplateMenu::getMenuId).toList();
    }
}
