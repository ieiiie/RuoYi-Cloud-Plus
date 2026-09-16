package com.ym.iot.jetlinks.service;

import com.ym.iot.device.domain.IotDataPoint;
import com.ym.iot.device.domain.vo.IotDataPointRecordVo;
import com.ym.iot.device.domain.vo.IotLatestVo;
import com.ym.iot.device.domain.vo.IotPestChartVo;
import com.ym.iot.device.domain.vo.IotPestNightChartVo;
import com.ym.iot.device.domain.vo.IotSeriesBatchVo;
import com.ym.iot.device.domain.vo.IotSeriesVo;
import com.ym.iot.device.service.IIotDataPointService;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksTelemetryService extends IIotDataPointService {
    IotLatestVo getLatest(Long id);

    Map<Long, IotLatestVo> getLatestMap(Collection<Long> ids);

    Map<Long, IotLatestVo> getLatestMapFresh(Collection<Long> ids);

    Map<Long, IotLatestVo> getLatestMapCached(Collection<Long> ids);

    Map<Long, IotLatestVo> warmupLatestMap(Collection<Long> ids);

    IotSeriesVo getSeries(Long id, String code, Date from, Date to, Integer step);

    IotSeriesBatchVo getSeriesBatch(Long id, List<String> codes, Date from, Date to, Integer step);

    IotDataPointRecordVo getRecord(Long id, Date time);

    IotDataPointRecordVo getAdjacentRecord(Long id, Date time, boolean previous);

    Date getAdjacentCollectTime(Long id, Date time, boolean previous);

    IotPestChartVo getPestChart(Long id, Date from, Date to);

    IotPestNightChartVo getPestNightChart(Long id, Date from, Date to);

    void appendPoint(IotDataPoint point);

    void appendDataPointStorageOnly(IotDataPoint point);

    void appendDataPointsStorageOnly(List<IotDataPoint> points);
}
