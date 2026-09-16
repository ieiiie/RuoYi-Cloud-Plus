package com.ym.iot.jetlinks.service.impl;

import cn.hutool.core.bean.BeanUtil;

import com.alibaba.fastjson2.JSON;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.fertilizer.domain.IotFertilizerControlLog;
import com.ym.iot.fertilizer.domain.IotFertilizerRecord;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.domain.dto.ProjectionRow;
import com.ym.iot.jetlinks.mapper.JetLinksCatalogMapper;
import com.ym.iot.jetlinks.mapper.JetLinksEventMapper;
import com.ym.iot.jetlinks.mapper.JetLinksProjectionMapper;
import com.ym.iot.jetlinks.support.JetLinksMapping;
import com.ym.iot.motorvalve.domain.ValveControlLog;
import com.ym.iot.motorvalve.domain.ValveSession;
import com.ym.jetlinks.rpc.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Date;
import java.util.Locale;

/** 每个事件单独提交；回执和业务投影同进退，RPC 返回前已经提交。 */
@Service
@ConditionalOnJetLinks
public class JetLinksEventProjectionService {
    private final JetLinksEventMapper eventMapper;
    private final JetLinksBusinessProjection projection;
    private final JetLinksCatalogAlarmProjection mirrors;
    private final org.springframework.context.ApplicationEventPublisher publisher;

    public JetLinksEventProjectionService(
            JetLinksRpcClient rpc,
            JetLinksEventMapper eventMapper,
            JetLinksProjectionMapper projectionMapper,
            JetLinksCatalogMapper catalogMapper,
            org.springframework.context.ApplicationEventPublisher publisher,
            @org.springframework.beans.factory.annotation.Value(
                            "${ym.iot.motorvalve.unconfigured-channel-sentinel:888.0}")
                    double sentinel) {
        this.eventMapper = eventMapper;
        this.projection = new JetLinksBusinessProjection(projectionMapper, rpc, sentinel);
        this.mirrors = new JetLinksCatalogAlarmProjection(catalogMapper, rpc, projection);
        this.publisher = publisher;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            timeout = 30,
            rollbackFor = Exception.class)
    public void replay(ChangeEventDto event, String operator) {
        apply(event);
        eventMapper.markReplayed(operator, event.eventId());
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            timeout = 30,
            rollbackFor = Exception.class)
    public void apply(ChangeEventDto event) {
        if (event == null || event.eventId() == null)
            throw new ServiceException("eventId required");
        // 遥测不逐条保存事件回执和完整载荷，避免普通采集持续膨胀回执表。
        // 阀位投影仍在本事务内执行，按设备/租户/归属版本/通道锁定状态，
        // 通过源时间和版本拒绝重复、过期数据；普通传感器遥测直接返回。
        if ("TELEMETRY_REPORTED".equals(event.type())) {
            projection.telemetry(event);
            return;
        }
        if (eventMapper.insertReceipt(
                        event.eventId(),
                        event.type(),
                        event.deviceId(),
                        event.sourceTime(),
                        JSON.toJSONString(event.data()))
                == 0) {
            // 投影已提交但推进下一步/确认回执失败时，重投仍需唤醒持久任务。
            notifyTask(event);
            return;
        }
        String table;
        String key;
        Class<?> type;
        switch (event.type()) {
            case "fertilizer.record" -> {
                if (!event.data().containsKey("recordId")) {
                    projection.fertilizer(event);
                    return;
                }
                table = "iot_fertilizer_record";
                key = "record_id";
                type = IotFertilizerRecord.class;
            }
            case "valve.session" -> {
                table = "iot_motorvalve_valve_session";
                key = "id";
                type = ValveSession.class;
            }
            case "valve.control.log" -> {
                table = "iot_motorvalve_control_log";
                key = "id";
                type = ValveControlLog.class;
            }
            case "fertilizer.control.log" -> {
                table = "iot_fertilizer_control_log";
                key = "log_id";
                type = IotFertilizerControlLog.class;
            }
            case "COMMAND_CHANGED", "command.changed" -> {
                projection.command(event);
                notifyTask(event);
                return;
            }
            case "DEVICE_CHANGED", "DEVICE_ARCHIVED" -> {
                projection.device(event);
                return;
            }
            case "DEVICE_STATUS_CHANGED" -> {
                mirrors.status(event);
                return;
            }
            case "PRODUCT_CHANGED", "PRODUCT_ARCHIVED" -> {
                mirrors.product(event);
                return;
            }
            case "CATEGORY_CHANGED", "CATEGORY_ARCHIVED" -> {
                mirrors.category(event);
                return;
            }
            // JetLinks owns alarm records and notifications. Keep the durable receipt for
            // native lifecycle facts and old queued facts; never recreate mirrors or resend
            // notices.
            case "ALARM_CHANGED",
                    "ALARM_CREATED",
                    "ALARM_RULE_CHANGED",
                    "ALARM_RULE_ARCHIVED",
                    "NATIVE_ALARM_OCCURRED",
                    "NATIVE_ALARM_RECOVERED",
                    "NATIVE_ALARM_HANDLED" -> {
                return;
            }
            default ->
                    throw new ServiceException(
                            "Unknown JetLinks event type; not acknowledged: " + event.type());
        }
        Map<String, Object> data = new LinkedHashMap<>(event.data());
        String tenant = tenantAtSource(event);
        data.put("tenantId", tenant);
        data.put("deviceId", JetLinksMapping.id(event.deviceId()));
        Object bean = BeanUtil.toBean(JetLinksMapping.legacyAudit(data), type);
        Map<String, Object> fields = BeanUtil.beanToMap(bean);
        Map<String, Object> columns = new LinkedHashMap<>();
        fields.forEach(
                (field, value) -> {
                    if (value != null && !Set.of("params", "searchValue").contains(field))
                        columns.put(
                                snake(field),
                                value instanceof Date date
                                        ? new java.sql.Timestamp(date.getTime())
                                        : value);
                });
        if (!columns.containsKey(key))
            throw new ServiceException("business event missing stable record ID");
        // Never move an existing historical record to the currently assigned tenant.
        ProjectionRow row = ProjectionRow.of(table, key, columns);
        var existing = eventMapper.selectTenantForUpdate(row);
        if (!existing.isEmpty() && !tenant.equals(existing.get(0)))
            throw new ServiceException("historical record tenant mismatch");
        eventMapper.upsertHistory(row);
    }

    private void notifyTask(ChangeEventDto event) {
        if (!Set.of("COMMAND_CHANGED", "command.changed").contains(event.type())) return;
        String request = Objects.toString(event.data().get("requestId"), "");
        int colon = request.lastIndexOf(':');
        if (colon > 0)
            publisher.publishEvent(
                    new com.ym.iot.jetlinks.domain.dto.FertilizerTaskReady(
                            request.substring(0, colon)));
    }

    private String tenantAtSource(ChangeEventDto event) {
        Object version = event.data().get("assignmentVersion");
        if (version == null)
            throw new ServiceException("business event requires captured assignmentVersion");
        var tenants =
                eventMapper.selectSourceTenants(
                        JetLinksMapping.id(event.deviceId()),
                        Long.valueOf(version.toString()),
                        new java.sql.Timestamp(event.sourceTime()),
                        new java.sql.Timestamp(event.sourceTime()));
        if (tenants.size() != 1 || tenants.get(0) == null)
            throw new ServiceException(
                    "business event has no authoritative assigned ownership interval");
        return tenants.get(0);
    }

    private static String snake(String field) {
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
