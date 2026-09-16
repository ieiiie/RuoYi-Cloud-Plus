package com.ym.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.parameter.enums.SaLogoutMode;
import com.baomidou.lock.annotation.Lock4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.auth.domain.vo.LoginVo;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.api.RemoteConfigService;
import com.ym.system.api.RemoteUserService;
import com.ym.system.api.domain.vo.RemoteClientVo;
import com.ym.system.api.model.LoginUser;
import org.springframework.stereotype.Service;

/**
 * 租户登录会话与同类终端并发控制。
 */
@Slf4j
@Service
public class TenantLoginSessionService {

    private static final String MAX_CONCURRENT_LOGIN_COUNT_KEY = "sys.account.maxConcurrentLoginCount";

    @DubboReference
    private RemoteConfigService remoteConfigService;
    @DubboReference
    private RemoteUserService remoteUserService;

    /**
     * 创建租户登录会话并按设备类型收敛在线数量。
     *
     * @param loginUser 租户本地用户
     * @param client 客户端配置
     * @return 登录响应
     */
    @Lock4j(name = "tenant-login-session", keys = {"#loginUser.globalUserId", "#client.deviceType"})
    public LoginVo login(LoginUser loginUser, RemoteClientVo client) {
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        SaLoginParameter model = IAuthStrategy.buildLoginParameter(client);
        LoginHelper.login(loginUser, model);
        enforceLimit(loginUser);
        return IAuthStrategy.buildLoginVo(loginUser, client,
            remoteUserService.listTenantUsers(loginUser.getGlobalUserId()));
    }

    /**
     * 将当前 token 切换到目标租户成员并执行目标租户并发限制。
     *
     * @param loginUser 目标租户本地用户
     * @param deviceType 当前 token 的设备类型
     */
    @Lock4j(name = "tenant-login-session", keys = {"#loginUser.globalUserId", "#deviceType"})
    public void switchTenant(LoginUser loginUser, String deviceType) {
        LoginHelper.withTokenSessionLock(() -> {
            // clearDynamic 会写入整份 Token-Session，必须与权限刷新共同串行化。
            TenantHelper.clearDynamic();
            loginUser.setDeviceType(deviceType);
            LoginHelper.updateLoginUser(loginUser);
            enforceLimit(loginUser);
            return null;
        });
    }

    private void enforceLimit(LoginUser loginUser) {
        int limit = queryLimit(loginUser.getTenantId());
        if (limit < 1) {
            return;
        }
        StpUtil.stpLogic.logoutByMaxLoginCount(
            loginUser.getLoginId(), null, loginUser.getDeviceType(), limit, SaLogoutMode.REPLACED);
    }

    private int queryLimit(String tenantId) {
        try {
            Integer limit = remoteConfigService.getConfigInt(tenantId, MAX_CONCURRENT_LOGIN_COUNT_KEY);
            if (limit != null && (limit == -1 || limit > 0)) {
                return limit;
            }
            log.warn("租户并发登录数量配置无效，按不限处理: tenantId={}, value={}", tenantId, limit);
        } catch (Exception ex) {
            log.warn("读取租户并发登录数量失败，按不限处理: tenantId={}", tenantId, ex);
        }
        return -1;
    }
}
