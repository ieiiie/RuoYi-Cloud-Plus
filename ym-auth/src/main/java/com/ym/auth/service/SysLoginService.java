package com.ym.auth.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.lock.annotation.Lock4j;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthUser;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.auth.form.RegisterBody;
import com.ym.auth.properties.CaptchaProperties;
import com.ym.auth.properties.UserPasswordProperties;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.constant.Constants;
import com.ym.common.core.constant.GlobalConstants;
import com.ym.common.core.enums.LoginType;
import com.ym.common.core.enums.UserType;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.exception.user.CaptchaException;
import com.ym.common.core.exception.user.CaptchaExpireException;
import com.ym.common.core.exception.user.UserException;
import com.ym.common.core.utils.MessageUtils;
import com.ym.common.core.utils.SpringUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.log.event.LoginInfoEvent;
import com.ym.common.redis.utils.RedisUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.api.RemoteSocialService;
import com.ym.system.api.RemoteUserService;
import com.ym.system.api.domain.bo.RemoteSocialBo;
import com.ym.system.api.domain.bo.RemoteUserBo;
import com.ym.system.api.domain.vo.RemoteSocialVo;
import com.ym.system.api.model.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysLoginService {

    @DubboReference
    private RemoteUserService remoteUserService;
    @DubboReference
    private RemoteSocialService remoteSocialService;

    @Autowired
    private UserPasswordProperties userPasswordProperties;
    @Autowired
    private final CaptchaProperties captchaProperties;

    /**
     * 绑定第三方用户
     *
     * @param authUserData 授权响应实体
     */
    @Lock4j
    public void socialRegister(AuthUser authUserData) {
        String authId = authUserData.getSource() + authUserData.getUuid();
        // 第三方用户信息
        RemoteSocialBo bo = BeanUtil.toBean(authUserData, RemoteSocialBo.class);
        BeanUtil.copyProperties(authUserData.getToken(), bo);
        Long globalUserId = LoginHelper.getGlobalUserId();
        if (ObjectUtil.isNull(globalUserId)) {
            throw new ServiceException("当前租户成员未关联全局账号，不能绑定第三方账号");
        }
        bo.setGlobalUserId(globalUserId);
        bo.setAuthId(authId);
        bo.setOpenId(authUserData.getUuid());
        bo.setUserName(authUserData.getUsername());
        bo.setNickName(authUserData.getNickname());
        List<RemoteSocialVo> checkList = remoteSocialService.selectByAuthId(authId);
        if (CollUtil.isNotEmpty(checkList)) {
            throw new ServiceException("此三方账号已经被绑定!");
        }
        // 查询是否已经绑定用户
        RemoteSocialBo params = new RemoteSocialBo();
        params.setGlobalUserId(globalUserId);
        params.setSource(bo.getSource());
        List<RemoteSocialVo> list = remoteSocialService.queryList(params);
        if (CollUtil.isEmpty(list)) {
            // 没有绑定用户, 新增用户信息
            remoteSocialService.insertByBo(bo);
        } else {
            // 更新用户信息
            bo.setId(list.getFirst().getId());
            remoteSocialService.updateByBo(bo);
            // 如果要绑定的平台账号已经被绑定过了 是否抛异常自行决断
            // throw new ServiceException("此平台账号已经被绑定!");
        }
    }

    /**
     * 退出登录
     */
    public void logout() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (ObjectUtil.isNull(loginUser)) {
                return;
            }
            recordLoginInfo(loginUser.getUsername(), Constants.LOGOUT, MessageUtils.message("user.logout.success"));
        } catch (NotLoginException ignored) {
        } finally {
            try {
                StpUtil.logout();
            } catch (NotLoginException ignored) {
            }
        }
    }

    /**
     * 注册
     */
    public void register(RegisterBody registerBody) {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        // 校验用户类型是否存在
        String userType = UserType.getUserType(registerBody.getUserType()).getUserType();

        boolean captchaEnabled = captchaProperties.getEnabled();
        // 验证码开关
        if (captchaEnabled) {
            validateCaptcha(username, registerBody.getCode(), registerBody.getUuid());
        }

        // 注册用户信息
        RemoteUserBo remoteUserBo = new RemoteUserBo();
        remoteUserBo.setUserName(username);
        remoteUserBo.setNickName(username);
        remoteUserBo.setPassword(BCrypt.hashpw(password));
        remoteUserBo.setUserType(userType);
        remoteUserBo.setTenantId(registerBody.getTenantId());

        boolean regFlag = remoteUserService.registerUserInfo(remoteUserBo);
        if (!regFlag) {
            throw new UserException("user.register.error");
        }
        recordLoginInfo(username, Constants.REGISTER, MessageUtils.message("user.register.success"));
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    public void validateCaptcha(String username, String code, String uuid) {
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtils.blankToDefault(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        if (!StringUtils.equalsIgnoreCase(code, captcha)) {
            recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"));
            throw new CaptchaException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     * @return
     */
    public void recordLoginInfo(String username, String status, String message) {
        // 封装对象
        LoginInfoEvent loginInfoEvent = new LoginInfoEvent();
        loginInfoEvent.setUsername(username);
        loginInfoEvent.setStatus(status);
        loginInfoEvent.setMessage(message);
        loginInfoEvent.setTenantId(TenantHelper.getTenantId());
        SpringUtils.context().publishEvent(loginInfoEvent);
    }

    /**
     * 登录校验
     */
    public void checkLogin(LoginType loginType, String username, Supplier<Boolean> supplier) {
        String errorKey = CacheNames.PWD_ERR_CNT_KEY + username;
        String loginFail = Constants.LOGIN_FAIL;
        Integer maxRetryCount = userPasswordProperties.getMaxRetryCount();
        Integer lockTime = userPasswordProperties.getLockTime();

        // 获取用户登录错误次数，默认为0 (可自定义限制策略 例如: key + username + ip)
        int errorNumber = ObjectUtil.defaultIfNull(RedisUtils.getCacheObject(errorKey), 0);
        // 锁定时间内登录 则踢出
        if (errorNumber >= maxRetryCount) {
            recordLoginInfo(username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
            throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
        }

        if (supplier.get()) {
            // 错误次数递增
            errorNumber++;
            RedisUtils.setCacheObject(errorKey, errorNumber, Duration.ofMinutes(lockTime));
            // 达到规定错误次数 则锁定登录
            if (errorNumber >= maxRetryCount) {
                recordLoginInfo(username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
                throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
            } else {
                // 未达到规定错误次数
                recordLoginInfo(username, loginFail, MessageUtils.message(loginType.getRetryLimitCount(), errorNumber));
                throw new UserException(loginType.getRetryLimitCount(), errorNumber);
            }
        }

        // 登录成功 清空错误次数
        RedisUtils.deleteObject(errorKey);
    }
}
