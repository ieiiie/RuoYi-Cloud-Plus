package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.await;

import cn.hutool.core.bean.BeanUtil;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.device.domain.IotDevice;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.domain.dto.ProjectionRow;
import com.ym.iot.jetlinks.mapper.JetLinksProjectionMapper;
import com.ym.iot.jetlinks.support.JetLinksMapping;
import com.ym.iot.jetlinks.support.JetLinksValveMetadata;
import com.ym.iot.motorvalve.enums.ValveType;
import com.ym.jetlinks.rpc.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;
import java.util.Date;

/** Runs inside the event receipt transaction on the physical business datasource. */
final class JetLinksBusinessProjection {
    private final JetLinksProjectionMapper projectionMapper;
    private final JetLinksRpcClient rpc;
    private final double unconfiguredChannelSentinel;

    JetLinksBusinessProjection(JetLinksProjectionMapper projectionMapper, JetLinksRpcClient rpc) {
        this(projectionMapper, rpc, JetLinksValveMetadata.UNCONFIGURED_CHANNEL_SENTINEL);
    }

    JetLinksBusinessProjection(
            JetLinksProjectionMapper projectionMapper, JetLinksRpcClient rpc, double sentinel) {
        if (!Double.isFinite(sentinel))
            throw new IllegalArgumentException("Valve channel sentinel must be finite");
        this.projectionMapper = projectionMapper;
        this.rpc = rpc;
        this.unconfiguredChannelSentinel = sentinel;
    }

    record Owner(String tenant, long version) {}

    Owner ownerAt(String device, long time, Long version, boolean command) {
        var rows =
                projectionMapper.selectSourceOwners(
                        JetLinksMapping.id(device),
                        version,
                        !command || version == null || version <= 0,
                        new Timestamp(time));
        if (rows.size() != 1 || rows.getFirst().get("tenant_id") == null)
            throw new ServiceException("设备事实没有唯一有效的源时间归属: " + device);
        var row = rows.getFirst();
        return new Owner(row.get("tenant_id").toString(), number(row.get("assignment_version")));
    }

    Map<String, Object> state(String key) {
        projectionMapper.initializeProjection(key, IdWorker.getId());
        return projectionMapper.lockProjection(key);
    }

    void state(String key, long time, long version, Map<String, Object> payload) {
        projectionMapper.updateProjection(time, version, JSON.toJSONString(payload), key);
    }

    static boolean newer(Map<String, Object> previous, long time, long version) {
        return time > number(previous.get("source_time"))
                || time == number(previous.get("source_time"))
                        && version > number(previous.get("version"));
    }

    void fertilizer(ChangeEventDto event) {
        Map<String, Object> data = event.data();
        Object records = data.get("records");
        if (records == null) records = map(data.get("data")).get("records");
        if (records instanceof Collection<?> list) {
            for (Object record : list) fertilizerRecord(event, map(record));
        } else fertilizerRecord(event, data);
    }

    private void fertilizerRecord(ChangeEventDto event, Map<String, Object> record) {
        String source = text(record, "sourceRecordId");
        if (source == null) throw new ServiceException("施肥事实缺少稳定 sourceRecordId");
        String device = Objects.toString(record.get("deviceId"), event.deviceId());
        if (!Objects.equals(device, event.deviceId())) throw new ServiceException("施肥事实设备与外层事件不一致");
        long time = number(record.getOrDefault("sourceTime", record.get("recordTime")));
        Owner owner = ownerAt(device, time, null, false);
        String key = "fertilizer:" + source;
        var previous = state(key);
        if (number(previous.get("source_time")) >= 0) return;
        String raw = text(record, "rawHex");
        if (raw == null || !raw.matches("(?i)[0-9a-f]{24}"))
            throw new ServiceException("施肥事实缺少12字节原文");
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("record_id", previous.get("business_id"));
        fields.put("device_id", JetLinksMapping.id(device));
        fields.put("tenant_id", owner.tenant());
        fields.put("device_code", deviceCode(device));
        fields.put("record_time", new Timestamp(time));
        fields.put("fertilization_type", number(record.get("type")) & 255);
        fields.put("fertilization_seconds", number(record.get("durationMinutes")));
        fields.put(
                "fertilization_quantity",
                new BigDecimal(
                                Objects.requireNonNull(record.get("quantity"), "quantity")
                                        .toString())
                        .setScale(3, RoundingMode.HALF_UP));
        fields.put("raw_payload_hex", raw);
        fields.put("del_flag", "0");
        fields.put("create_time", new Timestamp(event.receivedAt()));
        fields.put("update_time", new Timestamp(event.receivedAt()));
        // Reconcile already migrated legacy rows by their actual source bytes, not time/type alone.
        var existing =
                projectionMapper.findLegacyFertilizerRecord(
                        JetLinksMapping.id(device), owner.tenant(), new Timestamp(time), raw);
        if (existing.isEmpty()) upsert("iot_fertilizer_record", "record_id", fields);
        else projectionMapper.bindLegacyRecord(existing.get(0), key);
        state(
                key,
                time,
                event.version(),
                Map.of("tenantId", owner.tenant(), "assignmentVersion", owner.version()));
    }

