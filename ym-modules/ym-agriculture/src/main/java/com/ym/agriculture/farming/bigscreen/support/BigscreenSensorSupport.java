package com.ym.agriculture.farming.bigscreen.support;

import cn.hutool.core.util.StrUtil;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.iot.api.domain.bo.RemoteDeviceQueryBo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.agriculture.farming.bigscreen.config.BigscreenCaliber;
import com.ym.agriculture.farming.dashboard.support.DashboardSensorOnline;
import com.ym.agriculture.farming.field.dao.SfFieldIotMapper;
import com.ym.agriculture.farming.field.model.entity.SfFieldIot;
import com.ym.agriculture.farming.field.support.SfFieldIotDeviceAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 大屏农业传感器筛选与 product_key 子类型解析。
 *
 * @author ym-cloud
 */
@Component
@RequiredArgsConstructor
public class BigscreenSensorSupport {

    private final SfFieldIotMapper fieldIotMapper;
    private final SfFieldIotDeviceAccessor iotDeviceAccessor;

    /**
     * 是否为农业传感器（device_category=AG_SENSOR）。
     */
    public static boolean isAgSensor(RemoteDeviceSummaryVo device) {
        return device != null
            && BigscreenCaliber.SENSOR_DEVICE_CATEGORY.equalsIgnoreCase(
            StrUtil.trimToEmpty(device.getDeviceCategory()));
    }

    /**
     * 是否为施肥机（product_key=FERTILIZER）。
     */
    public static boolean isFertilizer(RemoteDeviceSummaryVo device) {
        return device != null
            && BigscreenCaliber.PRODUCT_KEY_FERTILIZER.equalsIgnoreCase(
            StrUtil.trimToEmpty(device.getProductKey()));
    }

    /**
     * 是否为控制阀（product_key=MOTORVALVE）。
     */
    public static boolean isMotorValve(RemoteDeviceSummaryVo device) {
        return device != null
            && BigscreenCaliber.PRODUCT_KEY_MOTORVALVE.equalsIgnoreCase(
            StrUtil.trimToEmpty(device.getProductKey()));
    }

    /**
     * 按 product_key 解析传感器子类型。
     */
    public static String resolveSensorSubType(RemoteDeviceSummaryVo device) {
        if (device == null) {
            return BigscreenCaliber.SENSOR_SUB_UNKNOWN;
        }
        String key = StrUtil.trimToEmpty(device.getProductKey());
        if (BigscreenCaliber.PRODUCT_KEY_WEATHER.equalsIgnoreCase(key)) {
            return BigscreenCaliber.SENSOR_SUB_WEATHER;
        }
        if (BigscreenCaliber.PRODUCT_KEY_SOIL.equalsIgnoreCase(key)) {
            return BigscreenCaliber.SENSOR_SUB_SOIL;
        }
        if (BigscreenCaliber.PRODUCT_KEY_PEST.equalsIgnoreCase(key)) {
            return BigscreenCaliber.SENSOR_SUB_PEST;
        }
        return BigscreenCaliber.SENSOR_SUB_UNKNOWN;
    }

    /**
     * 地块绑定的农业传感器（sf_field_iot ∩ iot_device AG_SENSOR）。
     */
    public List<RemoteDeviceSummaryVo> listFieldAgSensors(Long fieldId) {
        if (fieldId == null) {
            return List.of();
        }
        List<SfFieldIot> links = fieldIotMapper.selectNormalListByFieldId(fieldId);
        Set<String> sns = links.stream()
            .map(SfFieldIot::getDeviceSn)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .collect(Collectors.toCollection(HashSet::new));
        if (sns.isEmpty()) {
            return List.of();
        }
        RemoteDeviceQueryBo bo = new RemoteDeviceQueryBo();
        bo.setDeviceCodeList(new ArrayList<>(sns));
        List<RemoteDeviceSummaryVo> devices = TenantHelper.ignore(() -> iotDeviceAccessor.queryList(bo));
        if (devices == null || devices.isEmpty()) {
            return List.of();
        }
        return devices.stream()
            .filter(d -> SystemConstants.NORMAL.equals(d.getStatus()))
            .filter(BigscreenSensorSupport::isAgSensor)
            .toList();
    }

    /**
     * 按传感器子类型分组。
     */
    public Map<String, List<RemoteDeviceSummaryVo>> groupBySensorSubType(List<RemoteDeviceSummaryVo> sensors) {
        Map<String, List<RemoteDeviceSummaryVo>> grouped = new LinkedHashMap<>();
        grouped.put(BigscreenCaliber.SENSOR_SUB_WEATHER, new ArrayList<>());
        grouped.put(BigscreenCaliber.SENSOR_SUB_SOIL, new ArrayList<>());
        grouped.put(BigscreenCaliber.SENSOR_SUB_PEST, new ArrayList<>());
        grouped.put(BigscreenCaliber.SENSOR_SUB_UNKNOWN, new ArrayList<>());
        if (sensors == null) {
            return grouped;
        }
        for (RemoteDeviceSummaryVo sensor : sensors) {
            grouped.computeIfAbsent(resolveSensorSubType(sensor), k -> new ArrayList<>()).add(sensor);
        }
        return grouped;
    }

    /**
     * 统计农业传感器在线数。
     */
    public int countOnlineAgSensors(List<RemoteDeviceSummaryVo> sensors, Date now) {
        if (sensors == null || sensors.isEmpty() || now == null) {
            return 0;
        }
        int count = 0;
        for (RemoteDeviceSummaryVo sensor : sensors) {
            if (DashboardSensorOnline.isOnline(sensor, now)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 同子类型优先在线设备作为 primary。
     */
    public RemoteDeviceSummaryVo pickPrimary(List<RemoteDeviceSummaryVo> devices, Date now) {
        if (devices == null || devices.isEmpty()) {
            return null;
        }
        for (RemoteDeviceSummaryVo device : devices) {
            if (DashboardSensorOnline.isOnline(device, now)) {
                return device;
            }
        }
        return devices.get(0);
    }
}
