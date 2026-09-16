package com.ym.iot.device.telemetry;

import com.ym.iot.device.domain.IotDataPoint;
import com.ym.iot.device.domain.dto.IotTelemetryIngressContext;

import java.util.List;

/**
 * 将设备协议负载转换为平台统一遥测数据点。
 */
public interface IotTelemetryAdapter {

    String key();

    List<IotDataPoint> adapt(IotTelemetryIngressContext context);
}
