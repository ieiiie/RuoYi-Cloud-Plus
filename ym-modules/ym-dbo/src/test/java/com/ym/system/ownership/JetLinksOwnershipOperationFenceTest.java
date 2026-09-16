package com.ym.system.ownership;

import com.ym.common.core.exception.ServiceException;
import com.ym.system.ownership.service.JetLinksOwnershipOperationFence;
import com.ym.jetlinks.rpc.IotCommandRpcService;
import com.ym.jetlinks.rpc.RequestContext;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JetLinksOwnershipOperationFenceTest {
    private JetLinksOwnershipOperationFence adapter(IotCommandRpcService commands) throws Exception {
        var result=new JetLinksOwnershipOperationFence();
        var field=JetLinksOwnershipOperationFence.class.getDeclaredField("commands");
        field.setAccessible(true); field.set(result,commands); return result;
    }

    @Test void freezeUnfreezeAndReleaseCarryTheirExactTargetVersion() throws Exception {
        var commands=mock(IotCommandRpcService.class);
        var contexts=new ArrayList<RequestContext>();
        when(commands.setOperationFence(any(),eq("10"),anyLong(),anyBoolean())).thenAnswer(invocation->{
            RequestContext context=invocation.getArgument(0);
            assertEquals(invocation.<Long>getArgument(2),context.assignmentVersion());
            assertEquals("ym-dbo",context.caller()); assertEquals("dbo:9",context.operatorId());
            assertTrue(context.requestId().startsWith("dbo-fence-10-"));
            contexts.add(context); return CompletableFuture.completedFuture(true);
        });
        var fence=adapter(commands);
        fence.set(10L,2L,true,9L); fence.set(10L,3L,false,9L); fence.set(10L,5L,true,9L);
        assertEquals(java.util.List.of(2L,3L,5L),contexts.stream().map(RequestContext::assignmentVersion).toList());
        verify(commands,times(3)).setOperationFence(any(),eq("10"),anyLong(),anyBoolean());
    }

    @Test void rpcUsesTenSecondManagementDeadlineWithoutRetries() throws Exception {
        var reference=JetLinksOwnershipOperationFence.class.getDeclaredField("commands").getAnnotation(DubboReference.class);
        assertEquals(10000,reference.timeout()); assertEquals(0,reference.retries());
        assertEquals("jetlinks-iot",reference.group()); assertEquals("1.0.0",reference.version());
    }

    @Test void outerWaitDoesNotCutShortRpcDeadlineAndTimeoutNeverResubmits() throws Exception {
        var commands=mock(IotCommandRpcService.class);
        var future=new CompletableFuture<Boolean>() {
            @Override public Boolean get(long timeout,TimeUnit unit) throws TimeoutException {
                assertTrue(unit.toSeconds(timeout)>=10); throw new TimeoutException("unknown result");
            }
        };
        when(commands.setOperationFence(any(),anyString(),anyLong(),anyBoolean())).thenReturn(future);
        var fence=adapter(commands);
        assertThrows(ServiceException.class,()->fence.set(10L,2L,true,9L));
        verify(commands,times(1)).setOperationFence(any(),eq("10"),eq(2L),eq(true));
    }

    @Test void refusalAndMissingResultDoNotReportSuccess() throws Exception {
        var commands=mock(IotCommandRpcService.class);
        when(commands.setOperationFence(any(),anyString(),anyLong(),anyBoolean()))
            .thenReturn(CompletableFuture.completedFuture(false),CompletableFuture.completedFuture(null));
        var fence=adapter(commands);
        assertThrows(ServiceException.class,()->fence.set(10L,2L,true,9L));
        assertThrows(ServiceException.class,()->fence.set(10L,3L,false,9L));
        verify(commands,times(2)).setOperationFence(any(),eq("10"),anyLong(),anyBoolean());
    }

    @Test void interruptedWaitRetainsInterruptAndDoesNotRetry() throws Exception {
        var commands=mock(IotCommandRpcService.class);
        when(commands.setOperationFence(any(),anyString(),anyLong(),anyBoolean())).thenReturn(new CompletableFuture<>() {
            @Override public Boolean get(long timeout,TimeUnit unit) throws InterruptedException { throw new InterruptedException(); }
        });
        var fence=adapter(commands);
        try {
            assertThrows(ServiceException.class,()->fence.set(10L,2L,true,9L));
            assertTrue(Thread.currentThread().isInterrupted());
            verify(commands,times(1)).setOperationFence(any(),eq("10"),eq(2L),eq(true));
        } finally { Thread.interrupted(); }
    }
}
