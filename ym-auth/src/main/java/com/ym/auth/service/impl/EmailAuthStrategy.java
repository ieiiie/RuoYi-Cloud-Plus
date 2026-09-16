package com.ym.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.auth.domain.vo.LoginVo;
import com.ym.auth.form.EmailLoginBody;
import com.ym.auth.service.IAuthStrategy;
import com.ym.auth.service.SysLoginService;
import com.ym.auth.service.TenantLoginSessionService;
import com.ym.common.core.constant.Constants;
import com.ym.common.core.constant.GlobalConstants;
import com.ym.common.core.enums.LoginType;
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
 * 邮件认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("email" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class EmailAuthStrategy implements IAuthStrategy {

    private final SysLoginService loginService;
    private final TenantLoginSessionService tenantLoginSessionService;

    @DubboReference
    private RemoteUserService remoteUserService;

    @Override
    public LoginVo login(String body, RemoteClientVo client) {
        EmailLoginBody loginBody = JsonUtils.parseObject(body, EmailLoginBody.class);
        ValidatorUtils.validate(loginBody);
        String email = loginBody.getEmail();
        String emailCode = loginBody.getEmailCode();
        LoginUser loginUser = remoteUserService.getUserInfoByEmail(email);
        loginService.checkLogin(LoginType.EMAIL, loginUser.getUsername(), () -> !validateEmailCode(email, emailCode));
        return tenantLoginSessionService.login(loginUser, client);
    }

    /**
     * 校验邮箱验证码
     */
    private boolean validateEmailCode(String email, String emailCode) {
        String code = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(code)) {
            loginService.recordLoginInfo(email, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(emailCode);
    }

}
