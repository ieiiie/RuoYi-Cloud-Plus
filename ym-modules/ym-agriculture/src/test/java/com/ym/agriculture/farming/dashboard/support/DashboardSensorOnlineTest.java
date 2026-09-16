package com.ym.agriculture.farming.dashboard.support;

import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenOnlineRateVo;
import com.ym.agriculture.farming.bigscreen.service.impl.SfBigscreenServiceImpl;
import com.ym.agriculture.farming.bigscreen.support.SfBigscreenIotAccessor;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import org.junit.jupiter.api.Test;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardSensorOnlineTest {
    private RemoteDeviceSummaryVo device(String state) {
        RemoteDeviceSummaryVo result = new RemoteDeviceSummaryVo();
        result.setOnlineStatus(state);
        return result;
    }

    @Test void recentTelemetryNeverConfirmsUnknownOrOffline() {
        for (String state : List.of("UNKNOWN", "OFFLINE")) {
            var device = device(state);
            device.setLastReportTime(new Date());
            device.setLastRemoteFetchTime(new Date());
            assertFalse(DashboardSensorOnline.isOnline(device, new Date()));
            assertEquals(state, DashboardSensorOnline.status(device));
        }
    }

    @Test void countsIncludeUnknownButRateUsesOnlyConfirmed() {
        var counts = DashboardSensorOnline.count(List.of(device("ONLINE"), device("OFFLINE"), device("UNKNOWN"), device(null)));
        assertEquals(1, counts.online());
        assertEquals(1, counts.offline());
        assertEquals(2, counts.unknown());
        assertEquals(4, counts.total());
        assertEquals(2, counts.confirmed());
        assertEquals(50, counts.pct());
    }

    @Test void unknownOnlyHasZeroConfirmedAndNoOffline() {
        var counts = DashboardSensorOnline.count(List.of(device("UNKNOWN")));
        assertEquals(0, counts.offline());
        assertEquals(0, counts.confirmed());
        assertEquals(0, counts.pct());
        assertEquals(0, DashboardSensorOnline.count(List.of()).pct());
    }

    @Test void invalidAndMissingStatesRemainUnknown() {
        assertEquals("UNKNOWN", DashboardSensorOnline.status(null));
        assertEquals("UNKNOWN", DashboardSensorOnline.status(device("FAULT")));
        assertEquals("UNKNOWN", DashboardSensorOnline.status(device("")));
        assertTrue(DashboardSensorOnline.isOnline(device("ONLINE"), null));
    }

    @Test void overviewUsesAuthoritativeMixedStateCounts() throws ReflectiveOperationException {
        var accessor = mock(SfBigscreenIotAccessor.class);
        when(accessor.queryTenantNormalDevices()).thenReturn(List.of(device("ONLINE"), device("UNKNOWN"), device("UNKNOWN")));
        var service = mock(SfBigscreenServiceImpl.class, CALLS_REAL_METHODS);
        var field = SfBigscreenServiceImpl.class.getDeclaredField("iotAccessor");
        field.setAccessible(true);
        field.set(service, accessor);
        var method = SfBigscreenServiceImpl.class.getDeclaredMethod("buildOnlineRate");
        method.setAccessible(true);
        SfBigscreenOnlineRateVo rate = (SfBigscreenOnlineRateVo) method.invoke(service);
        assertNotNull(rate);
        assertEquals(3, rate.getTotal());
        assertEquals(1, rate.getOnline());
        assertEquals(0, rate.getOffline());
        assertEquals(2, rate.getUnknown());
        assertEquals(1, rate.getConfirmed());
        assertEquals(100, rate.getPct());
    }
}
