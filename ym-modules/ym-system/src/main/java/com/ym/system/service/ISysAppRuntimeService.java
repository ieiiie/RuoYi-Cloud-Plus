package com.ym.system.service;

import com.ym.system.domain.vo.AuthorizedAppVo;

import java.util.List;

/** 应用运行时授权服务。 */
public interface ISysAppRuntimeService {
    /** 查询已启用应用的最小公开登录配置。 */
    com.ym.system.domain.vo.PublicAppLoginVo selectPublicLogin(String appKey);
    /** 查询当前租户、套餐和用户共同授权的业务应用。 */
    List<AuthorizedAppVo> selectAuthorizedApps(Long userId);
}
