package com.ym.system.saas.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.saas.domain.bo.SaasTenantDictDefaultDataBo;
import com.ym.system.saas.domain.vo.SaasTenantDictDefaultDataVo;

import java.util.Collection;

public interface ISaasTenantDictDefaultDataService {
    PageResult<SaasTenantDictDefaultDataVo> queryPage(SaasTenantDictDefaultDataBo bo, PageQuery pageQuery);
    SaasTenantDictDefaultDataVo queryById(Long id);
    Long insert(SaasTenantDictDefaultDataBo bo);
    void update(SaasTenantDictDefaultDataBo bo);
    void deleteByIds(Collection<Long> ids);
}
