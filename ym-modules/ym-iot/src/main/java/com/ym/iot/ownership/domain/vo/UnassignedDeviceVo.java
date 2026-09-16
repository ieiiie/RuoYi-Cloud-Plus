package com.ym.iot.ownership.domain.vo;

public record UnassignedDeviceVo(Long deviceId, String deviceCode, String deviceName,
    Long assignmentVersion, String fenceStatus) { }
