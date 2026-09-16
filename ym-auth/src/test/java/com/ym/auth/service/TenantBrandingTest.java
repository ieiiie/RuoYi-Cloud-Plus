package com.ym.auth.service;

import com.ym.system.api.domain.vo.RemoteTenantUserVo;
import com.ym.system.api.model.LoginUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("dev")
class TenantBrandingTest {
    @Test
    void mapsCurrentTenantForLoginAndSwitch() {
        LoginUser user = new LoginUser();
        user.setTenantId("100001");
        user.setTenantName("农业租户");
        user.setTenantLogoUrl("/tenant-logo.png");
        var result = IAuthStrategy.toTenantVo(user);
        assertEquals("农业租户", result.getTenantName());
        assertEquals("/tenant-logo.png", result.getLogoUrl());
    }

    @Test
    void mapsTenantListUsedAfterPageRefresh() {
        RemoteTenantUserVo tenant = new RemoteTenantUserVo();
        tenant.setTenantId("100002");
        tenant.setLogoUrl("/factory.png");
        assertEquals("/factory.png", IAuthStrategy.toTenantVoList(List.of(tenant)).getFirst().getLogoUrl());
    }

    @Test
    void allowsExistingSessionsWithoutLogo() {
        assertNull(IAuthStrategy.toTenantVo(new LoginUser()).getLogoUrl());
    }
}
