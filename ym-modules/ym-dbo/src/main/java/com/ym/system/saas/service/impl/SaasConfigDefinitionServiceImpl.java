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
import com.ym.system.saas.domain.bo.SaasConfigDefinitionBo;
import com.ym.system.saas.domain.vo.*;
import com.ym.system.saas.mapper.*;
import com.ym.system.saas.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SaaS 参数定义运营服务实现。
 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasConfigDefinitionServiceImpl implements ISaasConfigDefinitionService {
    private static final Set<String> VALUE_TYPES = Set.of("STRING", "INTEGER", "DECIMAL", "BOOLEAN", "ENUM", "JSON", "PASSWORD");
    private final SaasConfigDefinitionMapper definitionMapper;
    private final SaasAppMapper appMapper;
    private final SaasTenantMapper tenantMapper;
    private final SaasPackageAppMapper packageAppMapper;
    private final SaasPackageMenuMapper packageMenuMapper;
    private final SaasMenuMapper menuMapper;
    private final SaasTenantConfigMapper configMapper;
    private final SaasTenantProvisionService provisionService;
    private final SaasControlNotifier controlNotifier;

    @Override
    public PageResult<SaasConfigDefinitionVo> queryPageList(SaasConfigDefinitionBo bo, PageQuery query) {
        Page<SaasConfigDefinitionVo> page = definitionMapper.selectVoPage(query.build(), QueryBuilder.lambda(SaasConfigDefinition.class)
            .likeIfText(SaasConfigDefinition::getConfigName, bo.getConfigName()).likeIfText(SaasConfigDefinition::getConfigKey, bo.getConfigKey())
            .eqIfPresent(SaasConfigDefinition::getAppId, bo.getAppId()).eqIfText(SaasConfigDefinition::getValueType, bo.getValueType())
            .eqIfText(SaasConfigDefinition::getStatus, bo.getStatus()).orderByAsc(SaasConfigDefinition::getOrderNum)
            .orderByAsc(SaasConfigDefinition::getDefinitionId).build());
        Map<Long, String> apps = appMapper.selectVoList().stream().collect(Collectors.toMap(SaasAppVo::getAppId, SaasAppVo::getAppName));
        page.getRecords().forEach(vo -> vo.setAppName(apps.get(vo.getAppId())));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public SaasConfigDefinitionVo queryById(Long id) {
        return definitionMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(SaasConfigDefinitionBo bo) {
        validate(bo, null);
        SaasConfigDefinition entity = MapstructUtils.convert(bo, SaasConfigDefinition.class);
        entity.setDefinitionId(IdGeneratorUtil.nextLongId());
        entity.setIssuedFlag(false);
        entity.setDelFlag("0");
        definitionMapper.insert(entity);
        provisionService.issueDefinition(entity.getDefinitionId());
        publishConfig(entity);
        return entity.getDefinitionId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateByBo(SaasConfigDefinitionBo bo) {
        SaasConfigDefinition old = definitionMapper.selectById(bo.getDefinitionId());
        if (old == null) throw new ServiceException("参数定义不存在");
        validate(bo, bo.getDefinitionId());
        if (!Objects.equals(old.getConfigKey(), bo.getConfigKey()) && configMapper.selectCount(QueryBuilder.lambda(SaasTenantConfig.class)
            .eq(SaasTenantConfig::getDefinitionId, bo.getDefinitionId()).build()) > 0)
            throw new ServiceException("已发放的参数键不能修改");
        SaasConfigDefinition entity = MapstructUtils.convert(bo, SaasConfigDefinition.class);
        entity.setIssuedFlag(old.getIssuedFlag());
        definitionMapper.updateById(entity);
        // 补发已有租户缺失的参数；已有实例保留租户自己的值。
        provisionService.issueDefinition(entity.getDefinitionId());
        publishConfig(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableByIds(Collection<Long> ids) {
        for (SaasConfigDefinition definition : definitionMapper.selectByIds(ids)) {
            definition.setStatus("1");
            definitionMapper.updateById(definition);
            publishConfig(definition);
        }
    }

    private void validate(SaasConfigDefinitionBo bo, Long id) {
        if (!VALUE_TYPES.contains(bo.getValueType())) throw new ServiceException("不支持的参数类型");
        if (definitionMapper.lambda().eq(SaasConfigDefinition::getConfigKey, bo.getConfigKey())
            .neIfPresent(SaasConfigDefinition::getDefinitionId, id).exists())
            throw new ServiceException("参数键已存在");
        if (bo.getAppId() != null) {
            SaasApp app = appMapper.selectById(bo.getAppId());
            if (app == null) throw new ServiceException("所属应用不存在");
            if ("COMPOSITE".equals(app.getAppType())) throw new ServiceException("组合应用不能拥有业务参数定义");
        }
        if (bo.getMinLength() != null && bo.getMaxLength() != null && bo.getMinLength() > bo.getMaxLength())
            throw new ServiceException("最小长度不能大于最大长度");
        if (bo.getMinValue() != null && bo.getMaxValue() != null && bo.getMinValue().compareTo(bo.getMaxValue()) > 0)
            throw new ServiceException("最小值不能大于最大值");
    }

    private void publishConfig(SaasConfigDefinition definition) {
        List<SaasTenant> tenants;
        if (definition.getAppId() == null) tenants = tenantMapper.selectList();
        else {
            LinkedHashSet<Long> packageIds = packageAppMapper.selectList(QueryBuilder.lambda(SaasPackageApp.class)
                    .eq(SaasPackageApp::getAppId, definition.getAppId()).build()).stream()
                .map(SaasPackageApp::getPackageId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            List<Long> sourceMenuIds = menuMapper.selectList(QueryBuilder.lambda(SaasMenu.class)
                    .eq(SaasMenu::getAppId, definition.getAppId()).build()).stream()
                .map(SaasMenu::getMenuId)
                .toList();
            if (!sourceMenuIds.isEmpty()) {
                packageMenuMapper.selectList(QueryBuilder.lambda(SaasPackageMenu.class)
                        .inIfNotEmpty(SaasPackageMenu::getMenuId, sourceMenuIds).build()).stream()
                    .map(SaasPackageMenu::getPackageId)
                    .forEach(packageIds::add);
            }
            tenants = packageIds.isEmpty() ? List.of() : tenantMapper.lambda().in(SaasTenant::getPackageId, packageIds).list();
        }
        controlNotifier.refreshTenantConfigAfterCommit(tenants.stream().map(SaasTenant::getTenantId).toList());
    }
}
