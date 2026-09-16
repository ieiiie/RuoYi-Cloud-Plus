package com.ym.system.saas.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.saas.domain.bo.SaasAppBo;
import com.ym.system.saas.domain.vo.SaasAppVo;

import java.util.Collection;

/**
 * SaaS 应用运营服务。
 */
public interface ISaasAppService {
    PageResult<SaasAppVo> queryPageList(SaasAppBo bo, PageQuery pageQuery);

    SaasAppVo queryById(Long appId);

    Long insertByBo(SaasAppBo bo);

    void updateByBo(SaasAppBo bo);

    /** 仅保存组合菜单关系，不修改应用基本信息和启用状态。 */
    void updateMenus(Long appId, Collection<Long> menuIds);

    void deleteWithValidByIds(Collection<Long> appIds);
}
