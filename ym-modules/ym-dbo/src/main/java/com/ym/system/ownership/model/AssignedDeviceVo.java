package com.ym.system.ownership.model;
import java.time.Instant;
public record AssignedDeviceVo(Long deviceId, String deviceCode, String deviceName, String tenantId,
    Long assignmentVersion, Instant effectiveFrom, String fenceStatus) { }
