package com.ym.system.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.domain.bo.SysTenantBo;
import com.ym.system.domain.vo.SysTenantVo;

import java.util.Collection;
import java.util.List;

/**
 * 租户服务。
 *
 * @author Lion Li
 */
public interface ISysTenantService {

    SysTenantVo queryById(Long id);

    SysTenantVo queryByTenantId(String tenantId);

    PageResult<SysTenantVo> queryPageList(SysTenantBo bo, PageQuery pageQuery);

    List<SysTenantVo> queryList(SysTenantBo bo);

    Boolean insertByBo(SysTenantBo bo);

    Boolean updateByBo(SysTenantBo bo);

    int updateTenantStatus(SysTenantBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验租户是否允许被平台管理员修改、停用或删除。
     *
     * @param tenantId 租户编号
     */
    void checkTenantAllowed(String tenantId);

    boolean checkCompanyNameUnique(SysTenantBo bo);

    /** 校验登录、注册和服务调用目标租户是否可用。 */
    void checkTenantAvailable(String tenantId);

    /** 校验指定租户是否还可创建用户。 */
    void checkAccountBalance(String tenantId);

    /** 按套餐更新租户管理员角色的菜单权限。 */
    Boolean syncTenantPackage(String tenantId, Long packageId);

}
