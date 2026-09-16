package com.ym.iot.dubbo;

import cn.hutool.core.bean.BeanUtil;

import com.ym.iot.api.RemoteIotTelemetryService;
import com.ym.iot.api.domain.vo.RemoteLatestTelemetryVo;
import com.ym.iot.api.domain.vo.RemotePestChartVo;
import com.ym.iot.api.domain.vo.RemoteSeriesVo;
import com.ym.iot.device.domain.vo.IotLatestVo;
import com.ym.iot.device.domain.vo.IotPestChartVo;
import com.ym.iot.device.service.IIotDataPointService;

import lombok.RequiredArgsConstructor;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 遥测跨服务实现。 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteIotTelemetryServiceImpl implements RemoteIotTelemetryService {

    private final IIotDataPointService dataPointService;

    @Override
    public Map<Long, RemoteLatestTelemetryVo> getLatestMap(Collection<Long> deviceIds) {
        return mapLatest(dataPointService.getLatestMap(deviceIds));
    }

    @Override
    public Map<Long, RemoteLatestTelemetryVo> getLatestMapFresh(Collection<Long> deviceIds) {
        return mapLatest(dataPointService.getLatestMapFresh(deviceIds));
    }

    @Override
    public RemotePestChartVo getPestChart(Long deviceId, Date from, Date to) {
        return mapPest(dataPointService.getPestChart(deviceId, from, to));
    }

    @Override
    public RemoteSeriesVo getSeries(Long deviceId, String metricCode, Date from, Date to, Integer step) {
        var source = dataPointService.getSeries(deviceId, metricCode, from, to, step);
        RemoteSeriesVo target = BeanUtil.toBean(source, RemoteSeriesVo.class);
        target.setPoints(source.getPoints() == null ? List.of() : source.getPoints().stream()
            .map(point -> BeanUtil.toBean(point, RemoteSeriesVo.SeriesPoint.class)).toList());
        return target;
    }

    private static Map<Long, RemoteLatestTelemetryVo> mapLatest(Map<Long, IotLatestVo> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<Long, RemoteLatestTelemetryVo> result = new LinkedHashMap<>(source.size());
        source.forEach((id, value) -> result.put(id, BeanUtil.toBean(value, RemoteLatestTelemetryVo.class)));
        return result;
    }

    private static RemotePestChartVo mapPest(IotPestChartVo source) {
        if (source == null) {
            return null;
        }
        RemotePestChartVo target = new RemotePestChartVo();
        target.setDeviceId(source.getDeviceId());
        target.setTimes(source.getTimes());
        target.setTotalSeries(mapList(source.getTotalSeries(), RemotePestChartVo.TimeValuePoint.class));
        target.setPestSeries(mapList(source.getPestSeries(), RemotePestChartVo.PestStackSeries.class));
        target.setRecords(source.getRecords() == null ? List.of() : source.getRecords().stream()
            .map(RemoteIotTelemetryServiceImpl::mapRecord).toList());
        target.setDefaultRecord(mapRecord(source.getDefaultRecord()));
        return target;
    }

    private static RemotePestChartVo.PestRecord mapRecord(IotPestChartVo.PestRecord source) {
        if (source == null) {
            return null;
        }
        RemotePestChartVo.PestRecord target = BeanUtil.toBean(source, RemotePestChartVo.PestRecord.class);
        target.setItems(mapList(source.getItems(), RemotePestChartVo.PestItem.class));
        return target;
    }

    private static <S, T> List<T> mapList(List<S> source, Class<T> targetType) {
        return source == null ? List.of() : source.stream().map(row -> BeanUtil.toBean(row, targetType)).toList();
    }
}
