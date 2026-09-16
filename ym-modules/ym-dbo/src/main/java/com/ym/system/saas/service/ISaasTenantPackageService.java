package com.ym.system.saas.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.saas.domain.bo.SaasTenantPackageBo;
import com.ym.system.saas.domain.vo.SaasTenantPackageVo;

import java.util.Collection;

/**
 * SaaS 租户套餐运营服务。
 */
public interface ISaasTenantPackageService {
    PageResult<SaasTenantPackageVo> queryPageList(SaasTenantPackageBo bo, PageQuery pageQuery);

    SaasTenantPackageVo queryById(Long id);

    Long insertByBo(SaasTenantPackageBo bo);

    void updateByBo(SaasTenantPackageBo bo);

    void updateStatus(SaasTenantPackageBo bo);

    void deleteWithValidByIds(Collection<Long> ids);
}
