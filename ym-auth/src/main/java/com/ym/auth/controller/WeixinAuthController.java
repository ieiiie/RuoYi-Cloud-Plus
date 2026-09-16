package com.ym.auth.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.ObjectUtil;
import com.ym.agriculture.api.farmtask.RemoteEmployeeAdmissionService;
import com.ym.agriculture.api.farmtask.domain.vo.RemoteEmployeeAdmissionVo;
import com.ym.auth.domain.vo.LoginVo;
import com.ym.auth.form.WeixinLoginBody;
import com.ym.auth.service.IAuthStrategy;
import com.ym.auth.service.SysLoginService;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.R;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.system.api.RemoteClientService;
import com.ym.system.api.domain.vo.RemoteClientVo;
import com.ym.system.api.model.LoginUser;
import com.ym.system.api.model.XcxLoginUser;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 微信登录历史路径适配，统一进入全局账号与租户会话。 */
@RestController
@RequiredArgsConstructor
public class WeixinAuthController {

    private final SysLoginService sysLoginService;
    @DubboReference
    private final RemoteClientService remoteClientService;
    @DubboReference
    private final RemoteEmployeeAdmissionService employeeAdmissionService;

    @SaIgnore
    @PostMapping("/mp-weixin/login")
    public R<LoginVo> miniProgramLogin(@RequestBody WeixinLoginBody body) {
        return R.ok(login(body));
    }

    @SaIgnore
    @PostMapping("/employee-wx/login")
    public R<LoginVo> employeeLogin(@RequestBody WeixinLoginBody body) {
        LoginVo login = login(body);
        RemoteEmployeeAdmissionVo admission = employeeAdmissionService.checkByOpenid(login.getOpenid());
        if (!admission.isAllowed()) {
            sysLoginService.logout();
            throw new ServiceException(admission.getMessage());
        }
        return R.ok(login);
    }

    @SaCheckLogin
    @GetMapping("/employee-wx/getInfo")
    public R<RemoteEmployeeAdmissionVo> employeeInfo() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (!(loginUser instanceof XcxLoginUser xcxLoginUser) || StringUtils.isBlank(xcxLoginUser.getOpenid())) {
            throw new ServiceException("当前会话不是微信小程序会话");
        }
        return R.ok(employeeAdmissionService.checkByOpenid(xcxLoginUser.getOpenid()));
    }

    private LoginVo login(WeixinLoginBody body) {
        if (body == null || StringUtils.isBlank(body.getClientId())) {
            throw new ServiceException("clientId 不能为空");
        }
        String code = StringUtils.blankToDefault(body.getXcxCode(), body.getCode());
        if (StringUtils.isBlank(code)) {
            throw new ServiceException("微信 code 不能为空");
        }
        RemoteClientVo client = remoteClientService.queryByClientId(body.getClientId());
        if (ObjectUtil.isNull(client) || !StringUtils.contains(client.getGrantType(), "xcx")) {
            throw new ServiceException("认证权限类型错误");
        }
        if (!SystemConstants.NORMAL.equals(client.getStatus())) {
            throw new ServiceException("认证权限类型已禁用");
        }
        Map<String, Object> xcx = new LinkedHashMap<>();
        xcx.put("clientId", body.getClientId());
        xcx.put("grantType", "xcx");
        xcx.put("appid", body.getAppid());
        xcx.put("xcxCode", code);
        return IAuthStrategy.login(JsonUtils.toJsonString(xcx), client, "xcx");
    }
}
