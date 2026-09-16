package com.ym.iot.device.telemetry;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 遥测适配器注册表。
 */
@Component
public class IotTelemetryAdapterRegistry {

    private final Map<String, IotTelemetryAdapter> adapters;

    public IotTelemetryAdapterRegistry(List<IotTelemetryAdapter> candidates) {
        Map<String, IotTelemetryAdapter> registry = new HashMap<>();
        for (IotTelemetryAdapter adapter : candidates) {
            String key = adapter.key();
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("遥测适配器 key 不能为空: " + adapter.getClass().getName());
            }
            if (registry.putIfAbsent(key, adapter) != null) {
                throw new IllegalStateException("重复的遥测适配器 key: " + key);
            }
        }
        adapters = Map.copyOf(registry);
    }

    public Optional<IotTelemetryAdapter> get(String key) {
        return key == null ? Optional.empty() : Optional.ofNullable(adapters.get(key.trim()));
    }
}
