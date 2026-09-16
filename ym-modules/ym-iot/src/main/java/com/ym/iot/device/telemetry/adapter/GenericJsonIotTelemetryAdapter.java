package com.ym.iot.device.telemetry.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.device.domain.IotDataPoint;
import com.ym.iot.device.domain.dto.IotTelemetryIngressContext;
import com.ym.iot.device.telemetry.IotTelemetryAdapter;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 通用 JSON 遥测适配器。支持数组、metrics 对象和扁平对象。
 */
@Component
public class GenericJsonIotTelemetryAdapter implements IotTelemetryAdapter {

    public static final String ADAPTER_KEY = "generic-json";
    private static final Set<String> RESERVED_KEYS = Set.of(
        "collectTime", "timestamp", "ts", "time", "deviceTime", "metrics");

    @Override
    public String key() {
        return ADAPTER_KEY;
    }

    @Override
    public List<IotDataPoint> adapt(IotTelemetryIngressContext context) {
        Object root;
        try {
            root = JSON.parse(context.getPayload());
        } catch (JSONException ex) {
            throw new ServiceException("JSON 解析失败: " + ex.getMessage());
        }
        Date fallback = context.getDefaultCollectTime() == null ? new Date() : context.getDefaultCollectTime();
        List<IotDataPoint> points = new ArrayList<>();
        if (root instanceof JSONArray array) {
            for (Object item : array) {
                if (item instanceof JSONObject object) {
                    points.add(fromObject(object, fallback));
                }
            }
        } else if (root instanceof JSONObject object) {
            JSONObject metrics = object.getJSONObject("metrics");
            JSONObject values = metrics == null ? object : metrics;
            for (String key : values.keySet()) {
                if (metrics == null && RESERVED_KEYS.contains(key)) {
                    continue;
                }
                points.add(fromValue(key, values.get(key), object, fallback));
            }
        } else {
            throw new ServiceException("generic-json 根节点必须为数组或对象");
        }
        return points;
    }

    private static IotDataPoint fromObject(JSONObject object, Date fallback) {
        String metricCode = object.getString("metricCode");
        if (metricCode == null || metricCode.isBlank()) {
            throw new ServiceException("数组元素缺少 metricCode");
        }
        IotDataPoint point = newPoint(metricCode, object.get("value"), object.get("valueText"));
        point.setMetricUnit(object.getString("unit"));
        String identifier = object.getString("propertyIdentifier");
        point.setPropertyIdentifier(identifier == null || identifier.isBlank() ? metricCode.trim() : identifier.trim());
        point.setCollectTime(parseTime(object, fallback));
        return point;
    }

    private static IotDataPoint fromValue(String metricCode, Object value, JSONObject root, Date fallback) {
        IotDataPoint point = newPoint(metricCode, value, null);
        point.setPropertyIdentifier(metricCode.trim());
        point.setCollectTime(parseTime(root, fallback));
        return point;
    }

    private static IotDataPoint newPoint(String metricCode, Object value, Object valueText) {
        if (metricCode == null || metricCode.isBlank()) {
            throw new ServiceException("metricCode 不能为空");
        }
        IotDataPoint point = new IotDataPoint();
        point.setTypedValue(valueText != null ? valueText : value);
        point.setMetricCode(metricCode.trim());
        if (valueText instanceof String text && !text.isBlank()) {
            point.setValueText(text);
        }
        if (value instanceof Number number) {
            point.setMetricValue(new BigDecimal(number.toString()));
        } else if (value != null) {
            point.setValueText(value instanceof String text ? text : JSON.toJSONString(value));
        }
        return point;
    }

    private static Date parseTime(JSONObject object, Date fallback) {
        for (String key : List.of("collectTime", "timestamp", "ts", "time", "deviceTime")) {
            Object value = object.get(key);
            if (value instanceof Number number) {
                long timestamp = number.longValue();
                return new Date(timestamp < 1_000_000_000_000L ? timestamp * 1000 : timestamp);
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Date.from(Instant.parse(text));
                } catch (Exception ignored) {
                    // 尝试下一个时间字段。
                }
            }
        }
        return fallback;
    }
}
