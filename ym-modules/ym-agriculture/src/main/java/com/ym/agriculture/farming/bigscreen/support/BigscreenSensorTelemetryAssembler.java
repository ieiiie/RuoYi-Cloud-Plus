package com.ym.agriculture.farming.bigscreen.support;

import cn.hutool.core.util.NumberUtil;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteLatestTelemetryVo;
import com.ym.iot.api.domain.vo.RemotePestChartVo;
import com.ym.agriculture.farming.bigscreen.config.BigscreenCaliber;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenSensorDeviceVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenSensorGroupVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenSensorTelemetryVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenSoilDepthVo;
import com.ym.agriculture.farming.dashboard.support.DashboardSensorOnline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 农业传感器遥测组装。
 */
@Component
@RequiredArgsConstructor
public class BigscreenSensorTelemetryAssembler {

    private static final int[] SOIL_DEPTHS = {10, 20, 30, 40};

    private final BigscreenSensorSupport sensorSupport;
    private final SfBigscreenIotAccessor iotAccessor;
    private final BigscreenPestChartAssembler pestChartAssembler;

    /**
     * 组装地块传感器遥测面板。
     */
    public SfBigscreenSensorTelemetryVo assemble(Long fieldId, List<RemoteDeviceSummaryVo> sensors,
                                                 Map<Long, RemoteLatestTelemetryVo> latestMap, Date now) {
        SfBigscreenSensorTelemetryVo vo = new SfBigscreenSensorTelemetryVo();
        vo.setFieldId(fieldId);
        if (sensors == null) {
            sensors = List.of();
        }
        vo.setSensorTotalCount(sensors.size());
        DashboardSensorOnline.Counts states = DashboardSensorOnline.count(sensors);
        vo.setSensorOnlineCount(states.online());
        vo.setSensorOfflineCount(states.offline());
        vo.setSensorUnknownCount(states.unknown());
        Map<String, List<RemoteDeviceSummaryVo>> grouped = sensorSupport.groupBySensorSubType(sensors);
        vo.setWeather(buildGroup(BigscreenCaliber.SENSOR_SUB_WEATHER, grouped, latestMap, now));
        vo.setSoil(buildGroup(BigscreenCaliber.SENSOR_SUB_SOIL, grouped, latestMap, now));
        vo.setPest(buildGroup(BigscreenCaliber.SENSOR_SUB_PEST, grouped, latestMap, now));
        return vo;
    }

    private SfBigscreenSensorGroupVo buildGroup(String subType, Map<String, List<RemoteDeviceSummaryVo>> grouped,
                                                Map<Long, RemoteLatestTelemetryVo> latestMap, Date now) {
        SfBigscreenSensorGroupVo group = new SfBigscreenSensorGroupVo();
        group.setSensorSubType(subType);
        List<RemoteDeviceSummaryVo> devices = grouped.getOrDefault(subType, List.of());
        for (RemoteDeviceSummaryVo device : devices) {
            group.getDevices().add(buildDeviceVo(device, latestMap, now));
        }
        RemoteDeviceSummaryVo primaryDevice = sensorSupport.pickPrimary(devices, now);
        if (primaryDevice != null) {
            group.setPrimary(buildDeviceVo(primaryDevice, latestMap, now));
        }
        return group;
    }

    private SfBigscreenSensorDeviceVo buildDeviceVo(RemoteDeviceSummaryVo device, Map<Long, RemoteLatestTelemetryVo> latestMap,
                                                    Date now) {
        SfBigscreenSensorDeviceVo vo = new SfBigscreenSensorDeviceVo();
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceCode(device.getDeviceCode());
        vo.setDeviceName(device.getDeviceName());
        vo.setProductKey(device.getProductKey());
        vo.setSensorSubType(BigscreenSensorSupport.resolveSensorSubType(device));
        vo.setOnline(DashboardSensorOnline.isOnline(device, now));
        vo.setOnlineStatus(DashboardSensorOnline.status(device));
        RemoteLatestTelemetryVo latest = latestMap != null ? latestMap.get(device.getDeviceId()) : null;
        if (latest != null && latest.getMetrics() != null) {
            vo.getMetrics().putAll(new LinkedHashMap<>(latest.getMetrics()));
        }
        if (latest != null && latest.getUnits() != null) {
            vo.getUnits().putAll(latest.getUnits());
        }
        if (BigscreenCaliber.SENSOR_SUB_SOIL.equals(vo.getSensorSubType())) {
            vo.setSoilDepths(parseSoilDepths(latest));
        }
        if (BigscreenCaliber.SENSOR_SUB_PEST.equals(vo.getSensorSubType()) && device.getDeviceId() != null) {
            RemotePestChartVo chart = iotAccessor.getPestChart(device.getDeviceId(), null, null);
            vo.setPestChart(pestChartAssembler.toLatestSnapshot(chart));
        }
        return vo;
    }

    private List<SfBigscreenSoilDepthVo> parseSoilDepths(RemoteLatestTelemetryVo latest) {
        List<SfBigscreenSoilDepthVo> depths = new ArrayList<>(SOIL_DEPTHS.length);
        if (latest == null || latest.getMetrics() == null) {
            return depths;
        }
        Map<String, ?> metrics = latest.getMetrics();
        for (int i = 0; i < SOIL_DEPTHS.length; i++) {
            int idx = i + 1;
            SfBigscreenSoilDepthVo depth = new SfBigscreenSoilDepthVo();
            depth.setDepth(SOIL_DEPTHS[i]);
            depth.setTemp(readDecimal(metrics, "hfzk_wd" + idx, "wd" + idx));
            depth.setHumidity(readDecimal(metrics, "hfzk_sd" + idx, "sd" + idx));
            depth.setEc(readDecimal(metrics, "hfzk_zd" + idx, "zd" + idx));
            depths.add(depth);
        }
        return depths;
    }

    private BigDecimal readDecimal(Map<String, ?> metrics, String... keys) {
        for (String key : keys) {
            Object val = metrics.get(key);
            if (val != null) {
                return NumberUtil.toBigDecimal(String.valueOf(val));
            }
        }
        return null;
    }
}
