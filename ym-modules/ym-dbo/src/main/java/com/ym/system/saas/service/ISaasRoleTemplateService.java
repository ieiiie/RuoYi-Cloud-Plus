package com.ym.system.saas.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.saas.domain.bo.SaasRoleTemplateBo;
import com.ym.system.saas.domain.vo.SaasRoleTemplateVo;

import java.util.Collection;

/**
 * SaaS 角色模板运营服务。
 */
public interface ISaasRoleTemplateService {
    PageResult<SaasRoleTemplateVo> queryPageList(SaasRoleTemplateBo bo, PageQuery pageQuery);

    SaasRoleTemplateVo queryById(Long id);

    Long insertByBo(SaasRoleTemplateBo bo);

    void updateByBo(SaasRoleTemplateBo bo);

    void deleteWithValidByIds(Collection<Long> ids);

    void sync(Long templateId, Collection<String> tenantIds);
}
