package com.ym.system.ownership.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Component;

/** Uses DBO's independent platform session; no business TenantHelper or default tenant. */
@Component
public class OwnershipActor {
    public void requirePermission(String action) {
        StpUtil.checkLogin();
        StpUtil.checkPermission("saas:iot-device-ownership:" + action);
    }
    public Long operatorId() {
        StpUtil.checkLogin();
        Long id = LoginHelper.getUserId();
        if (id == null) throw new ServiceException("平台登录态缺少操作人");
        return id;
    }
}
