package com.ym.auth.service;

import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.api.RemoteConfigService;
import com.ym.system.api.model.LoginUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Checks that clearing the whole session and enforcing the limit stay inside the switch lock. */
@Tag("dev")
class TenantLoginSessionServiceTest {
    @Test
    void switchClearsDynamicAndUpdatesMemberWithinSameTokenLock() throws Exception {
        List<String> events = new ArrayList<>();
        AtomicBoolean locked = new AtomicBoolean();
        LoginUser target = new LoginUser();
        target.setTenantId("synthetic-target");
        RemoteConfigService config = mock(RemoteConfigService.class);
        when(config.getConfigInt(anyString(), anyString())).thenAnswer(i -> {
            assertTrue(locked.get()); events.add("limit"); return -1;
        });
        TenantLoginSessionService service = service(config);
        try (var login = mockStatic(LoginHelper.class); var tenant = mockStatic(TenantHelper.class)) {
            login.when(() -> LoginHelper.withTokenSessionLock(any())).thenAnswer(i -> {
                locked.set(true); events.add("lock");
                try { return ((Supplier<?>) i.getArgument(0)).get(); }
                finally { locked.set(false); events.add("unlock"); }
            });
            tenant.when(TenantHelper::clearDynamic).thenAnswer(i -> {
                assertTrue(locked.get()); events.add("clear"); return null;
            });
            login.when(() -> LoginHelper.updateLoginUser(target)).thenAnswer(i -> {
                assertTrue(locked.get()); assertEquals("pc", target.getDeviceType());
                events.add("update"); return null;
            });
            service.switchTenant(target, "pc");
        }
        assertEquals(List.of("lock", "clear", "update", "limit", "unlock"), events);
    }

    @Test
    void failedSwitchPropagatesAndDoesNotApplyConcurrentLoginLimit() throws Exception {
        AtomicBoolean locked = new AtomicBoolean();
        LoginUser target = new LoginUser();
        RemoteConfigService config = mock(RemoteConfigService.class);
        try (var login = mockStatic(LoginHelper.class); var tenant = mockStatic(TenantHelper.class)) {
            login.when(() -> LoginHelper.withTokenSessionLock(any())).thenAnswer(i -> {
                locked.set(true);
                try { return ((Supplier<?>) i.getArgument(0)).get(); }
                finally { locked.set(false); }
            });
            login.when(() -> LoginHelper.updateLoginUser(target)).thenThrow(new IllegalStateException("synthetic failure"));
            assertThrows(IllegalStateException.class, () -> service(config).switchTenant(target, "pc"));
            assertFalse(locked.get());
            verifyNoInteractions(config);
        }
    }

    private TenantLoginSessionService service(RemoteConfigService config) throws Exception {
        TenantLoginSessionService service = new TenantLoginSessionService();
        var field = TenantLoginSessionService.class.getDeclaredField("remoteConfigService");
        field.setAccessible(true); field.set(service, config);
        return service;
    }
}
