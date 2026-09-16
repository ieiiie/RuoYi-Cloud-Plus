package com.ym.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.auth.domain.vo.LoginVo;
import com.ym.auth.form.PasswordLoginBody;
import com.ym.auth.properties.CaptchaProperties;
import com.ym.auth.service.IAuthStrategy;
import com.ym.auth.service.SysLoginService;
import com.ym.auth.service.TenantLoginSessionService;
import com.ym.common.core.constant.Constants;
import com.ym.common.core.constant.GlobalConstants;
import com.ym.common.core.enums.LoginType;
import com.ym.common.core.exception.user.CaptchaException;
import com.ym.common.core.exception.user.CaptchaExpireException;
import com.ym.common.core.utils.MessageUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.core.utils.ValidatorUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.common.redis.utils.RedisUtils;
import com.ym.system.api.RemoteUserService;
import com.ym.system.api.domain.vo.RemoteClientVo;
import com.ym.system.api.model.LoginUser;
import org.springframework.stereotype.Service;

/**
 * 密码认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("password" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class PasswordAuthStrategy implements IAuthStrategy {

    private final CaptchaProperties captchaProperties;

    private final SysLoginService loginService;
    private final TenantLoginSessionService tenantLoginSessionService;

    @DubboReference
    private RemoteUserService remoteUserService;

    @Override
    public LoginVo login(String body, RemoteClientVo client) {
        PasswordLoginBody loginBody = JsonUtils.parseObject(body, PasswordLoginBody.class);
        ValidatorUtils.validate(loginBody);
        String username = loginBody.getUsername();
        String password = loginBody.getPassword();
        String code = loginBody.getCode();
        String uuid = loginBody.getUuid();
        // 验证码开关
        if (captchaProperties.getEnabled()) {
            validateCaptcha(username, code, uuid);
        }
        LoginUser loginUser = remoteUserService.getUserInfo(username);
        loginService.checkLogin(LoginType.PASSWORD, username, () -> !BCrypt.checkpw(password, loginUser.getPassword()));
        return tenantLoginSessionService.login(loginUser, client);
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    private void validateCaptcha(String username, String code, String uuid) {
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtils.blankToDefault(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            loginService.recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        if (!StringUtils.equalsIgnoreCase(code, captcha)) {
            loginService.recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"));
            throw new CaptchaException();
        }
    }

}
