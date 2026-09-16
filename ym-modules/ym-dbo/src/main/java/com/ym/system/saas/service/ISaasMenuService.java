package com.ym.system.saas.service;

import com.ym.system.saas.domain.bo.SaasMenuBo;
import com.ym.system.saas.domain.vo.SaasMenuVo;

import java.util.Collection;
import java.util.List;

/**
 * SaaS 租户菜单运营服务。
 */
public interface ISaasMenuService {
    List<SaasMenuVo> queryList(SaasMenuBo bo);

    SaasMenuVo queryById(Long menuId);

    Long insertByBo(SaasMenuBo bo);

    void updateByBo(SaasMenuBo bo);

    void deleteWithValidByIds(Collection<Long> menuIds);
}
