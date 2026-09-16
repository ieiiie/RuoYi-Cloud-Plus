package com.ym.iot.ownership.domain.dto;

import java.time.Instant;

/** 数据库中的完整归属区间。仅用于服务端授权，不直接返回其他租户信息。 */
public record HistoryOwnershipRow(
        Long deviceId,
        String tenantId,
        Long assignmentVersion,
        Instant effectiveFrom,
        Instant effectiveTo,
        String metadataSnapshot) {}
