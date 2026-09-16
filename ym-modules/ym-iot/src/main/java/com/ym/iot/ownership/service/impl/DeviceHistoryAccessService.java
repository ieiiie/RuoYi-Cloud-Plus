package com.ym.iot.ownership.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.await;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.device.domain.bo.IotDeviceBo;
import com.ym.iot.device.domain.vo.IotDeviceHistoryVo;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.ownership.domain.dto.HistoryOwnershipRow;
import com.ym.iot.ownership.mapper.DeviceHistoryMapper;
import com.ym.iot.ownership.service.IDeviceAccessService;
import com.ym.iot.ownership.support.OwnershipActor;
import com.ym.jetlinks.rpc.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

/** 历史读取与当前操作分开授权；每次请求重新读取归属，禁止跨租户复用查询资格。 */
@Service
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class DeviceHistoryAccessService {
    private final DeviceHistoryMapper mapper;
    private final OwnershipActor actor;
    private final IDeviceAccessService access;
    private final JetLinksAccess currentAccess;
    private final JetLinksRpcClient rpc;

    public record Grant(
            Long deviceId, String tenant, List<HistoryOwnershipRow> intervals, long asOf) {
        public long lastVisibleTime() {
            var last = intervals.getLast();
            return Math.min(
                    asOf,
                    last.effectiveTo() == null ? asOf : last.effectiveTo().toEpochMilli() - 1);
        }

        public TelemetryReadScope scope() {
            return new TelemetryReadScope(
                    deviceId.toString(),
                    intervals.stream()
                            .filter(
                                    r ->
                                            r.effectiveFrom().toEpochMilli() <= asOf
                                                    && (r.effectiveTo() == null
                                                            || r.effectiveTo().toEpochMilli() > 0))
                            .map(
                                    r ->
                                            new TelemetryTimeWindow(
                                                    Math.max(0, r.effectiveFrom().toEpochMilli()),
                                                    Math.min(
                                                            asOf + 1,
                                                            r.effectiveTo() == null
                                                                    ? asOf + 1
                                                                    : r.effectiveTo()
                                                                            .toEpochMilli())))
                            .toList());
        }

        public boolean contains(long time) {
            return time >= 0
                    && time <= asOf
                    && intervals.stream()
                            .anyMatch(
                                    r ->
                                            time >= r.effectiveFrom().toEpochMilli()
                                                    && (r.effectiveTo() == null
                                                            || time
                                                                    < r.effectiveTo()
                                                                            .toEpochMilli()));
        }
    }

    public Grant capture(Long id) {
        DeviceAccessServiceImpl.validateId(id);
        String tenant = actor.tenantId();
        List<HistoryOwnershipRow> all = mapper.intervals(id);
        validate(all);
        List<HistoryOwnershipRow> own =
                all.stream().filter(r -> tenant.equals(r.tenantId())).toList();
        if (own.isEmpty()) throw new ServiceException("设备没有属于当前租户的历史记录");
        return new Grant(id, tenant, own, System.currentTimeMillis());
    }

    /** 检查所有租户区间，防止历史表异常导致同一时刻同时授权给两家租户。 */
    static void validate(List<HistoryOwnershipRow> rows) {
        HistoryOwnershipRow previous = null;
        for (var row : rows) {
            if (row.effectiveFrom() == null
                    || row.assignmentVersion() == null
                    || row.effectiveTo() != null && !row.effectiveTo().isAfter(row.effectiveFrom())
                    || previous != null
                            && (previous.effectiveTo() == null
                                    || previous.effectiveTo().isAfter(row.effectiveFrom())))
                throw new ServiceException("设备归属历史区间异常，暂不可查询");
            previous = row;
        }
    }

    public void recheck(Grant grant) {
        Grant latest = capture(grant.deviceId());
        if (!grant.tenant().equals(latest.tenant())
                || !grant.intervals().equals(latest.intervals()))
            throw new ServiceException("设备归属已变化，请重新查询历史数据");
    }

    public List<RecordDto> properties(Grant grant, long time) {
        var row =
                grant.intervals().stream()
                        .filter(
                                r ->
                                        time >= r.effectiveFrom().toEpochMilli()
                                                && (r.effectiveTo() == null
                                                        || time < r.effectiveTo().toEpochMilli()))
                        .findFirst()
                        .orElse(null);
        if (row == null) return List.of();
        if (row.effectiveTo() != null) return snapshotProperties(row.metadataSnapshot());
        // 只有仍属于本租户的开放区间可以读取当前物模型，转出后的查询绝不走当前设备详情。
        var current = currentAccess.snapshot(grant.deviceId());
        RecordDto device = await(rpc.getDevice().device(grant.deviceId().toString()));
        if (device == null) throw new ServiceException("设备不存在");
        Object product = device.data().get("productId");
        List<RecordDto> result =
                product == null
                        ? List.of()
                        : await(rpc.getCatalog().properties(product.toString()));
        currentAccess.requireVersion(grant.deviceId(), current.getAssignmentVersion());
        return result == null ? List.of() : result;
    }

    static List<RecordDto> snapshotProperties(String json) {
        Map<String, Object> map = metadata(json);
        if (!(map.get("properties") instanceof Collection<?> rows)) return List.of();
        List<RecordDto> out = new ArrayList<>();
        for (Object item : rows) {
            Map<String, Object> row = JSON.parseObject(JSON.toJSONString(item));
            Map<String, Object> data =
                    row.get("data") instanceof Map<?, ?> raw
                            ? JSON.parseObject(JSON.toJSONString(raw))
                            : row;
            Map<String, Object> safe = new LinkedHashMap<>();
            for (String key :
                    List.of(
                            "identifier",
                            "metricCode",
                            "name",
                            "unit",
                            "dataType",
                            "valueType",
                            "type")) if (data.get(key) != null) safe.put(key, data.get(key));
            String id =
                    Objects.toString(safe.get("identifier"), Objects.toString(row.get("id"), null));
            if (id != null) out.add(new RecordDto(id, safe, 0L));
        }
        return out;
    }

    static Map<String, Object> metadata(String json) {
        return json == null || json.isBlank() ? Map.of() : JSON.parseObject(json);
    }

    public IotDeviceHistoryVo detail(Long id) {
        Grant grant = capture(id);
        var last = grant.intervals().getLast();
        var owner = access.currentOwnership(id);
        boolean current = owner != null && grant.tenant().equals(owner.getTenantId());
        Map<String, Object> source;
        if (current && "ACTIVE".equals(owner.getFenceStatus())) {
            var device = await(rpc.getDevice().device(id.toString()));
            if (device == null) throw new ServiceException("设备不存在");
            source = device.data();
            currentAccess.requireVersion(id, owner.getAssignmentVersion());
        } else source = metadata(last.metadataSnapshot());
        // 始终逐字段选取；历史视图不含新租户、配置、密钥、位置或在线状态。
        IotDeviceHistoryVo vo = new IotDeviceHistoryVo();
        vo.setDeviceId(id);
        vo.setDeviceName(Objects.toString(source.get("deviceName"), id.toString()));
        vo.setDeviceCode(Objects.toString(source.get("deviceCode"), id.toString()));
        if (source.get("productId") != null)
            vo.setProductId(Long.valueOf(source.get("productId").toString()));
        vo.setProductName(Objects.toString(source.get("productName"), null));
        vo.setProductKey(Objects.toString(source.get("productKey"), null));
        vo.setDeviceCategory(Objects.toString(source.get("deviceCategory"), null));
        vo.setOwnershipStatus(current ? "CURRENT" : "TRANSFERRED");
        vo.setCanOperate(current && "ACTIVE".equals(owner.getFenceStatus()));
        vo.setCanViewHistory(true);
        vo.setOwnershipPeriods(
                grant.intervals().stream()
                        .map(
                                r ->
                                        new IotDeviceHistoryVo.Period(
                                                Math.max(0, r.effectiveFrom().toEpochMilli()),
                                                r.effectiveTo() == null
                                                        ? null
                                                        : r.effectiveTo().toEpochMilli()))
                        .toList());
        vo.setProperties(
                current && !vo.isCanOperate()
                        ? List.of()
                        : properties(grant, grant.lastVisibleTime()));
        recheck(grant);
        return vo;
    }

    public PageResult<IotDeviceHistoryVo> visible(
            IotDeviceBo bo, PageQuery request, String status) {
        String tenant = actor.tenantId();
        String filter = status == null ? "ALL" : status;
        if (!Set.of("ALL", "CURRENT", "TRANSFERRED").contains(filter))
            throw new ServiceException("非法归属筛选");
        long number =
                request == null || request.getPageNum() == null
                        ? 1
                        : Math.max(1, request.getPageNum());
        long size =
                request == null || request.getPageSize() == null
                        ? 20
                        : Math.max(1, Math.min(500, request.getPageSize()));
        var page = mapper.visible(new Page<>(number, size), tenant, bo, filter);
        if (page.getCurrent() != number) return PageResult.build(List.of(), page.getTotal());
        List<IotDeviceHistoryVo> rows = new ArrayList<>();
        for (var row : page.getRecords()) {
            var grant = capture(row.getDeviceId());
            var owner = access.currentOwnership(row.getDeviceId());
            if (owner == null
                    || !tenant.equals(owner.getTenantId())
                    || !"CURRENT".equals(row.getOwnershipStatus())) {
                rows.add(detail(row.getDeviceId()));
                continue;
            }
            // 列表使用本地目录的白名单列，不为每一台设备分别调用产品物模型 RPC。
            row.setCanOperate("ACTIVE".equals(owner.getFenceStatus()));
            row.setCanViewHistory(true);
            row.setOwnershipPeriods(
                    grant.intervals().stream()
                            .map(
                                    r ->
                                            new IotDeviceHistoryVo.Period(
                                                    Math.max(0, r.effectiveFrom().toEpochMilli()),
                                                    r.effectiveTo() == null
                                                            ? null
                                                            : r.effectiveTo().toEpochMilli()))
                            .toList());
            row.setProperties(List.of());
            recheck(grant);
            rows.add(row);
        }
        return PageResult.build(rows, page.getTotal());
    }
}
