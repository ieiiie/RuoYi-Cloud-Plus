package com.ym.system.ownership;
import com.ym.system.ownership.service.*;
import com.ym.jetlinks.rpc.*;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.satoken.utils.LoginHelper;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnershipAdaptersTest {
    @Test void operationsActorUsesPlatformPermissionWithoutBusinessTenantOrSuperadminOnlyRestriction() {
        try(var stp=mockStatic(StpUtil.class);var login=mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(42L);
            var actor=new OwnershipActor(); actor.requirePermission("transfer");
            assertEquals(42L,actor.operatorId());
            stp.verify(()->StpUtil.checkPermission("saas:iot-device-ownership:transfer"));
        }
    }
    @Test void alarmCleanupPassesPublishedFenceVersionAndStableRequestIdentity() {
        var rpc=mock(IotAlarmRpcService.class);
        when(rpc.removeDeviceScope(any(),eq("10"),eq(1L))).thenAnswer(inv->{
            RequestContext context=inv.getArgument(0);
            assertEquals("ym-dbo",context.caller()); assertEquals("dbo:42",context.operatorId());
            assertEquals("persisted-request",context.requestId()); assertEquals(3L,context.assignmentVersion());
            return CompletableFuture.completedFuture(true);
        });
        var adapter=new JetLinksOwnershipAlarmScopeCleanup(); ReflectionTestUtils.setField(adapter,"alarms",rpc);
        adapter.removeDevice(10L,"658226",1L,3L,"persisted-request","dbo:42");
        verify(rpc).removeDeviceScope(any(),eq("10"),eq(1L));
        when(rpc.removeDeviceScope(any(),anyString(),anyLong())).thenReturn(CompletableFuture.completedFuture(false));
        assertThrows(ServiceException.class,()->adapter.removeDevice(10L,"658226",1L,3L,"persisted-request","dbo:42"));
    }
    @Test void guardMissingKeyAndUnavailableResponseDenyChange() {
        var rpc=mock(IotBusinessGuardRpcService.class); var adapter=new OwnershipBlockerService();
        ReflectionTestUtils.setField(adapter,"guard",rpc);
        when(rpc.checkOwnershipChange(any())).thenReturn(CompletableFuture.completedFuture(Map.of()));
        assertThrows(ServiceException.class,()->adapter.blockers(10L));
        when(rpc.checkOwnershipChange(any())).thenReturn(CompletableFuture.completedFuture(Map.of("10",List.of("FERTILIZER_LOCAL_TASK(1): running"))));
        assertEquals(1,adapter.blockers(10L).size());
        when(rpc.checkOwnershipChange(any())).thenReturn(CompletableFuture.completedFuture(Map.of("10",List.of())));
        assertTrue(adapter.blockers(10L).isEmpty());
        verify(rpc,never()).checkArchive(any());
    }
    @Test void tenantValidationUsesSaasRowsAndRejectsDeletedExpiredDisabledOrMissing() {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
        try {
            jdbc.execute("CREATE TABLE sys_tenant(tenant_id VARCHAR, company_name VARCHAR, status CHAR, del_flag CHAR, expire_time TIMESTAMP)");
            jdbc.update("INSERT INTO sys_tenant VALUES('000000','Default','0','0',NULL),('disabled','Disabled','1','0',NULL),('deleted','Deleted','0','2',NULL),('expired','Expired','0','0','2020-01-01')");
            var validator=new OwnershipTargetTenantValidator(jdbc);
            validator.requireActive("000000"); assertEquals(1,validator.activeTenants().size());
            for(String id:List.of("disabled","deleted","expired","missing"," 000000 "))
                assertThrows(ServiceException.class,()->validator.requireActive(id));
        } finally { jdbc.execute("SHUTDOWN"); }
    }
}