    void command(ChangeEventDto event) {
        Map<String, Object> data = event.data();
        String command = required(data, "commandId"),
                request = required(data, "requestId"),
                function = required(data, "functionId"),
                status = required(data, "state");
        long version = number(data.get("assignmentVersion"));
        Owner owner = ownerAt(event.deviceId(), event.sourceTime(), version, true);
        Map<String, Object> result = map(data.get("result"));
        Map<String, Object> protocol = protocol(result);
        long updated = number(data.getOrDefault("updatedAt", event.sourceTime()));
        String key = "command:" + command;
        var previous = state(key);
        if (!newer(previous, updated, event.version())) return;
        var tasks = projectionMapper.lockCommandTasks(request, command);
        if (!tasks.isEmpty()) {
            var task = tasks.get(0);
            validateTask(task, event.deviceId(), owner, version);
            if (!"fertilizer.task".equals(task.get("function_id")))
                projectionMapper.updateCommandFact(
                        command,
                        status,
                        JSON.toJSONString(result),
                        new Timestamp(updated),
                        task.get("request_id"));
        }
        reconcileFertilizerTask(event, data, request, command, status, owner, version);
        boolean valve =
                Set.of(
                                        "controlValve",
                                        "percentControl",
                                        "readData",
                                        "readGatewayStatus",
                                        "syncNtpTime",
                                        "getControlProfile",
                                        "getCurrentPercent")
                                .contains(function)
                        || protocol.containsKey("valveType")
                        || protocol.containsKey("valveNo");
        boolean fertilizer =
                Set.of("control", "writeRegister", "readSettings", "readRecords", "readLocation")
                        .contains(function);
        if (!valve && !fertilizer && "readRealtimeData".equals(function)) {
            String category = Objects.toString(core(event.deviceId()).get("deviceCategory"), "");
            valve = "MOTORVALVE".equals(category);
            fertilizer = "FERTILIZER".equals(category);
        }
        if (valve) valveLog(event, data, protocol, owner, number(previous.get("business_id")));
        if (fertilizer)
            fertilizerLog(event, data, protocol, owner, number(previous.get("business_id")));
        Object records = protocol.get("records");
        if (records instanceof Collection<?> list
                && "readRecords".equals(function)
                && "fertilization".equals(text(map(data.get("inputs")), "kind")))
            for (Object record : list) fertilizerRecord(event, map(record));
        state(
                key,
                updated,
                event.version(),
                Map.of(
                        "tenantId",
                        owner.tenant(),
                        "state",
                        status,
                        "operatorId",
                        Objects.toString(data.get("operatorId"), ""),
                        "caller",
                        Objects.toString(data.get("caller"), "")));
    }

    private void validateTask(Map<String, Object> task, String device, Owner owner, long version) {
        if (number(task.get("device_id")) != JetLinksMapping.id(device)
                || !owner.tenant().equals(task.get("tenant_id"))
                || number(task.get("assignment_version")) != version)
            throw new ServiceException("命令持久任务与回执归属不一致");
    }

