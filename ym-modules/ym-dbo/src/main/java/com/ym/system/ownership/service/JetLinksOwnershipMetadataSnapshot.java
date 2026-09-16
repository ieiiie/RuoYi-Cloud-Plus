package com.ym.system.ownership.service;

import com.ym.common.core.exception.ServiceException;
import com.ym.jetlinks.rpc.IotCatalogRpcService;
import com.ym.jetlinks.rpc.IotDeviceRpcService;
import com.ym.jetlinks.rpc.RecordDto;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** 关闭租户持有区间前，从核心目录捕获展示快照；不读取硬件，也不保存完整技术档案。 */
@Component
public class JetLinksOwnershipMetadataSnapshot {
    private static final List<String> DEVICE_FIELDS = List.of(
        "deviceCode", "deviceName", "productId", "productName", "productKey", "deviceCategory");
    private static final List<String> PROPERTY_FIELDS = List.of("identifier", "metricCode", "name", "unit");

    @DubboReference(group="jetlinks-iot",version="2.0.0",check=false,retries=0,timeout=5000)
    private IotDeviceRpcService devices;
    @DubboReference(group="jetlinks-iot",version="2.0.0",check=false,retries=0,timeout=5000)
    private IotCatalogRpcService catalog;
    private final JsonMapper jsonMapper;

    public JetLinksOwnershipMetadataSnapshot(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * 调用方必须持有归属行锁且旧归属处于 FROZEN；返回值仅写入即将关闭的历史行。
     * 空属性列表是合法目录结果，null/异常则拒绝变更，不能用空快照掩盖 RPC 故障。
     *
     * @param deviceId 旧归属的设备 ID，禁止从目标租户或请求体获得展示数据
     * @return 白名单 JSON；ID 为字符串，缺失的可选展示值为 null
     */
    public String capture(Long deviceId) {
        try {
            if (devices == null || catalog == null) throw new IllegalStateException("Snapshot RPC unavailable");
            String id = Objects.requireNonNull(deviceId).toString();
            RecordDto device = devices.device(id).get(7, TimeUnit.SECONDS);
            if (device == null || !id.equals(device.id()) || device.data() == null)
                throw new IllegalStateException("Invalid snapshot device");
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("deviceId", id);
            for (String field : DEVICE_FIELDS) snapshot.put(field, text(device.data(), field));
            String productId = requiredText(device.data(), "productId");
            List<RecordDto> properties = catalog.properties(productId).get(7, TimeUnit.SECONDS);
            if (properties == null) throw new IllegalStateException("Missing snapshot properties");
            List<RecordDto> safeProperties = new ArrayList<>(properties.size());
            for (RecordDto property : properties) {
                if (property == null || property.id() == null || property.id().isBlank() || property.data() == null
                    || !productId.equals(requiredText(property.data(), "productId"))
                    || !property.id().equals(requiredText(property.data(), "propertyId")))
                    throw new IllegalStateException("Invalid snapshot property");
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("propertyId", property.id());
                data.put("productId", productId);
                for (String field : PROPERTY_FIELDS) data.put(field, text(property.data(), field));
                requiredText(data, "identifier");
                // 重新构造 RecordDto，绝不复制原 data，避免配置、凭证或未来新增字段进入历史。
                safeProperties.add(new RecordDto(property.id(), data, property.version()));
            }
            snapshot.put("properties", safeProperties);
            return jsonMapper.writeValueAsString(snapshot);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ServiceException("归属展示快照读取中断，设备保持冻结，请恢复核心读取服务后重试栅栏同步");
        } catch (Exception error) {
            // 外层会拼接此消息；不能附带远程异常消息、cause、完整 DTO 或 JSON 到日志/响应。
            throw new ServiceException("归属展示快照读取失败，设备保持冻结，请恢复核心读取服务后重试栅栏同步");
        }
    }

    private static String requiredText(Map<String, Object> data, String field) {
        String value = text(data, field);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing snapshot field");
        return value;
    }

    /** 白名单值也须符合目录文本契约，禁止将嵌套配置对象通过 toString 混入展示字段。 */
    private static String text(Map<String, Object> data, String field) {
        Object value = data.get(field);
        if (value == null) return null;
        if (value instanceof String text) return text;
        throw new IllegalStateException("Invalid snapshot field type");
    }
}
