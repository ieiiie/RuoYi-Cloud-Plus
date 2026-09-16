package com.ym.agriculture.farming.field.support;

import com.ym.iot.api.RemoteIotTelemetryService;
import com.ym.iot.api.domain.vo.RemoteLatestTelemetryVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/** 地块域访问 IoT 遥测的 Dubbo 桥接。 */
@Component
public class SfFieldIotDataPointAccessor {

    @DubboReference
    private RemoteIotTelemetryService remoteIotTelemetryService;

    public Map<Long, RemoteLatestTelemetryVo> getLatestMap(Collection<Long> deviceIds) {
        return remoteIotTelemetryService.getLatestMap(deviceIds);
    }

    public Map<Long, RemoteLatestTelemetryVo> getLatestMapFresh(Collection<Long> deviceIds) {
        return remoteIotTelemetryService.getLatestMapFresh(deviceIds);
    }
}
