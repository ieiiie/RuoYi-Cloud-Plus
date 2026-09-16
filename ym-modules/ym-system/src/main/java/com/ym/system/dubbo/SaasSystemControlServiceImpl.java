package com.ym.system.dubbo;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.redis.utils.CacheUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.api.SaasSystemControlService;
import com.ym.system.api.model.LoginUser;
import com.ym.system.service.ISysDictTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/** SaaS 系统服务的幂等缓存和会话控制实现。 */
@Service
@DubboService
@RequiredArgsConstructor
public class SaasSystemControlServiceImpl implements SaasSystemControlService {

    private final ISysDictTypeService dictTypeService;

    @Override
    public void refreshGlobalDict() {
        dictTypeService.resetDictCache();
    }

    @Override
    public void refreshTenantConfig(String tenantId) {
        if (StringUtils.isBlank(tenantId) || "*".equals(tenantId)) {
            CacheUtils.clear(CacheNames.SYS_CONFIG);
            return;
        }
        TenantHelper.dynamic(tenantId, () -> CacheUtils.clear(CacheNames.SYS_CONFIG));
    }

    @Override
    public void invalidateTenantSessions(String tenantId) {
        List<String> tokenKeys = StpUtil.searchTokenValue("", 0, -1, false);
        if (CollUtil.isEmpty(tokenKeys)) {
            return;
        }
        for (String tokenKey : tokenKeys) {
            String token = StringUtils.substringAfterLast(tokenKey, StringUtils.COLON);
            LoginUser loginUser = LoginHelper.getLoginUser(token);
            if (ObjectUtil.isNotNull(loginUser)
                && ("*".equals(tenantId) || StringUtils.equals(tenantId, loginUser.getTenantId()))) {
                StpUtil.logoutByTokenValue(token);
            }
        }
    }
}
