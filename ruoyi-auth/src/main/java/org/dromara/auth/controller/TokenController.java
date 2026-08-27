package org.dromara.auth.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.auth.domain.vo.LoginVo;
import org.dromara.auth.domain.vo.TenantLoginVo;
import org.dromara.auth.form.RegisterBody;
import org.dromara.auth.form.SocialLoginBody;
import org.dromara.auth.service.IAuthStrategy;
import org.dromara.auth.service.SysLoginService;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.model.LoginBody;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.encrypt.annotation.ApiEncrypt;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.common.social.config.properties.SocialLoginConfigProperties;
import org.dromara.common.social.config.properties.SocialProperties;
import org.dromara.common.social.utils.SocialUtils;
import org.dromara.resource.api.RemoteMessageService;
import org.dromara.system.api.RemoteClientService;
import org.dromara.system.api.RemoteConfigService;
import org.dromara.system.api.RemoteSocialService;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.bo.RemoteSocialBo;
import org.dromara.system.api.domain.vo.RemoteClientVo;
import org.dromara.system.api.domain.vo.RemoteSocialVo;
import org.dromara.system.api.model.LoginUser;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * token 控制
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class TokenController {

    private final SocialProperties socialProperties;
    private final SysLoginService sysLoginService;
    private final ScheduledExecutorService scheduledExecutorService;

    @DubboReference
    private final RemoteConfigService remoteConfigService;
    @DubboReference
    private final RemoteClientService remoteClientService;
    @DubboReference
    private final RemoteSocialService remoteSocialService;
    @DubboReference
    private final RemoteUserService remoteUserService;
    @DubboReference(stub = "true")
    private final RemoteMessageService remoteMessageService;

    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
    @ApiEncrypt
    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody String body) {
        LoginBody loginBody = JsonUtils.parseObject(body, LoginBody.class);
        ValidatorUtils.validate(loginBody);
        // 授权类型和客户端id
        String clientId = loginBody.getClientId();
        String grantType = loginBody.getGrantType();
        RemoteClientVo clientVo = remoteClientService.queryByClientId(clientId);

        // 查询不到 client 或 client 内不包含 grantType
        if (ObjectUtil.isNull(clientVo) || !StringUtils.contains(clientVo.getGrantType(), grantType)) {
            log.info("客户端id: {} 认证类型：{} 异常!.", clientId, grantType);
            return R.fail(MessageUtils.message("auth.grant.type.error"));
        } else if (!SystemConstants.NORMAL.equals(clientVo.getStatus())) {
            return R.fail(MessageUtils.message("auth.grant.type.blocked"));
        }
        // 登录
        LoginVo loginVo = IAuthStrategy.login(body, clientVo, grantType);

        Long userId = LoginHelper.getUserId();
        String tenantId = LoginHelper.getTenantId();
        scheduledExecutorService.schedule(() -> {
            TenantHelper.dynamic(tenantId, () -> remoteMessageService.publishMessage(
                List.of(userId), DateUtils.getTodayHour(new Date()) + "好，欢迎登录 RuoYi-Cloud-Plus 后台管理系统"));
        }, 5, TimeUnit.SECONDS);
        return R.ok(loginVo);
    }

    /**
     * 查询当前全局账号可进入的有效租户。
     *
     * <p>网关转发时会去掉 {@code /auth} 前缀，因此外部地址为
     * {@code GET /auth/tenant/list}。</p>
     */
    @SaCheckLogin
    @GetMapping("/tenant/list")
    public R<List<TenantLoginVo>> tenantList() {
        return R.ok(IAuthStrategy.toTenantVoList(
            remoteUserService.listTenantUsers(LoginHelper.getGlobalUserId())));
    }

    /**
     * 在不换发 token 的情况下切换当前浏览器会话的租户成员身份。
     */
    @SaCheckLogin
    @PutMapping("/tenant/{tenantId}")
    public R<TenantLoginVo> switchTenant(@PathVariable String tenantId) {
        LoginUser loginUser = remoteUserService.getUserInfoByGlobalUserId(
            LoginHelper.getGlobalUserId(), tenantId);
        // 平台管理员的临时数据视图不能覆盖用户主动选择的成员租户。
        TenantHelper.clearDynamic();
        LoginHelper.updateLoginUser(loginUser);
        return R.ok(IAuthStrategy.toTenantVo(loginUser));
    }

    /**
     * 第三方登录请求
     *
     * @param source 登录来源
     * @return 结果
     */
    @GetMapping("/binding/{source}")
    public R<String> authBinding(@PathVariable("source") String source,
                                 @RequestParam String domain) {
        SocialLoginConfigProperties obj = socialProperties.getType().get(source);
        if (ObjectUtil.isNull(obj)) {
            return R.fail(source + "平台账号暂不支持");
        }
        AuthRequest authRequest = SocialUtils.getAuthRequest(source, socialProperties);
        Map<String, String> map = new HashMap<>();
        map.put("domain", domain);
        map.put("state", AuthStateUtils.createState());
        String authorizeUrl = authRequest.authorize(Base64.encode(JsonUtils.toJsonString(map), StandardCharsets.UTF_8));
        return R.data(authorizeUrl);
    }

    /**
     * 第三方登录回调业务处理 绑定授权
     *
     * @param loginBody 请求体
     * @return 结果
     */
    @PostMapping("/social/callback")
    public R<Void> socialCallback(@RequestBody SocialLoginBody loginBody) {
        // 获取第三方登录信息
        AuthResponse<AuthUser> response = SocialUtils.loginAuth(
            loginBody.getSource(), loginBody.getSocialCode(),
            loginBody.getSocialState(), socialProperties);
        AuthUser authUserData = response.getData();
        // 判断授权响应是否成功
        if (!response.ok()) {
            return R.fail(response.getMsg());
        }
        sysLoginService.socialRegister(authUserData);
        return R.ok();
    }


    /**
     * 取消授权
     *
     * @param socialId socialId
     */
    @DeleteMapping(value = "/unlock/{socialId}")
    public R<Void> unlockSocial(@PathVariable Long socialId) {
        RemoteSocialBo query = new RemoteSocialBo();
        query.setGlobalUserId(LoginHelper.getGlobalUserId());
        boolean belongsToCurrentAccount = remoteSocialService.queryList(query).stream()
            .map(RemoteSocialVo::getId)
            .anyMatch(socialId::equals);
        if (!belongsToCurrentAccount) {
            return R.fail("无权取消该第三方授权");
        }
        Boolean rows = remoteSocialService.deleteWithValidById(socialId);
        return rows ? R.ok() : R.fail("取消授权失败");
    }

    /**
     * 登出方法
     */
    @PostMapping("logout")
    public R<Void> logout() {
        sysLoginService.logout();
        return R.ok();
    }

    /**
     * 用户注册
     */
    @ApiEncrypt
    @PostMapping("register")
    public R<Void> register(@RequestBody RegisterBody registerBody) {
        String tenantId = registerBody.getTenantId();
        TenantHelper.checkTenantId(tenantId);
        if (!remoteConfigService.selectRegisterEnabled(tenantId)) {
            return R.fail("当前系统没有开启注册功能！");
        }
        // 用户注册
        TenantHelper.dynamic(tenantId, () -> sysLoginService.register(registerBody));
        return R.ok();
    }

}
