package com.ym.iot.device.service;

import com.ym.iot.device.domain.bo.IotTelemetryIngressBo;
import com.ym.iot.device.domain.dto.IotTelemetryIngestResult;

/**
 * 遥测统一接入服务。
 */
public interface IIotTelemetryIngressService {

    default int ingest(IotTelemetryIngressBo bo) {
        return ingestWithResult(bo).pointsWritten();
    }

    IotTelemetryIngestResult ingestWithResult(IotTelemetryIngressBo bo);
}
