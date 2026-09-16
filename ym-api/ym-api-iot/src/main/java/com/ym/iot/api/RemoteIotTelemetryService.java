package com.ym.iot.api;

import com.ym.iot.api.domain.vo.RemoteLatestTelemetryVo;
import com.ym.iot.api.domain.vo.RemotePestChartVo;
import com.ym.iot.api.domain.vo.RemoteSeriesVo;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/** 遥测与实时状态跨服务契约。 */
public interface RemoteIotTelemetryService {

    Map<Long, RemoteLatestTelemetryVo> getLatestMap(Collection<Long> deviceIds);

    Map<Long, RemoteLatestTelemetryVo> getLatestMapFresh(Collection<Long> deviceIds);

    RemotePestChartVo getPestChart(Long deviceId, Date from, Date to);

    RemoteSeriesVo getSeries(Long deviceId, String metricCode, Date from, Date to, Integer step);
}
