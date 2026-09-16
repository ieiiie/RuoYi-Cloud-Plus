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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中科合肥 {@code GET /iot/device/listByDeviceId/{deviceId}} 返回体中 {@code data} 对象的解析适配器。
 * <p>
 * {@code payload} 须为<strong>单个</strong>上报对象的 {@code data} 字段序列化后的 JSON 字符串（不含外层 deviceId、timestamp 等）。
 * 标量字段映射为测点；嵌套对象/数组写入 {@link IotDataPoint#setValueText(String)}（JSON 文本）。
 * 采集时间优先取 {@code dataTime}、{@code createTime}。
 *
 * @see com.ym.iot.hfzk.service.IHfzkDeviceTelemetryService
 */
@Component
public class HfzkGatewayTelemetryAdapter implements IotTelemetryAdapter {

    public static final String ADAPTER_KEY = "hfzk-gateway-json";
    private static final String NUMBER_METRIC_CODE = "hfzk_number";
    private static final String BUGER_LIST_METRIC_CODE = "hfzk_bugerList";

    private static final Set<String> SKIP_METRIC_KEYS = Set.of(
        "id", "sn", "coding",
        "dataTime", "createTime", "timestamp", "ts", "time",
        "imageName",
        "buger", "bugerList", "coords"
    );

    /** 匹配前导数字（含正负号、小数）及尾部单位：如 "58μg/m3" → group(1)="58", group(2)="μg/m3" */
    private static final Pattern VALUE_UNIT_PATTERN = Pattern.compile("^([+-]?\\d+(?:\\.\\d+)?)\\s*(.+)$");

    /** 匹配纯数字（不带单位）：如 "28.50" */
    private static final Pattern PURE_NUMBER_PATTERN = Pattern.compile("^[+-]?\\d+(?:\\.\\d+)?$");

    @Override
    public String key() {
        return ADAPTER_KEY;
    }

    @Override
    public List<IotDataPoint> adapt(IotTelemetryIngressContext context) {
        String raw = context.getPayload();
        if (raw == null || raw.isBlank()) {
            throw new ServiceException("payload 为空");
        }
        final JSONObject root;
        try {
            root = JSON.parseObject(raw);
        } catch (JSONException e) {
            throw new ServiceException("hfzk-gateway-json 解析失败: " + e.getMessage());
        }
        if (root == null || root.isEmpty()) {
            throw new ServiceException("hfzk-gateway-json 根对象为空");
        }
        Date fallback = context.getDefaultCollectTime() != null ? context.getDefaultCollectTime() : new Date();
        Date collectTime = parseHfzkCollectTime(root, fallback);
        List<IotDataPoint> out = new ArrayList<>();
        boolean pestPayload = isPestPayload(root);
        for (String key : root.keySet()) {
            if (key == null || key.isBlank() || SKIP_METRIC_KEYS.contains(key)) {
                continue;
            }
            Object val = root.get(key);
            if (val == null) {
                continue;
            }
            out.add(pointFromValue(key.trim(), val, collectTime));
        }
        if (pestPayload && root.get("number") == null) {
            out.add(numberZeroPoint(collectTime));
        }
        normalizeBugerList(root, collectTime, out, pestPayload);
        if (out.isEmpty()) {
            IotDataPoint rawPoint = new IotDataPoint();
            rawPoint.setMetricCode("hfzk_payload");
            rawPoint.setValueText(raw.length() > 8000 ? raw.substring(0, 8000) + "…" : raw);
            rawPoint.setCollectTime(collectTime);
            out.add(rawPoint);
        }
        return out;
    }

    /**
     * 将 bugerList 数组归一化为通用结构 [{label, value, date}] 后存为单个测点。
     */
    private static void normalizeBugerList(JSONObject root, Date collectTime, List<IotDataPoint> out, boolean pestPayload) {
        JSONArray bugerList = root.getJSONArray("bugerList");
        if (bugerList == null || bugerList.isEmpty()) {
            if (pestPayload) {
                out.add(bugerListPoint(collectTime, new JSONArray()));
            }
            return;
        }
        JSONArray normalized = HfzkPestItems.normalizeVendor(bugerList);
        if (normalized.isEmpty() && !pestPayload) {
            return;
        }
        out.add(bugerListPoint(collectTime, normalized));
    }

    private static IotDataPoint numberZeroPoint(Date collectTime) {
        IotDataPoint p = new IotDataPoint();
        p.setMetricCode(NUMBER_METRIC_CODE);
        p.setPropertyIdentifier(NUMBER_METRIC_CODE);
        p.setCollectTime(collectTime);
        p.setMetricValue(BigDecimal.ZERO);
        return p;
    }

    private static IotDataPoint bugerListPoint(Date collectTime, JSONArray normalized) {
        IotDataPoint p = new IotDataPoint();
        p.setMetricCode(BUGER_LIST_METRIC_CODE);
        p.setPropertyIdentifier(BUGER_LIST_METRIC_CODE);
        p.setCollectTime(collectTime);
        p.setValueText(normalized.toJSONString());
        return p;
    }

    private static boolean isPestPayload(JSONObject root) {
        return root.containsKey("bugerList")
            || root.containsKey("buger")
            || root.containsKey("number")
            || root.containsKey("image")
            || root.containsKey("newImage")
            || root.containsKey("yImage");
    }

    private static IotDataPoint pointFromValue(String metricCode, Object val, Date collectTime) {
        IotDataPoint p = new IotDataPoint();
        String code = "hfzk_" + metricCode;
        p.setMetricCode(code);
        p.setPropertyIdentifier(code);
        p.setCollectTime(collectTime);
        if (val instanceof Number n) {
            p.setMetricValue(new BigDecimal(n.toString()));
        } else if (val instanceof Boolean b) {
            p.setValueText(b ? "true" : "false");
        } else if (val instanceof String s) {
            p.setValueText(s);
            parseStringValueAndUnit(p, s);
        } else if (val instanceof JSONObject || val instanceof JSONArray) {
            p.setValueText(JSON.toJSONString(val));
        } else {
            p.setValueText(String.valueOf(val));
        }
        return p;
    }

    /**
     * 从字符串中拆分数值和单位。
     * <ul>
     *   <li>{@code "58μg/m3"} → metricValue=58, metricUnit="μg/m3"</li>
     *   <li>{@code "1013.3hPa"} → metricValue=1013.3, metricUnit="hPa"</li>
     *   <li>{@code "28.50"} → metricValue=28.50, metricUnit=null</li>
     *   <li>{@code "65.00%"} → metricValue=65.00, metricUnit="%"</li>
     *   <li>非数字开头的字符串不做拆分</li>
     * </ul>
     */
    private static void parseStringValueAndUnit(IotDataPoint p, String s) {
        if (s == null || s.isBlank()) {
            return;
        }
        String trimmed = s.trim();

        if (PURE_NUMBER_PATTERN.matcher(trimmed).matches()) {
            try {
                p.setMetricValue(new BigDecimal(trimmed));
            } catch (NumberFormatException ignored) {
                // keep as valueText only
            }
            return;
        }

        Matcher m = VALUE_UNIT_PATTERN.matcher(trimmed);
        if (m.matches()) {
            String numPart = m.group(1);
            String unitPart = m.group(2).trim();
            try {
                p.setMetricValue(new BigDecimal(numPart));
            } catch (NumberFormatException ignored) {
                return;
            }
            if (!unitPart.isEmpty()) {
                p.setMetricUnit(unitPart);
            }
        }
    }

    private static Date parseHfzkCollectTime(JSONObject data, Date fallback) {
        for (String k : List.of("dataTime", "createTime", "timestamp", "ts", "time")) {
            String s = data.getString(k);
            if (s == null || s.isBlank()) {
                continue;
            }
            Date d = tryParseTimeString(s.trim());
            if (d != null) {
                return d;
            }
        }
        Object num = data.get("timestamp");
        if (num instanceof Number n) {
            long ms = n.longValue();
            if (ms < 1_000_000_000_000L) {
                ms *= 1000;
            }
            return new Date(ms);
        }
        return fallback;
    }

    private static Date tryParseTimeString(String s) {
        try {
            return Date.from(java.time.Instant.parse(s));
        } catch (Exception ignored) {
            // continue
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {
            // continue
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {
            return null;
        }
    }
}
