package com.ym.system.saas.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.saas.domain.bo.SaasConfigDefinitionBo;
import com.ym.system.saas.domain.vo.SaasConfigDefinitionVo;

import java.util.Collection;

/**
 * SaaS 参数定义运营服务。
 */
public interface ISaasConfigDefinitionService {
    PageResult<SaasConfigDefinitionVo> queryPageList(SaasConfigDefinitionBo bo, PageQuery pageQuery);

    SaasConfigDefinitionVo queryById(Long id);

    Long insertByBo(SaasConfigDefinitionBo bo);

    void updateByBo(SaasConfigDefinitionBo bo);

    void disableByIds(Collection<Long> ids);
}
