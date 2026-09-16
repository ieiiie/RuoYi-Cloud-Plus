package com.ym.iot.ownership.support;

import com.ym.common.core.exception.ServiceException;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;

import org.springframework.stereotype.Component;

/** Never derives the acting tenant from a DTO, device profile, binding or fallback constant. */
@Component
public class OwnershipActor {
    public String tenantId() {
        if (!LoginHelper.isLogin()) throw new ServiceException("请先登录");
        String tenantId = TenantHelper.getTenantId();
        if (tenantId == null || tenantId.isBlank()) throw new ServiceException("当前登录态缺少租户信息");
        return tenantId;
    }
    public Long operatorId() {
        tenantId();
        Long id = LoginHelper.getUserId();
        if (id == null) throw new ServiceException("当前登录态缺少操作人");
        return id;
    }
}