    private void reconcileFertilizerTask(
            ChangeEventDto event,
            Map<String, Object> data,
            String request,
            String command,
            String status,
            Owner owner,
            long version) {
        int colon = request.lastIndexOf(':');
        if (colon < 1) return;
        int step;
        try {
            step = Integer.parseInt(request.substring(colon + 1));
        } catch (NumberFormatException ex) {
            return;
        }
        var rows = projectionMapper.lockFertilizerTask(request.substring(0, colon));
        if (rows.isEmpty()) return;
        var task = rows.getFirst();
        validateTask(task, event.deviceId(), owner, version);
        var progress = map(task.get("result_json"));
        if (number(progress.getOrDefault("stepIndex", 0)) != step) return;
        if (Set.of("SUCCEEDED", "FAILED", "TIMED_OUT")
                .contains(Objects.toString(task.get("state")))) return;
        if ("SUCCEEDED".equals(status)) {
            progress.put("stepIndex", step + 1);
            progress.put("lastResult", data.get("result"));
            Object plan = map(task.get("inputs_json")).get("plan");
            if (!(plan instanceof Collection<?> steps)) throw new ServiceException("任务计划缺失");
            projectionMapper.confirmFertilizerStep(
                    step + 1 >= steps.size() ? "SUCCEEDED" : "RUNNING",
                    JSON.toJSONString(progress),
                    new Timestamp(event.sourceTime()),
                    task.get("request_id"));
        } else if (Set.of("FAILED", "TIMED_OUT", "CANCELLED").contains(status)) {
            projectionMapper.failFertilizerStep(
                    command,
                    JSON.toJSONString(data.get("result")),
                    new Timestamp(event.sourceTime()),
                    task.get("request_id"));
        } else
            projectionMapper.updateFertilizerStep(
                    "UNKNOWN".equals(status) ? "UNKNOWN" : "RUNNING",
                    command,
                    new Timestamp(event.sourceTime()),
                    task.get("request_id"));
    }

    private void valveLog(
            ChangeEventDto event,
            Map<String, Object> data,
            Map<String, Object> protocol,
            Owner owner,
            long logId) {
        var inputs = map(data.get("inputs"));
        String function = required(data, "functionId"), status = required(data, "state");
        Map<String, Object> fields = baseLog(event, owner);
        fields.put("id", logId);
        fields.put("create_time", new Timestamp(number(data.get("createdAt"))));
        fields.put("update_time", new Timestamp(number(data.get("updatedAt"))));
        fields.put("create_by", businessOperator(data));
        fields.put("update_by", businessOperator(data));
        fields.put("product_type", protocol.get("productType"));
        fields.put("lora_addr", protocol.get("loraAddr"));
        fields.put("valve_no", protocol.getOrDefault("valveNo", inputs.get("valveNo")));
        fields.put("target_position", protocol.getOrDefault("targetAngle", inputs.get("position")));
        fields.put("target_percent", inputs.get("percent"));
        fields.put("target_channel", inputs.get("channel"));
        fields.put(
                "command_type",
                switch (function) {
                    case "controlValve", "percentControl" -> "CONTROL";
                    case "readGatewayStatus" -> "READ_STATUS";
                    case "syncNtpTime" -> "NTP_SYNC";
                    default -> "READ_DATA";
                });
        fields.put("command_text", protocol.get("payload"));
        fields.put("reply_text", protocol.get("replyText"));
        fields.put("error_code", protocol.get("detail"));
        fields.put(
                "status",
                switch (status) {
                    case "SUCCEEDED" -> "SUCCESS";
                    case "ACKNOWLEDGED" -> "ACK";
                    case "SENT" -> "SENT";
                    case "FAILED", "TIMED_OUT", "CANCELLED" -> "FAILED";
                    default -> status;
                });
        upsert("iot_motorvalve_control_log", "id", fields);
        if ("SUCCEEDED".equals(status) && "targetReached".equals(text(protocol, "status"))) {
            Object actual = protocol.get("actualAngle"), target = protocol.get("targetAngle");
            if (actual == null || target == null || fields.get("valve_no") == null)
                throw new ServiceException("targetReached缺少阀位事实");
            long time =
                    number(
                            protocol.getOrDefault(
                                    "sourceTime",
                                    map(protocol.get("properties"))
                                            .getOrDefault("sourceTime", event.sourceTime())));
            valvePosition(
                    event.deviceId(),
                    owner,
                    (int) number(fields.get("valve_no")),
                    (int) number(target),
                    time,
                    event.version(),
                    valveType(event.deviceId(), protocol),
                    logId,
                    businessOperator(data));
        }
    }

