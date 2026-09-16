package com.ym.system.resource.controller;


import cn.hutool.core.util.RandomUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ym.common.core.constant.Constants;
import com.ym.common.core.constant.GlobalConstants;
import com.ym.common.core.domain.R;
import com.ym.common.core.utils.SpringUtils;
import com.ym.common.core.utils.regex.RegexValidator;
import com.ym.common.redis.annotation.RateLimiter;
import com.ym.common.redis.utils.RedisUtils;
import com.ym.common.web.core.BaseController;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;

/**
 * 短信功能
 *
 * @author Lion Li
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sms")
public class SysSmsController extends BaseController {

    /**
     * 短信验证码
     *
     * @param phoneNumber 用户手机号
     */
    @GetMapping("/code")
    public R<Void> smsCaptcha(@NotBlank(message = "{user.phonenumber.not.blank}") String phoneNumber) {
        if (!RegexValidator.isMobile(phoneNumber)) {
            return R.fail("请输入正确的手机号！");
        }
        return SpringUtils.getAopProxy(this).smsCaptchaImpl(phoneNumber);
    }

    /**
     * 短信验证码发送实现。单独代理调用保证限流切面生效。
     */
    @RateLimiter(key = "#phoneNumber", time = 60, count = 1)
    public R<Void> smsCaptchaImpl(String phoneNumber) {
        String key = GlobalConstants.CAPTCHA_CODE_KEY + phoneNumber;
        String code = RandomUtil.randomNumbers(4);
        // 验证码模板id 自行处理 (查数据库或写死均可)
        String templateId = "";
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
        map.put("code", code);
        SmsBlend smsBlend = SmsFactory.getSmsBlend("tx1");
        SmsResponse smsResponse = smsBlend.sendMessage(phoneNumber, templateId, map);
        if (!smsResponse.isSuccess()) {
            log.error("验证码短信发送异常 => {}", smsResponse);
            Object data = smsResponse.getData();
            return R.fail(data == null ? "验证码短信发送失败" : data.toString());
        }
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        return R.ok();
    }

}
