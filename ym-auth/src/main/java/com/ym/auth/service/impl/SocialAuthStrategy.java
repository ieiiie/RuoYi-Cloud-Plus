package com.ym.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.auth.domain.vo.LoginVo;
import com.ym.auth.form.SocialLoginBody;
import com.ym.auth.service.IAuthStrategy;
import com.ym.auth.service.TenantLoginSessionService;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.ValidatorUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.common.social.config.properties.SocialProperties;
import com.ym.common.social.utils.SocialUtils;
import com.ym.system.api.RemoteSocialService;
import com.ym.system.api.RemoteUserService;
import com.ym.system.api.domain.vo.RemoteClientVo;
import com.ym.system.api.domain.vo.RemoteSocialVo;
import com.ym.system.api.model.LoginUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 第三方授权策略
 *
 * @author thiszhc is 三三
 */
@Slf4j
@Service("social" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class SocialAuthStrategy implements IAuthStrategy {

    private final SocialProperties socialProperties;
    private final TenantLoginSessionService tenantLoginSessionService;

    @DubboReference
    private RemoteSocialService remoteSocialService;
    @DubboReference
    private RemoteUserService remoteUserService;

    /**
     * 登录-第三方授权登录
     *
     * @param body   登录信息
     * @param client 客户端信息
     */
    @Override
    public LoginVo login(String body, RemoteClientVo client) {
        SocialLoginBody loginBody = JsonUtils.parseObject(body, SocialLoginBody.class);
        ValidatorUtils.validate(loginBody);
        AuthResponse<AuthUser> response = SocialUtils.loginAuth(
            loginBody.getSource(), loginBody.getSocialCode(),
            loginBody.getSocialState(), socialProperties);
        if (!response.ok()) {
            throw new ServiceException(response.getMsg());
        }
        AuthUser authUserData = response.getData();
        List<RemoteSocialVo> list = remoteSocialService.selectByAuthId(
            authUserData.getSource() + authUserData.getUuid());
        if (CollUtil.isEmpty(list)) {
            throw new ServiceException("你还没有绑定第三方账号，绑定后才可以登录！");
        }
        RemoteSocialVo socialVo = list.getFirst();

        LoginUser loginUser = remoteUserService.getUserInfoByGlobalUserId(socialVo.getGlobalUserId());
        return tenantLoginSessionService.login(loginUser, client);
    }

}
