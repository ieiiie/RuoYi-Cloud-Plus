package com.ym.system.saas.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.saas.domain.bo.SaasTenantDictTypeBo;
import com.ym.system.saas.domain.vo.SaasTenantDictTypeVo;

import java.util.Collection;

public interface ISaasTenantDictTypeService {
    PageResult<SaasTenantDictTypeVo> queryPage(SaasTenantDictTypeBo bo, PageQuery pageQuery);
    SaasTenantDictTypeVo queryById(Long id);
    Long insert(SaasTenantDictTypeBo bo);
    void update(SaasTenantDictTypeBo bo);
    void deleteByIds(Collection<Long> ids);
}
