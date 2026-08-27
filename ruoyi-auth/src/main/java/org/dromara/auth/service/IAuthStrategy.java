package org.dromara.auth.service;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.ObjectUtil;
import org.dromara.auth.domain.vo.LoginVo;
import org.dromara.auth.domain.vo.TenantLoginVo;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.domain.vo.RemoteClientVo;
import org.dromara.system.api.domain.vo.RemoteTenantUserVo;
import org.dromara.system.api.model.LoginUser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 授权策略
 *
 * @author Michelle.Chung
 */
public interface IAuthStrategy {

    String BASE_NAME = "AuthStrategy";

    /**
     * 登录
     *
     * @param body      登录对象
     * @param client    授权管理视图对象
     * @param grantType 授权类型
     * @return 登录验证信息
     */
    static LoginVo login(String body, RemoteClientVo client, String grantType) {
        // 授权类型和客户端id
        String beanName = grantType + BASE_NAME;
        if (!SpringUtils.containsBean(beanName)) {
            throw new ServiceException("授权类型不正确!");
        }
        IAuthStrategy instance = SpringUtils.getBean(beanName);
        return instance.login(body, client);
    }

    /**
     * 按客户端配置构建统一登录参数。
     */
    static SaLoginParameter buildLoginParameter(RemoteClientVo client) {
        return buildLoginParameter(client, null);
    }

    /**
     * 按客户端配置构建统一登录参数，并预留自定义扩展入口。
     */
    static SaLoginParameter buildLoginParameter(RemoteClientVo client, Consumer<SaLoginParameter> customizer) {
        SaLoginParameter model = new SaLoginParameter();
        model.setDeviceType(client.getDeviceType());
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        model.setExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY, client.getAccessPath());
        model.setExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY, client.getIpWhitelist());
        if (ObjectUtil.isNotNull(customizer)) {
            customizer.accept(model);
        }
        return model;
    }

    /**
     * 构建登录响应，并返回当前 token 对应的租户及可切换租户列表。
     */
    static LoginVo buildLoginVo(LoginUser loginUser, RemoteClientVo client, List<RemoteTenantUserVo> tenants) {
        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(cn.dev33.satoken.stp.StpUtil.getTokenValue());
        loginVo.setExpireIn(cn.dev33.satoken.stp.StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        loginVo.setCurrentTenant(toTenantVo(loginUser));
        loginVo.setTenants(toTenantVoList(tenants));
        return loginVo;
    }

    /** 将 token 中的当前租户成员转换为接口响应对象。 */
    static TenantLoginVo toTenantVo(LoginUser loginUser) {
        TenantLoginVo tenant = new TenantLoginVo();
        tenant.setTenantId(loginUser.getTenantId());
        tenant.setTenantName(loginUser.getTenantName());
        tenant.setUserId(loginUser.getUserId());
        return tenant;
    }

    /** 将远程租户成员列表转换为认证服务的接口对象。 */
    static List<TenantLoginVo> toTenantVoList(List<RemoteTenantUserVo> tenants) {
        if (tenants == null || tenants.isEmpty()) {
            return List.of();
        }
        List<TenantLoginVo> result = new ArrayList<>(tenants.size());
        for (RemoteTenantUserVo item : tenants) {
            TenantLoginVo tenant = new TenantLoginVo();
            tenant.setTenantId(item.getTenantId());
            tenant.setTenantName(item.getTenantName());
            tenant.setUserId(item.getUserId());
            result.add(tenant);
        }
        return result;
    }

    /**
     * 登录
     *
     * @param body   登录对象
     * @param client 授权管理视图对象
     * @return 登录验证信息
     */
    LoginVo login(String body, RemoteClientVo client);

}
