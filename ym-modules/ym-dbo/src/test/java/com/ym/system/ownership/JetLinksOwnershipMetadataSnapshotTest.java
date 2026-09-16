package com.ym.system.ownership;

import com.ym.common.core.exception.ServiceException;
import com.ym.jetlinks.rpc.IotCatalogRpcService;
import com.ym.jetlinks.rpc.IotDeviceRpcService;
import com.ym.jetlinks.rpc.RecordDto;
import com.ym.system.ownership.service.JetLinksOwnershipMetadataSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** RPC 使用离线替身；验证快照白名单和失败边界，不访问硬件、真实租户或数据库。 */
class JetLinksOwnershipMetadataSnapshotTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private IotDeviceRpcService devices;
    private IotCatalogRpcService catalog;
    private JetLinksOwnershipMetadataSnapshot snapshots;

    @BeforeEach void setup() {
        devices = mock(IotDeviceRpcService.class);
        catalog = mock(IotCatalogRpcService.class);
        snapshots = new JetLinksOwnershipMetadataSnapshot(mapper);
        ReflectionTestUtils.setField(snapshots, "devices", devices);
        ReflectionTestUtils.setField(snapshots, "catalog", catalog);
        when(devices.device("10")).thenReturn(CompletableFuture.completedFuture(device(Map.of())));
        when(catalog.properties("20")).thenReturn(CompletableFuture.completedFuture(List.of(property(Map.of()))));
    }

    private static RecordDto device(Map<String, Object> extra) {
        Map<String, Object> data = new LinkedHashMap<>(Map.of(
            "deviceName", "转移前设备", "deviceCode", "SN10", "productId", "20",
            "productName", "环境传感器", "productKey", "weather", "deviceCategory", "SENSOR"));
        data.putAll(extra);
        return new RecordDto("10", data, 12L);
    }

    private static RecordDto property(Map<String, Object> extra) {
        Map<String, Object> data = new LinkedHashMap<>(Map.of(
            "propertyId", "30", "productId", "20", "identifier", "temperature",
            "metricCode", "air_temp", "name", "温度", "unit", "℃"));
        data.putAll(extra);
        return new RecordDto("30", data, 7L);
    }

    @Test @SuppressWarnings("unchecked") void keepsOnlyDisplayWhitelistAndRoundTripsRecordDto() {
        // 测试标记不是凭证；包括未知字段，确保不能因 DTO 将来扩展而扩大历史暴露范围。
        Map<String, Object> privateFields = Map.of(
            "deviceSecret", "DO_NOT_SERIALIZE", "tenantId", "DO_NOT_SERIALIZE",
            "position", Map.of("longitude", "DO_NOT_SERIALIZE"),
            "configuration", Map.of("token", "DO_NOT_SERIALIZE"), "futureField", "DO_NOT_SERIALIZE");
        when(devices.device("10")).thenReturn(CompletableFuture.completedFuture(device(privateFields)));
        when(catalog.properties("20")).thenReturn(CompletableFuture.completedFuture(List.of(property(privateFields))));
        String json = snapshots.capture(10L);
        Map<String, Object> result = mapper.readValue(json, Map.class);
        assertEquals(Set.of("deviceId", "deviceCode", "deviceName", "productId", "productName",
            "productKey", "deviceCategory", "properties"), result.keySet());
        assertEquals("10", result.get("deviceId"));
        assertEquals("转移前设备", result.get("deviceName"));
        assertFalse(json.contains("DO_NOT_SERIALIZE"));
        Map<String, Object> raw = ((List<Map<String, Object>>) result.get("properties")).getFirst();
        assertEquals(Set.of("id", "data", "version"), raw.keySet());
        RecordDto row = mapper.readValue(mapper.writeValueAsString(raw), RecordDto.class);
        assertEquals("30", row.id()); assertEquals(7L, row.version());
        assertEquals(Set.of("propertyId", "productId", "identifier", "metricCode", "name", "unit"), row.data().keySet());
        assertEquals("air_temp", row.data().get("metricCode")); assertEquals("℃", row.data().get("unit"));
        verify(devices).device("10"); verify(catalog).properties("20");
        verifyNoMoreInteractions(devices, catalog);
    }

    @Test void permitsEmptyPropertiesAndDoesNotGuessAbsentMetricCodeOrUnit() {
        when(catalog.properties("20")).thenReturn(CompletableFuture.completedFuture(List.of()));
        assertTrue(mapper.readTree(snapshots.capture(10L)).get("properties").isEmpty());
        Map<String, Object> missing = new LinkedHashMap<>(); missing.put("metricCode", null); missing.put("unit", null);
        when(catalog.properties("20")).thenReturn(CompletableFuture.completedFuture(List.of(property(missing))));
        var data = mapper.readTree(snapshots.capture(10L)).get("properties").get(0).get("data");
        assertTrue(data.get("metricCode").isNull()); assertTrue(data.get("unit").isNull());
        assertEquals("temperature", data.get("identifier").asString());
    }

    @Test void missingDeviceNullPropertiesAndForeignProductFailClosed() {
        when(devices.device("10")).thenReturn(CompletableFuture.completedFuture(null));
        assertThrows(ServiceException.class, () -> snapshots.capture(10L)); verifyNoInteractions(catalog);
        when(devices.device("10")).thenReturn(CompletableFuture.completedFuture(device(Map.of())));
        when(catalog.properties("20")).thenReturn(CompletableFuture.completedFuture(null));
        assertThrows(ServiceException.class, () -> snapshots.capture(10L));
        when(catalog.properties("20")).thenReturn(CompletableFuture.completedFuture(List.of(property(Map.of("productId", "99")))));
        assertThrows(ServiceException.class, () -> snapshots.capture(10L));
    }

    @Test void nestedObjectCannotEscapeThroughAllowedDisplayField() {
        when(devices.device("10")).thenReturn(CompletableFuture.completedFuture(device(
            Map.of("deviceName", Map.of("token", "DO_NOT_SERIALIZE")))));
        ServiceException error = assertThrows(ServiceException.class, () -> snapshots.capture(10L));
        assertFalse(error.getMessage().contains("DO_NOT_SERIALIZE")); assertNull(error.getCause());
        verifyNoInteractions(catalog);
    }

    @Test void remoteExceptionDetailsDoNotEscapeIntoOuterMessageOrCause() {
        when(catalog.properties("20")).thenReturn(CompletableFuture.failedFuture(
            new IllegalStateException("rpc://fixture?credential=DO_NOT_SERIALIZE")));
        ServiceException error = assertThrows(ServiceException.class, () -> snapshots.capture(10L));
        assertFalse(error.getMessage().contains("DO_NOT_SERIALIZE")); assertNull(error.getCause());
    }

    @Test @SuppressWarnings("unchecked") void timeoutAndInterruptionFailWithoutWaitingAndPreserveInterruptFlag() throws Exception {
        CompletableFuture<RecordDto> pending = mock(CompletableFuture.class);
        when(devices.device("10")).thenReturn(pending);
        when(pending.get(7, TimeUnit.SECONDS)).thenThrow(new TimeoutException("DO_NOT_SERIALIZE"));
        assertThrows(ServiceException.class, () -> snapshots.capture(10L));
        doThrow(new InterruptedException("DO_NOT_SERIALIZE")).when(pending).get(7, TimeUnit.SECONDS);
        try {
            assertThrows(ServiceException.class, () -> snapshots.capture(10L));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }
}
