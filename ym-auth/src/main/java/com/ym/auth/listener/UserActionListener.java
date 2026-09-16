package com.ym.auth.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.extern.slf4j.Slf4j;
import com.ym.auth.event.UserLoginSuccessEvent;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.utils.SpringUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.redis.utils.RedisUtils;
import org.springframework.stereotype.Component;

/**
 * 用户行为 侦听器的实现
 *
 * @author Lion Li
 */
@Component
@Slf4j
public class UserActionListener implements SaTokenListener {

    /**
     * 每次登录时触发
     */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginParameter loginParameter) {
        SpringUtils.context().publishEvent(new UserLoginSuccessEvent(loginId, tokenValue, loginParameter));
    }

    /**
     * 每次注销时触发
     */
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        RedisUtils.deleteObject(CacheNames.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doLogout, useId:{}, token:***{}", loginId, StringUtils.right(tokenValue, 8));
    }

    /**
     * 每次被踢下线时触发
     */
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        RedisUtils.deleteObject(CacheNames.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doLogoutByLoginId, useId:{}, token:***{}", loginId, StringUtils.right(tokenValue, 8));
    }

    /**
     * 每次被顶下线时触发
     */
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        RedisUtils.deleteObject(CacheNames.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doReplaced, useId:{}, token:***{}", loginId, StringUtils.right(tokenValue, 8));
    }

    /**
     * 每次被封禁时触发
     */
    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
    }

    /**
     * 每次被解封时触发
     */
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
    }

    /**
     * 每次打开二级认证时触发
     */
    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCreateSession(String id) {
    }

    /**
     * 每次注销Session时触发
     */
    @Override
    public void doLogoutSession(String id) {
    }

    /**
     * 每次Token续期时触发
     */
    @Override
    public void doRenewTimeout(String loginType, Object loginId, String tokenValue, long timeout) {
    }

}
