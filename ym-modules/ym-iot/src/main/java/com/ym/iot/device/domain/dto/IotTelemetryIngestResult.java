package com.ym.iot.device.domain.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 单次遥测接入结果。
 */
public record IotTelemetryIngestResult(int pointsWritten, Date maxCollectTimeAmongWritten)
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static IotTelemetryIngestResult empty() {
        return new IotTelemetryIngestResult(0, null);
    }
}
