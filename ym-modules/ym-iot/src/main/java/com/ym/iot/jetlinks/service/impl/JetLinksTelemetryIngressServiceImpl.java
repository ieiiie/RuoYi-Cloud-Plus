package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.*;

import com.ym.common.core.exception.ServiceException;
import com.ym.iot.device.domain.IotDataPoint;
import com.ym.iot.device.domain.IotDevice;
import com.ym.iot.device.domain.bo.IotTelemetryIngressBo;
import com.ym.iot.device.domain.dto.IotTelemetryIngestResult;
import com.ym.iot.device.domain.dto.IotTelemetryIngressContext;
import com.ym.iot.device.telemetry.IotTelemetryAdapter;
import com.ym.iot.device.telemetry.IotTelemetryAdapterRegistry;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksDeviceService;
import com.ym.iot.jetlinks.service.IJetLinksTelemetryIngressService;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.jetlinks.support.JetLinksMapping;
import com.ym.jetlinks.rpc.*;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Primary
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksTelemetryIngressServiceImpl implements IJetLinksTelemetryIngressService {
    private final JetLinksRpcClient rpc;
    private final JetLinksAccess access;
    private final IJetLinksDeviceService devices;
    private final IotTelemetryAdapterRegistry adapters;

    @Override
    public IotTelemetryIngestResult ingestWithResult(IotTelemetryIngressBo bo) {
        if (bo == null || !bo.isDeviceTargetValid())
            throw new ServiceException("deviceId 与 deviceCode 必须且只能填一个");
        Long id =
                bo.getDeviceId() != null
                        ? bo.getDeviceId()
                        : devices.requireCode(bo.getDeviceCode());
        return access.write(
                id,
                () -> {
                    IotDevice device =
                            JetLinksMapping.bean(devices.get(id), "deviceId", IotDevice.class);
                    if (device == null || !"0".equals(device.getStatus()))
                        throw new ServiceException("设备不存在或已停用");
                    var ownership = access.snapshot(id);
                    device.setTenantId(ownership.getTenantId());
                    IotTelemetryAdapter adapter =
                            adapters.get(bo.getAdapterKey())
                                    .orElseThrow(() -> new ServiceException("未注册的遥测适配器"));
                    Date fallback =
                            bo.getDefaultCollectTime() == null
                                    ? new Date()
                                    : bo.getDefaultCollectTime();
                    List<IotDataPoint> points =
                            adapter.adapt(
                                    IotTelemetryIngressContext.builder()
                                            .tenantId(ownership.getTenantId())
                                            .device(device)
                                            .adapterKey(bo.getAdapterKey())
                                            .payload(bo.getPayload())
                                            .defaultCollectTime(fallback)
                                            .build());
                    if (points == null || points.isEmpty()) return IotTelemetryIngestResult.empty();
                    List<RecordDto> properties =
                            device.getProductId() == null
                                    ? List.of()
                                    : await(
                                            rpc.getCatalog()
                                                    .properties(device.getProductId().toString()));
                    if (properties == null) throw new ServiceException("核心未返回物模型属性");
                    Map<Long, Map<String, Object>> byTime = new LinkedHashMap<>();
                    for (IotDataPoint point : points) {
                        Date time =
                                point.getCollectTime() == null ? fallback : point.getCollectTime();
                        if (time.toInstant().isBefore(ownership.getEffectiveFrom()))
                            throw new ServiceException("拒绝写入当前归属期之前的遥测");
                        if (point.getMetricCode() == null || point.getMetricCode().isBlank())
                            throw new ServiceException("遥测指标编码不能为空");
                        String identifier =
                                point.getPropertyIdentifier() == null
                                                || point.getPropertyIdentifier().isBlank()
                                        ? point.getMetricCode()
                                        : point.getPropertyIdentifier();
                        identifier =
                                JetLinksTelemetryServiceImpl.identifier(identifier, properties);
                        String propertyId = identifier;
                        if (!properties.isEmpty()
                                && properties.stream()
                                        .noneMatch(
                                                p ->
                                                        propertyId.equals(
                                                                Objects.toString(
                                                                        p.data().get("identifier"),
                                                                        null)))) continue;
                        Object value =
                                point.getTypedValue() != null
                                        ? point.getTypedValue()
                                        : point.getMetricValue() != null
                                                ? point.getMetricValue()
                                                : point.getValueText();
                        if (value != null)
                            byTime.computeIfAbsent(time.getTime(), t -> new LinkedHashMap<>())
                                    .put(identifier, value);
                    }
                    long receivedAt = System.currentTimeMillis();
                    List<TelemetrySample> samples =
                            byTime.entrySet().stream()
                                    .map(
                                            e ->
                                                    new TelemetrySample(
                                                            id.toString(),
                                                            null,
                                                            e.getKey(),
                                                            receivedAt,
                                                            e.getValue()))
                                    .toList();
                    Integer written =
                            await(rpc.getTelemetry().ingest(access.context(rpc, id), samples));
                    if (written == null) throw new ServiceException("JetLinks未返回写入结果");
                    return new IotTelemetryIngestResult(
                            written,
                            byTime.isEmpty() ? null : new Date(Collections.max(byTime.keySet())));
                });
    }
}
