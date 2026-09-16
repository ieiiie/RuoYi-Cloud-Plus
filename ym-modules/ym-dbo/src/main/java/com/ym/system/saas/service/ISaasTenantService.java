package com.ym.system.saas.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.saas.domain.bo.SaasTenantBo;
import com.ym.system.saas.domain.vo.SaasTenantVo;

import java.util.Collection;

/**
 * SaaS 租户运营服务。
 */
public interface ISaasTenantService {
    PageResult<SaasTenantVo> queryPageList(SaasTenantBo bo, PageQuery pageQuery);

    SaasTenantVo queryById(Long id);

    Long insertByBo(SaasTenantBo bo);

    void updateByBo(SaasTenantBo bo);

    void deleteWithValidByIds(Collection<Long> ids);
}
