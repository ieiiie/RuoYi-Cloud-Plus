package com.ym.iot.jetlinks.support;

import com.ym.common.core.exception.ServiceException;
import com.ym.iot.motorvalve.enums.ValveType;
import com.ym.iot.motorvalve.support.MotorvalveValveTypeResolver;

import java.util.*;

/** Technical configuration is authoritative; never infer a valve type from a display name. */
public final class JetLinksValveMetadata {
    /** 历史阀门遥测约定：888 表示该测点未配置，不按实际阀位参与业务统计。 */
    public static final double UNCONFIGURED_CHANNEL_SENTINEL = 888.0;

    private JetLinksValveMetadata() {}

    public static ValveType configured(Map<String, Object> data) {
        return explicit(configuredCode(data));
    }

    public static String configuredCode(Map<String, Object> data) {
        Map<String, Object> configuration = map(data.get("configuration")),
                legacy = map(data.get("configJson"));
        String code = text(configuration, "valveType");
        if (code == null) code = text(data, "valveType");
        if (code == null) code = text(legacy, "valveType");
        if (code == null) code = text(map(configuration.get("ymCatalog")), "valveType");
        if (code == null) throw new ServiceException("核心设备缺少明确 valveType，请在平台补齐技术接入配置");
        explicit(code); // Validate without losing the separate_MOTORVALVE physical channel count.
        return code;
    }

    public static ValveType explicit(String code) {
        String type = MotorvalveValveTypeResolver.inferValveTypeCodeFromProductKey(code);
        return ValveType.fromStrictCode(type == null ? code : type);
    }

    private static Map<String, Object> map(Object value) {
        if (value == null) return new LinkedHashMap<>();
        if (value instanceof String text) return com.alibaba.fastjson2.JSON.parseObject(text);
        if (!(value instanceof Map<?, ?> source)) throw new ServiceException("技术配置对象格式错误");
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(key.toString(), item));
        return result;
    }

    private static String text(Map<String, Object> data, String field) {
        Object value = data.get(field);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}
