package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.await;
import static com.ym.iot.jetlinks.service.impl.JetLinksBusinessProjection.*;

import com.ym.common.core.exception.ServiceException;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.domain.dto.CategoryProjection;
import com.ym.iot.jetlinks.mapper.JetLinksCatalogMapper;
import com.ym.iot.jetlinks.support.JetLinksMapping;
import com.ym.iot.product.domain.IotProduct;
import com.ym.jetlinks.rpc.*;

import java.util.*;

/**
 * Catalog/status mirrors share the durable receipt transaction. Native alarms are never mirrored.
 */
final class JetLinksCatalogAlarmProjection {
    private final JetLinksCatalogMapper catalogMapper;
    private final JetLinksRpcClient rpc;
    private final JetLinksBusinessProjection projection;

    JetLinksCatalogAlarmProjection(
            JetLinksCatalogMapper catalogMapper,
            JetLinksRpcClient rpc,
            JetLinksBusinessProjection projection) {
        this.catalogMapper = catalogMapper;
        this.rpc = rpc;
        this.projection = projection;
    }

    void status(ChangeEventDto event) {
        String key = "status:" + event.deviceId();
        var previous = projection.state(key);
        if (!newer(previous, event.sourceTime(), event.version())) return;
        var data = event.data();
        String status = required(data, "onlineStatus");
        if (!Set.of("ONLINE", "OFFLINE", "UNKNOWN").contains(status))
            throw new ServiceException("非法核心连接状态");
        if ("OFFLINE".equals(status) && data.get("offlineSince") == null)
            throw new ServiceException("权威离线事实缺少offlineSince");
        if (Boolean.FALSE.equals(data.get("authoritative"))) status = "UNKNOWN";
        if (catalogMapper.countDevice(JetLinksMapping.id(event.deviceId())) == 0)
            projection.device(
                    new ChangeEventDto(
                            event.eventId() + ":catalog",
                            "DEVICE_CHANGED",
                            event.deviceId(),
                            event.sourceTime(),
                            event.receivedAt(),
                            event.version(),
                            projection.core(event.deviceId())));
        catalogMapper.updateDeviceOnline(status, JetLinksMapping.id(event.deviceId()));
        projection.state(key, event.sourceTime(), event.version(), data);
    }

    void product(ChangeEventDto event) {
        String id = required(event.data(), "productId"), key = "product:" + id;
        var previous = projection.state(key);
        if (!newer(previous, event.sourceTime(), event.version())) return;
        if (event.type().contains("ARCHIVED")) {
            catalogMapper.archiveProduct(JetLinksMapping.id(id));
            projection.state(
                    key,
                    event.sourceTime(),
                    event.version(),
                    Map.of("productId", id, "archived", true));
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>(event.data());
        if (text(data, "productName") == null
                || text(data, "productKey") == null
                || text(data, "deviceCategory") == null) {
            RecordDto current = await(rpc.getCatalog().product(id));
            if (current == null || !id.equals(current.id()))
                throw new ServiceException("核心产品镜像缺少完整档案: " + id);
            data.putAll(current.data());
        }
        Map<String, Object> fields = entityColumns(data, IotProduct.class);
        fields.put("product_id", JetLinksMapping.id(id));
        fields.put("tenant_id", null);
        fields.put("product_name", required(data, "productName"));
        fields.put("product_key", required(data, "productKey"));
        fields.put("device_category", category(required(data, "deviceCategory")));
        fields.putIfAbsent("status", "0");
        fields.put(
                "del_flag",
                event.type().contains("ARCHIVED") || Boolean.TRUE.equals(data.get("archived"))
                        ? "2"
                        : "0");
        projection.upsert("iot_product", "product_id", fields);
        catalogMapper.updateDevicesCategory(fields.get("device_category"), JetLinksMapping.id(id));
        projection.state(key, event.sourceTime(), event.version(), data);
    }

    void category(ChangeEventDto event) {
        String id = required(event.data(), "id"), key = "category:" + id;
        var previous = projection.state(key);
        if (!newer(previous, event.sourceTime(), event.version())) return;
        Map<String, Object> data = new LinkedHashMap<>(event.data());
        String code = category(required(data, "code"));
        var rows = catalogMapper.lockCategoryCode(id);
        String oldCode = rows.isEmpty() ? null : rows.getFirst();
        var categoryRow =
                new CategoryProjection(
                        id,
                        code,
                        required(data, "name"),
                        data.get("parentId") == null ? null : data.get("parentId").toString(),
                        number(data.getOrDefault("sortIndex", 0)),
                        event.type().contains("ARCHIVED") ? 1 : 0,
                        event.sourceTime(),
                        event.version());
        if (rows.isEmpty()) catalogMapper.insertCategory(categoryRow);
        else catalogMapper.updateCategory(categoryRow);
        if (oldCode != null && !oldCode.equals(code)) {
            catalogMapper.renameProductCategory(code, oldCode);
            catalogMapper.renameDeviceCategory(code, oldCode);
        }
        projection.state(key, event.sourceTime(), event.version(), data);
    }

    private static String category(String code) {
        if (code.length() > 64) throw new ServiceException("分类key最长64字符");
        return code;
    }
}
