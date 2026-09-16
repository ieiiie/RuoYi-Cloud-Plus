package com.ym.system.saas.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.saas.domain.bo.SaasOssConfigBo;
import com.ym.system.saas.domain.vo.SaasOssConfigVo;

import java.util.Collection;
import java.util.List;

/**
 * SaaS OSS 配置运营服务。
 */
public interface ISaasOssConfigService {
    PageResult<SaasOssConfigVo> queryPageList(SaasOssConfigBo bo, PageQuery pageQuery);

    List<SaasOssConfigVo> queryList();

    SaasOssConfigVo queryById(Long id);

    Long insertByBo(SaasOssConfigBo bo);

    void updateByBo(SaasOssConfigBo bo);

    void deleteWithValidByIds(Collection<Long> ids);
}