    private void fertilizerLog(
            ChangeEventDto event,
            Map<String, Object> data,
            Map<String, Object> protocol,
            Owner owner,
            long logId) {
        String status = required(data, "state");
        var inputs = map(data.get("inputs"));
        long begin = number(data.get("createdAt")), end = number(data.get("updatedAt"));
        Map<String, Object> fields = baseLog(event, owner);
        fields.put("log_id", logId);
        fields.put("operator_id", businessOperator(data));
        fields.put("operator_name", data.get("operatorName"));
        fields.put("operator_ip", data.get("operatorIp"));
        String request = required(data, "requestId");
        fields.put(
                "task_id",
                request.contains(":") ? request.substring(0, request.lastIndexOf(':')) : request);
        fields.put("command", inputs.getOrDefault("command", data.get("functionId")));
        fields.put("status", "TIMED_OUT".equals(status) ? "TIMEOUT" : status);
        fields.put("error_message", protocol.get("detail"));
        fields.put("request_body", JSON.toJSONString(inputs));
        fields.put("emergency_reason", data.get("emergencyReason"));
        fields.put(
                "downlink_frame", protocol.getOrDefault("downlinkFrame", protocol.get("payload")));
        fields.put("downlink_topic", protocol.get("commandTopic"));
        fields.put("begin_at", new Timestamp(begin));
        fields.put("create_time", new Timestamp(begin));
        if (Set.of("SUCCEEDED", "FAILED", "TIMED_OUT", "CANCELLED").contains(status)) {
            fields.put("end_at", new Timestamp(end));
            fields.put("duration_ms", (int) Math.min(Integer.MAX_VALUE, Math.max(0, end - begin)));
        }
        upsert("iot_fertilizer_control_log", "log_id", fields);
    }

    private Map<String, Object> baseLog(ChangeEventDto event, Owner owner) {
        var map = new LinkedHashMap<String, Object>();
        map.put("device_id", JetLinksMapping.id(event.deviceId()));
        map.put("tenant_id", owner.tenant());
        map.put("device_code", deviceCode(event.deviceId()));
        return map;
    }

    void telemetry(ChangeEventDto event) {
        Map<String, Object> properties = map(event.data().get("properties"));
        if (properties.keySet().stream().noneMatch(k -> k.matches("valve_position_[1-4]"))) return;
        String type = text(event.data(), "valveType");
        if (type == null) type = JetLinksValveMetadata.configuredCode(core(event.deviceId()));
        JetLinksValveMetadata.explicit(type);
        boolean separate = "separate_MOTORVALVE".equalsIgnoreCase(type);
        Map<String, Object> sourceTimes = map(event.data().get("propertySourceTimes"));
        for (int valve = 1; valve <= 4; valve++) {
            String property = "valve_position_" + valve;
            Object value = properties.get(property);
            if (value == null) continue;
            long time = number(sourceTimes.getOrDefault(property, event.sourceTime()));
            Owner owner = ownerAt(event.deviceId(), time, null, false);
            BigDecimal angle;
            try {
                angle = new BigDecimal(value.toString());
            } catch (NumberFormatException ex) {
                throw new ServiceException("非法阀位遥测: " + property);
            }
            boolean auxiliary = !separate && valve != 1;
            // Same configured sentinel/tolerance as ValveControlReplyService; never turn it into
            // zero.
            boolean unconfigured =
                    Math.abs(angle.doubleValue() - unconfiguredChannelSentinel) < 0.01;
            if (!Double.isFinite(angle.doubleValue())
                    || !auxiliary
                            && !unconfigured
                            && (angle.signum() < 0 || angle.compareTo(BigDecimal.valueOf(360)) > 0))
                throw new ServiceException("非法阀位遥测: " + property);
            String key =
                    "valve-telemetry:"
                            + event.deviceId()
                            + ":"
                            + owner.tenant()
                            + ":"
                            + owner.version()
                            + ":"
                            + valve;
            var previous = state(key);
            if (!newer(previous, time, event.version())) continue;
            Map<String, Object> observation = new LinkedHashMap<>();
            observation.put("rawAngle", angle);
            observation.put(
                    "quality",
                    auxiliary ? "AUXILIARY" : unconfigured ? "UNCONFIGURED" : "OBSERVED");
            observation.put("valveType", type);
            observation.put("assignmentVersion", owner.version());
            observation.put("eventId", event.eventId());
            // Legacy sessions describe a correlated control action, never an unsolicited
            // startup/field snapshot.
            // COMMAND_CHANGED targetReached alone supplies the request/log and integer
            // targetPosition.
            state(key, time, event.version(), observation);
        }
    }

