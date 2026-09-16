package com.ym.system.ownership.model;

import lombok.Value;
import java.time.Instant;

/** Immutable authoritative ownership. Null tenant means unassigned, never the default tenant. */
@Value
public class DeviceOwnershipSnapshot {
    Long deviceId;
    String tenantId;
    Long assignmentVersion;
    Instant effectiveFrom;
    String fenceStatus;
}
