package com.ym.iot.device.domain.dto;

import com.ym.iot.device.domain.IotDevice;

import lombok.Builder;
import lombok.Getter;

import java.util.Date;

/**
 * 已完成设备和租户解析的遥测适配上下文。
 */
@Getter
@Builder
public class IotTelemetryIngressContext {

    private final String tenantId;
    private final IotDevice device;
    private final String adapterKey;
    private final String payload;
    private final Date defaultCollectTime;
}
