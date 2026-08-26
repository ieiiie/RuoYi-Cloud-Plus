package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.system.domain.SysTenant;
import org.dromara.system.domain.SysTenantPackage;
import org.dromara.system.domain.bo.SysTenantPackageBo;
import org.dromara.system.domain.vo.SysTenantPackageVo;
import org.dromara.system.mapper.SysTenantMapper;
import org.dromara.system.mapper.SysTenantPackageMapper;
import org.dromara.system.service.ISysTenantPackageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 租户套餐服务实现。
 *
 * <p>套餐和租户主数据均为平台数据，已在租户行级插件的排除表清单中，
 * 因此平台管理员无论当前选择哪个业务租户，都可统一维护套餐。</p>
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysTenantPackageServiceImpl implements ISysTenantPackageService {

    private final SysTenantPackageMapper tenantPackageMapper;
    private final SysTenantMapper tenantMapper;

    @Override
    public SysTenantPackageVo queryById(Long packageId) {
        return tenantPackageMapper.selectVoById(packageId);
    }

    @Override
    public PageResult<SysTenantPackageVo> queryPageList(SysTenantPackageBo bo, PageQuery pageQuery) {
        Page<SysTenantPackageVo> page = tenantPackageMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<SysTenantPackageVo> queryList(SysTenantPackageBo bo) {
        return tenantPackageMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public List<SysTenantPackageVo> selectEnabledList() {
        return tenantPackageMapper.lambda()
            .eq(SysTenantPackage::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SysTenantPackage::getPackageId)
            .voList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysTenantPackageBo bo) {
        SysTenantPackage tenantPackage = MapstructUtils.convert(bo, SysTenantPackage.class);
        tenantPackage.setMenuIds(joinMenuIds(bo.getMenuIds()));
        return tenantPackageMapper.insert(tenantPackage) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysTenantPackageBo bo) {
        SysTenantPackage tenantPackage = MapstructUtils.convert(bo, SysTenantPackage.class);
        tenantPackage.setMenuIds(joinMenuIds(bo.getMenuIds()));
        return tenantPackageMapper.updateById(tenantPackage) > 0;
    }

    @Override
    public int updatePackageStatus(SysTenantPackageBo bo) {
        SysTenantPackage tenantPackage = new SysTenantPackage();
        tenantPackage.setPackageId(bo.getPackageId());
        tenantPackage.setStatus(bo.getStatus());
        return tenantPackageMapper.updateById(tenantPackage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (Boolean.TRUE.equals(isValid) && tenantMapper.lambda()
            .in(SysTenant::getPackageId, ids)
            .exists()) {
            throw new ServiceException("租户套餐已被使用，无法删除");
        }
        return tenantPackageMapper.deleteByIds(ids) > 0;
    }

    @Override
    public boolean checkPackageNameUnique(SysTenantPackageBo bo) {
        boolean exists = tenantPackageMapper.lambda()
            .eq(SysTenantPackage::getPackageName, bo.getPackageName())
            .neIfPresent(SysTenantPackage::getPackageId, bo.getPackageId())
            .exists();
        return !exists;
    }

    private LambdaQueryWrapper<SysTenantPackage> buildQueryWrapper(SysTenantPackageBo bo) {
        return new LambdaQueryWrapper<SysTenantPackage>()
            .like(StringUtils.isNotBlank(bo.getPackageName()), SysTenantPackage::getPackageName, bo.getPackageName())
            .eq(StringUtils.isNotBlank(bo.getStatus()), SysTenantPackage::getStatus, bo.getStatus())
            .orderByAsc(SysTenantPackage::getPackageId);
    }

    /**
     * 将前端数组保存为稳定的逗号分隔格式，空数组表示该套餐不授权任何菜单。
     */
    private String joinMenuIds(Long[] menuIds) {
        if (ObjectUtil.isEmpty(menuIds) || CollUtil.isEmpty(Arrays.asList(menuIds))) {
            return StringUtils.EMPTY;
        }
        return StringUtils.join(",", Arrays.asList(menuIds));
    }

}
