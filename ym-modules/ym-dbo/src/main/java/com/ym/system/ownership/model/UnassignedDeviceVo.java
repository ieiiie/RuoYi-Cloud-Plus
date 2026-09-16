package com.ym.system.ownership.model;
public record UnassignedDeviceVo(Long deviceId, String deviceCode, String deviceName,
    Long assignmentVersion, String fenceStatus) { }