    private ValveType valveType(String device, Map<String, Object> facts) {
        String code = text(facts, "valveType");
        return code == null
                ? JetLinksValveMetadata.configured(core(device))
                : JetLinksValveMetadata.explicit(code);
    }

    private void valvePosition(
            String device,
            Owner owner,
            int valve,
            int position,
            long time,
            long version,
            ValveType type,
            Long logId,
            Long operator) {
        String key = "valve-position:" + device + ":" + owner.tenant() + ":" + valve;
        var previous = state(key);
        var payload = map(previous.get("payload_json"));
        if (!newer(previous, time, version)) return;
        boolean unchanged = Objects.equals(payload.get("position"), position);
        var open =
                projectionMapper.lockOpenValveSessions(
                        JetLinksMapping.id(device), owner.tenant(), valve);
        boolean closed = type.isClosedPosition(position);
        if (unchanged) {
            if (logId != null && !closed && !open.isEmpty())
                projectionMapper.attachOpenControl(logId, operator, open.get(0).get("id"));
            state(key, time, version, Map.of("position", position));
            return;
        }
        for (var session : open) {
            Object openTime = session.get("open_time");
            // MySQL DATETIME may be LocalDateTime; use the same local-time semantics as Timestamp
            // writes.
            long from =
                    openTime instanceof java.time.LocalDateTime local
                            ? Timestamp.valueOf(local).getTime()
                            : ((Date) openTime).getTime();
            projectionMapper.closeValveSession(
                    closed ? "CLOSED" : "ABORTED",
                    new Timestamp(time),
                    (int) Math.min(Integer.MAX_VALUE, Math.max(0, (time - from) / 1000)),
                    position,
                    logId,
                    operator,
                    closed ? null : "阀位变化，原开阀会话结束",
                    new Timestamp(time),
                    session.get("id"));
        }
        if (!closed || open.isEmpty()) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("id", IdWorker.getId());
            fields.put("tenant_id", owner.tenant());
            fields.put("device_id", JetLinksMapping.id(device));
            fields.put("device_code", deviceCode(device));
            fields.put("valve_no", valve);
            fields.put("valve_type", type.getCode());
            fields.put("open_time", new Timestamp(time));
            fields.put("create_time", new Timestamp(time));
            fields.put("update_time", new Timestamp(time));
            fields.put("status", closed ? "CLOSED" : "OPEN");
            if (closed) {
                fields.put("close_position", position);
                fields.put("close_time", new Timestamp(time));
                fields.put("duration_seconds", 0);
                fields.put("close_control_log_id", logId);
                fields.put("close_by", operator);
            } else {
                fields.put("open_position", position);
                fields.put("open_control_log_id", logId);
                fields.put("open_by", operator);
            }
            upsert("iot_motorvalve_valve_session", "id", fields);
        }
        state(key, time, version, Map.of("position", position));
    }

    void device(ChangeEventDto event) {
        String key = "device:" + event.deviceId();
        var previous = state(key);
        if (!newer(previous, event.sourceTime(), event.version())) return;
        if (event.type().contains("ARCHIVED")) {
            projectionMapper.archiveDevice(JetLinksMapping.id(event.deviceId()));
            state(key, event.sourceTime(), event.version(), Map.of("archived", true));
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>(event.data());
        // DEVICE_CHANGED is an authoritative catalog fact. Missing mandatory fields are obtained
        // only from core.
        if (text(data, "deviceCategory") == null
                || text(data, "deviceCode") == null
                || text(data, "deviceName") == null) data.putAll(core(event.deviceId()));
        Map<String, Object> fields = entityColumns(data, IotDevice.class);
        fields.put("device_id", JetLinksMapping.id(event.deviceId()));
        fields.put("tenant_id", null);
        fields.put("device_category", required(data, "deviceCategory"));
        fields.put("device_code", required(data, "deviceCode"));
        fields.put("device_name", required(data, "deviceName"));
        fields.put(
                "del_flag",
                event.type().contains("ARCHIVED") || "2".equals(text(data, "delFlag")) ? "2" : "0");
        fields.putIfAbsent("status", "0");
        fields.putIfAbsent("online_status", "OFFLINE");
        upsert("iot_device", "device_id", fields);
        state(key, event.sourceTime(), event.version(), Map.of());
    }

    Map<String, Object> core(String device) {
        RecordDto row = await(rpc.getDevice().device(device));
        if (row == null || !device.equals(row.id()))
            throw new ServiceException("核心设备档案缺失: " + device);
        return row.data();
    }

    String deviceCode(String device) {
        var rows = projectionMapper.selectDeviceCodes(JetLinksMapping.id(device));
        if (rows.size() == 1 && rows.getFirst() != null) return rows.getFirst();
        return required(core(device), "deviceCode");
    }

    static Map<String, Object> entityColumns(Map<String, Object> data, Class<?> type) {
        Map<String, Object> fields = new LinkedHashMap<>();
        Map<String, Object> beanFields =
                BeanUtil.beanToMap(BeanUtil.toBean(JetLinksMapping.legacyAudit(data), type));
        beanFields.forEach(
                (field, value) -> {
                    if (value != null
                            && !Set.of("params", "searchValue", "version").contains(field))
                        fields.put(
                                snake(field),
                                value instanceof Date d ? new Timestamp(d.getTime()) : value);
                });
        var safe = JetLinksMapping.legacyAudit(data);
        for (String field : JetLinksMapping.AUDIT_LONG_FIELDS)
            if (beanFields.containsKey(field) && data.containsKey(field) && safe.get(field) == null)
                fields.put(snake(field), null);
        return fields;
    }

    private static Long businessOperator(Map<String, Object> data) {
        return "ym-iot".equals(data.get("caller"))
                ? JetLinksMapping.numericOperator(data.get("operatorId"))
                : null;
    }

    void upsert(String table, String key, Map<String, Object> fields) {
        ProjectionRow row = ProjectionRow.of(table, key, fields);
        var tenants = projectionMapper.selectTenantForUpdate(row);
        if (!tenants.isEmpty()) {
            if (!Set.of("iot_device", "iot_product").contains(table)
                    && !Objects.equals(tenants.get(0), fields.get("tenant_id")))
                throw new ServiceException("历史业务记录租户不匹配");
            var updates =
                    fields.keySet().stream()
                            .filter(
                                    k ->
                                            !k.equals(key)
                                                    && !Set.of("tenant_id", "device_id").contains(k)
                                                    && !("iot_device".equals(table)
                                                            && "online_status".equals(k))
                                                    && (fields.get(k) != null
                                                            || Set.of("iot_device", "iot_product")
                                                                    .contains(table)))
                            .toList();
            if (updates.isEmpty()) return;
            Map<String, Object> changes = new LinkedHashMap<>();
            updates.forEach(column -> changes.put(column, fields.get(column)));
            projectionMapper.updateRow(row.updating(changes));
        } else {
            projectionMapper.insertRow(row);
        }
    }

    static Map<String, Object> protocol(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>(source);
        for (int i = 0; i < 5; i++) {
            Map<String, Object> nested = new LinkedHashMap<>();
            if (result.get("output") instanceof Map<?, ?>)
                nested.putAll(map(result.remove("output")));
            if (result.get("result") instanceof Map<?, ?>)
                nested.putAll(map(result.remove("result")));
            if (nested.isEmpty()) break;
            result.putAll(nested);
        }
        return result;
    }

    static Map<String, Object> map(Object value) {
        if (value == null) return new LinkedHashMap<>();
        if (value instanceof String s) return JSON.parseObject(s);
        if (!(value instanceof Map<?, ?> source)) throw new ServiceException("事件对象格式错误");
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((k, v) -> result.put(k.toString(), v));
        return result;
    }

    static long number(Object value) {
        if (value instanceof Date d) return d.getTime();
        if (value == null) throw new ServiceException("事件数值字段缺失");
        try {
            return new BigDecimal(value.toString()).longValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new ServiceException("事件数值字段无效");
        }
    }

    static String required(Map<String, Object> data, String field) {
        String value = text(data, field);
        if (value == null) throw new ServiceException("事件缺少字段: " + field);
        return value;
    }

    static String text(Map<String, Object> data, String field) {
        Object value = data.get(field);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private static String snake(String field) {
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
