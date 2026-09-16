package com.ym.iot.ownership.dubbo;

import com.ym.iot.ownership.mapper.DeviceOwnershipMapper;
import com.ym.iot.ownership.service.IOwnershipBlockerService;
import com.ym.iot.ownership.service.support.DeviceOwnershipRepository;
import com.ym.jetlinks.rpc.IotBusinessGuardRpcService;

import lombok.RequiredArgsConstructor;

import org.apache.dubbo.config.annotation.DubboService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Internal operations-only reverse guard for both business and native JetLinks archival entry
 * points. No HTTP exposure, no tenant context/attachment, no writes. Transport/registry must remain
 * internal; Dubbo's generated service token also prevents direct calls that bypass service
 * discovery.
 */
@DubboService(group = "jetlinks-iot-business", version = "2.0.0", retries = 0, token = "true")
@RequiredArgsConstructor
public class IotBusinessGuardRpcServiceImpl implements IotBusinessGuardRpcService {
    private final DeviceOwnershipRepository repository;
    private final IOwnershipBlockerService blockers;

    private final DeviceOwnershipMapper ownershipMapper;

    @Override
    public CompletableFuture<Map<String, Long>> alarmDeviceAssignments(String scopeKey) {
        if (scopeKey == null || !scopeKey.matches("business:[A-Za-z0-9_-]{1,20}")) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Expected business:<tenant> scope"));
        }
        String tenantId = scopeKey.substring("business:".length());
        try {
            // 使用 IoT 默认库和显式租户条件；此内部查询不修改上下文或业务数据。
            Map<String, Long> assignments = new LinkedHashMap<>();
            for (var assignment : ownershipMapper.selectAlarmAssignments(tenantId)) {
                Long id = assignment.getDeviceId();
                Long version = assignment.getAssignmentVersion();
                if (id == null || id <= 0)
                    throw new IllegalStateException("Invalid ownership device ID");
                if (version == null || version < 0)
                    throw new IllegalStateException("Invalid ownership version");
                assignments.put(id.toString(), version);
            }
            return CompletableFuture.completedFuture(Collections.unmodifiableMap(assignments));
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Alarm assignment lookup unavailable", exception));
        }
    }

    @Override
    public CompletableFuture<Map<String, List<String>>> checkArchive(List<String> deviceIds) {
        return check(deviceIds, true);
    }

    @Override
    public CompletableFuture<Map<String, List<String>>> checkOwnershipChange(
            List<String> deviceIds) {
        return check(deviceIds, false);
    }

    private CompletableFuture<Map<String, List<String>>> check(
            List<String> deviceIds, boolean archive) {
        if (deviceIds == null
                || deviceIds.size() > 200
                || deviceIds.stream().anyMatch(id -> id == null)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            "deviceIds must contain at most 200 non-null IDs"));
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String requestedId : deviceIds) {
            if (result.containsKey(requestedId)) continue;
            result.put(requestedId, checkOne(requestedId, archive));
        }
        return CompletableFuture.completedFuture(Collections.unmodifiableMap(result));
    }

    private List<String> checkOne(String requestedId, boolean archive) {
        final Long deviceId;
        try {
            deviceId = Long.valueOf(requestedId);
            if (deviceId <= 0 || !deviceId.toString().equals(requestedId)) {
                return List.of(
                        "INVALID_DEVICE_ID: deviceId must be a canonical positive Long decimal"
                                + " string");
            }
        } catch (NumberFormatException exception) {
            return List.of(
                    "INVALID_DEVICE_ID: deviceId must be a canonical positive Long decimal string");
        }
        try {
            if (!repository.deviceExists(deviceId)) {
                return List.of(
                        "DEVICE_UNKNOWN: business device is missing or archived; refuse operation"
                                + " until reconciled");
            }
            var ownership = repository.find(deviceId);
            if (archive && ownership != null && !"ACTIVE".equals(ownership.getFenceStatus())) {
                return List.of(
                        "OWNERSHIP_CHANGING: ownership operation is frozen or awaiting"
                                + " synchronization");
            }
            return blockers.blockers(deviceId).stream()
                    .map(
                            blocker ->
                                    blocker.code()
                                            + "("
                                            + blocker.count()
                                            + "): "
                                            + blocker.message())
                    .toList();
        } catch (RuntimeException exception) {
            // Missing business tables / query errors must NEVER be interpreted as an empty blocker
            // list.
            return List.of(
                    "BUSINESS_GUARD_UNAVAILABLE: unable to verify all business dependencies;"
                            + " operation refused");
        }
    }
}
