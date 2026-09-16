package com.ym.iot.jetlinks.service;

import com.ym.iot.device.domain.bo.IotTelemetryIngressBo;
import com.ym.iot.device.domain.dto.IotTelemetryIngestResult;
import com.ym.iot.device.service.IIotTelemetryIngressService;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksTelemetryIngressService extends IIotTelemetryIngressService {
    IotTelemetryIngestResult ingestWithResult(IotTelemetryIngressBo bo);
}
