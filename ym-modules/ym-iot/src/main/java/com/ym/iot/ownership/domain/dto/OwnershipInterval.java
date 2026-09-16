package com.ym.iot.ownership.domain.dto;

/** 告警历史可见区间，结束时间为空表示区间仍有效。 */
public record OwnershipInterval(
        Long deviceId, java.time.Instant effectiveFrom, java.time.Instant effectiveTo) {}
