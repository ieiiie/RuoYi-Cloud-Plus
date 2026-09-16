package com.ym.system.ownership.model;

import java.time.Instant;

/** Each version owns [effectiveFrom,effectiveTo); historical business rows keep their stored tenant. */
public record OwnershipHistoryVo(Long deviceId, String tenantId, String previousTenantId,
    Long assignmentVersion, Instant effectiveFrom, Instant effectiveTo, String action,
    Long operatorId, String operatorTenantId, String reason, String operatorSource) { }
