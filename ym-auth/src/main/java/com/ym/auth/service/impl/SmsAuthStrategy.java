package com.ym.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.auth.domain.vo.LoginVo;
import com.ym.auth.form.SmsLoginBody;
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
 * 短信认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("sms" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class SmsAuthStrategy implements IAuthStrategy {

    private final SysLoginService loginService;
    private final TenantLoginSessionService tenantLoginSessionService;

    @DubboReference
    private RemoteUserService remoteUserService;

    @Override
    public LoginVo login(String body, RemoteClientVo client) {
        SmsLoginBody loginBody = JsonUtils.parseObject(body, SmsLoginBody.class);
        ValidatorUtils.validate(loginBody);
        String phoneNumber = loginBody.getPhoneNumber();
        String smsCode = loginBody.getSmsCode();
        LoginUser loginUser = remoteUserService.getUserInfoByPhoneNumber(phoneNumber);
        loginService.checkLogin(LoginType.SMS, loginUser.getUsername(), () -> !validateSmsCode(phoneNumber, smsCode));
        return tenantLoginSessionService.login(loginUser, client);
    }

    /**
     * 校验短信验证码
     */
    private boolean validateSmsCode(String phoneNumber, String smsCode) {
        String code = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + phoneNumber);
        if (StringUtils.isBlank(code)) {
            loginService.recordLoginInfo(phoneNumber, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(smsCode);
    }

}
