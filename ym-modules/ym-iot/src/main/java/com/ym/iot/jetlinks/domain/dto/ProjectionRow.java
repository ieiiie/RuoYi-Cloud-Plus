package com.ym.iot.jetlinks.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.device.domain.IotDevice;
import com.ym.iot.fertilizer.domain.IotFertilizerControlLog;
import com.ym.iot.fertilizer.domain.IotFertilizerRecord;
import com.ym.iot.motorvalve.domain.ValveControlLog;
import com.ym.iot.motorvalve.domain.ValveSession;
import com.ym.iot.product.domain.IotProduct;

import java.lang.reflect.Modifier;
import java.util.*;

/** 事件投影的结构化写入参数。仅允许现有实体对应的六张表和真实持久化字段， Mapper 中的表名、列名只能来自此对象；业务值仍由 MyBatis 绑定。 */
public final class ProjectionRow {
    private record Target(String key, Class<?> entity) {}

    private static final Map<String, Target> TARGETS =
            Map.of(
                    "iot_device", new Target("device_id", IotDevice.class),
                    "iot_product", new Target("product_id", IotProduct.class),
                    "iot_fertilizer_record", new Target("record_id", IotFertilizerRecord.class),
                    "iot_fertilizer_control_log",
                            new Target("log_id", IotFertilizerControlLog.class),
                    "iot_motorvalve_valve_session", new Target("id", ValveSession.class),
                    "iot_motorvalve_control_log", new Target("id", ValveControlLog.class));
    private final String table;
    private final String key;
    private final Map<String, Object> fields;

    private ProjectionRow(String table, String key, Map<String, Object> fields) {
        Target target = TARGETS.get(table);
        if (target == null
                || !target.key().equals(key)
                || fields == null
                || fields.get(key) == null) throw new ServiceException("不支持的业务投影表或主键");
        Set<String> columns = new HashSet<>();
        for (Class<?> type = target.entity(); type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                TableField annotation = field.getAnnotation(TableField.class);
                if (annotation != null && !annotation.exist()) continue;
                String name =
                        annotation != null && !annotation.value().isBlank()
                                ? annotation.value()
                                : field.getName()
                                        .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                                        .toLowerCase(Locale.ROOT);
                columns.add(name);
            }
        }
        if (!columns.containsAll(fields.keySet())) throw new ServiceException("业务投影包含未声明字段");
        this.table = table;
        this.key = key;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static ProjectionRow of(String table, String key, Map<String, Object> fields) {
        return new ProjectionRow(table, key, fields);
    }

    public ProjectionRow updating(Map<String, Object> changes) {
        if (!fields.keySet().containsAll(changes.keySet()))
            throw new ServiceException("更新字段超出投影范围");
        Map<String, Object> next = new LinkedHashMap<>(changes);
        next.put(key, getId());
        return of(table, key, next);
    }

    public String getTable() {
        return table;
    }

    public String getKey() {
        return key;
    }

    public Object getId() {
        return fields.get(key);
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public Map<String, Object> getUpdates() {
        Map<String, Object> updates = new LinkedHashMap<>(fields);
        updates.remove(key);
        updates.remove("tenant_id");
        updates.remove("device_id");
        return Collections.unmodifiableMap(updates);
    }
}
