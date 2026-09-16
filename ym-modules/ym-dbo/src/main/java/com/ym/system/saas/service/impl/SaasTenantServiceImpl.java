package com.ym.system.saas.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.system.saas.domain.*;
import com.ym.system.saas.domain.bo.SaasTenantBo;
import com.ym.system.saas.domain.vo.*;
import com.ym.system.saas.mapper.*;
import com.ym.system.saas.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SaaS 租户运营服务实现。
 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasTenantServiceImpl implements ISaasTenantService {
    private final SaasTenantMapper tenantMapper;
    private final SaasTenantPackageMapper packageMapper;
    private final SaasOssConfigMapper ossConfigMapper;
    private final SaasRegionMapper regionMapper;
    private final SaasTenantProvisionService provisionService;
    private final SaasControlNotifier controlNotifier;

    @Override
    public PageResult<SaasTenantVo> queryPageList(SaasTenantBo bo, PageQuery query) {
        Page<SaasTenantVo> page = tenantMapper.selectVoPage(query.build(), QueryBuilder.lambda(SaasTenant.class)
            .likeIfText(SaasTenant::getCompanyName, bo.getCompanyName()).likeIfText(SaasTenant::getContactUserName, bo.getContactUserName())
            .eqIfText(SaasTenant::getTenantId, bo.getTenantId()).eqIfPresent(SaasTenant::getPackageId, bo.getPackageId())
            .eqIfText(SaasTenant::getStatus, bo.getStatus()).orderByAsc(SaasTenant::getId).build());
        Map<Long, String> packages = packageMapper.selectVoList().stream()
            .collect(Collectors.toMap(SaasTenantPackageVo::getPackageId, SaasTenantPackageVo::getPackageName));
        Map<Long, String> ossConfigs = ossConfigMapper.selectList().stream()
            .collect(Collectors.toMap(SaasOssConfig::getOssConfigId, SaasOssConfig::getConfigKey));
        page.getRecords().forEach(vo -> {
            vo.setPackageName(packages.get(vo.getPackageId()));
            vo.setOssConfigKey(ossConfigs.get(vo.getOssConfigId()));
        });
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public SaasTenantVo queryById(Long id) {
        SaasTenantVo tenant = tenantMapper.selectVoById(id);
        if (tenant != null && tenant.getOssConfigId() != null) {
            SaasOssConfig config = ossConfigMapper.selectById(tenant.getOssConfigId());
            tenant.setOssConfigKey(config == null ? null : config.getConfigKey());
        }
        return tenant;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(SaasTenantBo bo) {
        normalizeContactPhone(bo);
        normalizeRegion(bo);
        return provisionService.createTenant(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateByBo(SaasTenantBo bo) {
        SaasTenant old = tenantMapper.selectById(bo.getId());
        if (old == null) throw new ServiceException("租户不存在");
        if (packageMapper.selectById(bo.getPackageId()) == null) throw new ServiceException("租户套餐不存在");
        if (ossConfigMapper.selectById(bo.getOssConfigId()) == null) throw new ServiceException("OSS配置不存在");
        normalizeContactPhone(bo);
        normalizeRegion(bo);
        SaasTenant entity = MapstructUtils.convert(bo, SaasTenant.class);
        entity.setTenantId(old.getTenantId());
        if (tenantMapper.updateById(entity) != 1) throw new ServiceException("租户更新失败，请刷新后重试");
        if (!Objects.equals(old.getPackageId(), entity.getPackageId())) {
            provisionService.synchronizeTenantRoleMenus(old.getTenantId(), entity.getPackageId());
        }
        controlNotifier.invalidateTenantSessionsAfterCommit(List.of(old.getTenantId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithValidByIds(Collection<Long> ids) {
        List<SaasTenant> tenants = tenantMapper.selectByIds(ids);
        if (tenants.stream().anyMatch(item -> "000000".equals(item.getTenantId())))
            throw new ServiceException("默认管理租户不能删除");
        tenantMapper.deleteByIds(ids);
        List<String> tenantIds = tenants.stream().map(SaasTenant::getTenantId).toList();
        if (!tenantIds.isEmpty()) controlNotifier.invalidateTenantSessionsAfterCommit(tenantIds);
    }

    /**
     * 校验省、市、区县的层级关系，并以权威区划表中的名称覆盖客户端传值。
     */
    private void normalizeRegion(SaasTenantBo bo) {
        SaasRegion province = requireRegion(bo.getProvinceCode(), "province", "省级");
        SaasRegion city = requireRegion(bo.getCityCode(), "city", "市级");
        SaasRegion district = requireRegion(bo.getDistrictCode(), "district", "区县级");

        if (!Objects.equals(city.getParentId(), parseAdcode(province.getAdcode()))
            || !Objects.equals(district.getParentId(), parseAdcode(city.getAdcode()))) {
            throw new ServiceException("省、市、区县行政区划层级不匹配");
        }

        bo.setProvinceCode(province.getAdcode());
        bo.setCityCode(city.getAdcode());
        bo.setDistrictCode(district.getAdcode());
        bo.setRegionName(district.getFullName() == null || district.getFullName().isBlank()
            ? String.join("/", province.getRegionName(), city.getRegionName(), district.getRegionName())
            : district.getFullName());
    }

    private void normalizeContactPhone(SaasTenantBo bo) {
        String contactPhone = bo.getContactPhone();
        bo.setContactPhone(contactPhone == null || contactPhone.isBlank() ? null : contactPhone.trim());
    }

    private SaasRegion requireRegion(String adcode, String expectedLevel, String levelName) {
        SaasRegion region = regionMapper.selectOne(QueryBuilder.lambda(SaasRegion.class)
            .eq(SaasRegion::getAdcode, adcode.trim())
            .build());
        if (region == null || !expectedLevel.equalsIgnoreCase(region.getRegionLevel())) {
            throw new ServiceException(levelName + "行政区划不正确");
        }
        return region;
    }

    private static Long parseAdcode(String adcode) {
        try {
            return Long.valueOf(adcode);
        } catch (NumberFormatException e) {
            throw new ServiceException("行政区划编码不正确");
        }
    }
}
