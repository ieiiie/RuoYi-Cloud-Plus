package org.dromara.system.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.system.domain.bo.SysTenantPackageBo;
import org.dromara.system.domain.vo.SysTenantPackageVo;

import java.util.Collection;
import java.util.List;

/**
 * 租户套餐服务。
 *
 * @author Lion Li
 */
public interface ISysTenantPackageService {

    SysTenantPackageVo queryById(Long packageId);

    PageResult<SysTenantPackageVo> queryPageList(SysTenantPackageBo bo, PageQuery pageQuery);

    List<SysTenantPackageVo> queryList(SysTenantPackageBo bo);

    List<SysTenantPackageVo> selectEnabledList();

    Boolean insertByBo(SysTenantPackageBo bo);

    Boolean updateByBo(SysTenantPackageBo bo);

    int updatePackageStatus(SysTenantPackageBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    boolean checkPackageNameUnique(SysTenantPackageBo bo);

}
