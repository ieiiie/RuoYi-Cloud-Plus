package com.ym.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWechatMiniProgramRequest;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.auth.domain.vo.LoginVo;
import com.ym.auth.form.XcxLoginBody;
import com.ym.auth.properties.WeixinMiniappProperties;
import com.ym.auth.service.IAuthStrategy;
import com.ym.auth.service.TenantLoginSessionService;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.ValidatorUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.system.api.RemoteUserService;
import com.ym.system.api.domain.vo.RemoteClientVo;
import com.ym.system.api.model.XcxLoginUser;
import org.springframework.stereotype.Service;

/**
 * 邮件认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("xcx" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class XcxAuthStrategy implements IAuthStrategy {

    private final TenantLoginSessionService tenantLoginSessionService;
    private final WeixinMiniappProperties weixinMiniappProperties;

    @DubboReference
    private RemoteUserService remoteUserService;

    @Override
    public LoginVo login(String body, RemoteClientVo client) {
        XcxLoginBody loginBody = JsonUtils.parseObject(body, XcxLoginBody.class);
        ValidatorUtils.validate(loginBody);
        // xcxCode 为 小程序调用 wx.login 授权后获取
        String xcxCode = loginBody.getXcxCode();
        // 多个小程序识别使用
        String appid = loginBody.getAppid();

        // 校验 appid + appsrcret + xcxCode 调用登录凭证校验接口 获取 session_key 与 openid
        String appSecret;
        try {
            appSecret = weixinMiniappProperties.requireSecret(appid);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new ServiceException(ex.getMessage());
        }
        AuthRequest authRequest = new AuthWechatMiniProgramRequest(AuthConfig.builder()
            .clientId(appid).clientSecret(appSecret)
            .ignoreCheckRedirectUri(true).ignoreCheckState(true).build());
        AuthCallback authCallback = new AuthCallback();
        authCallback.setCode(xcxCode);
        AuthResponse<AuthUser> resp = authRequest.login(authCallback);
        String openid, unionId;
        if (resp.ok()) {
            AuthToken token = resp.getData().getToken();
            openid = token.getOpenId();
            // 微信小程序只有关联到微信开放平台下之后才能获取到 unionId，因此unionId不一定能返回。
            unionId = token.getUnionId();
        } else {
            throw new ServiceException(resp.getMsg());
        }
        // 第三方平台联调仍由具体小程序配置完成；账号定位已统一到全局绑定表。
        XcxLoginUser loginUser = remoteUserService.getUserInfoByOpenid(openid);
        LoginVo loginVo = tenantLoginSessionService.login(loginUser, client);
        loginVo.setOpenid(openid);
        return loginVo;
    }

}
