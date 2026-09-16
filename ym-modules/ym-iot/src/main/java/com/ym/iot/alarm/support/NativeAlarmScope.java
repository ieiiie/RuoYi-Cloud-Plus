package com.ym.iot.alarm.support;

import com.alibaba.fastjson2.JSON;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.ownership.domain.dto.HistoryOwnershipRow;
import com.ym.iot.ownership.mapper.DeviceOwnershipMapper;
import com.ym.iot.ownership.service.impl.DeviceHistoryAccessService;
import com.ym.iot.ownership.service.impl.DeviceHistoryAccessService.Grant;
import com.ym.iot.ownership.support.OwnershipActor;
import com.ym.jetlinks.rpc.TelemetryTimeWindow;

import org.springframework.stereotype.Component;

import java.util.*;

/** 告警只借用归属历史进行授权；不保存告警内容，也不读取转出设备的当前技术档案。 */
@Component
@ConditionalOnJetLinks
public class NativeAlarmScope {
    private final DeviceOwnershipMapper ownershipMapper;
    private final OwnershipActor actor;
    private final JetLinksAccess access;
    private final DeviceHistoryAccessService history;

    public NativeAlarmScope(
            DeviceOwnershipMapper ownershipMapper,
            OwnershipActor actor,
            JetLinksAccess access,
            DeviceHistoryAccessService history) {
        this.ownershipMapper = ownershipMapper;
        this.actor = actor;
        this.access = access;
        this.history = history;
    }

    public String key() {
        return "business:" + actor.tenantId();
    }

    public Map<String, Object> current() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scopeKey", key());
        result.put("authorizedDeviceIds", access.ids());
        return result;
    }

    /** 本次调用独享的授权凭据；窗口不可变，返回校验不能重新生成更晚的 asOf 窗口。 */
    public record HistoricalScope(
            String tenant, Map<String, Grant> grants,
            Map<String, List<TelemetryTimeWindow>> windows) {
        public HistoricalScope {
            grants = Collections.unmodifiableMap(new LinkedHashMap<>(grants));
            Map<String, List<TelemetryTimeWindow>> copy = new LinkedHashMap<>();
            windows.forEach((id, periods) -> copy.put(id, List.copyOf(periods)));
            windows = Collections.unmodifiableMap(copy);
        }

        /** 每次返回独立的 RPC 参数副本；远程参数与本地校验凭据不共享可变集合。 */
        public Map<String, Object> filters() {
            Map<String, List<Map<String, Object>>> visible = new LinkedHashMap<>();
            windows.forEach((id, periods) -> {
                List<Map<String, Object>> values = new ArrayList<>();
                for (var period : periods) {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("from", period.from());
                    value.put("to", period.to());
                    values.add(value);
                }
                visible.put(id, values);
            });
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("authorizedDeviceIds", List.copyOf(windows.keySet()));
            result.put("visibilityWindows", visible);
            return result;
        }
    }

    public Map<String, Object> historical() {
        return captureHistorical(null).filters();
    }

    /**
     * 产品筛选按每个归属区间执行，同一设备再次归属本租户也不合并中间空档。
     * 关闭区间仅认当时保存的 productId；缺失/损坏快照保守不匹配。
     */
    public HistoricalScope captureHistorical(String productId) {
        String tenant = actor.tenantId();
        long asOf = System.currentTimeMillis();
        Map<String, Grant> grants = new LinkedHashMap<>();
        Map<String, List<TelemetryTimeWindow>> windows = new LinkedHashMap<>();
        for (Long id : ownershipMapper.selectTenantIntervals(tenant).stream()
                .map(r -> r.deviceId()).distinct().toList()) {
            Grant captured = history.capture(id);
            if (!tenant.equals(captured.tenant())) throw changed();
            // 所有设备共享请求开始时的上界；capture 自带的上界若更早，只能继续收窄。
            Grant grant = new Grant(id, tenant, captured.intervals(), Math.min(asOf, captured.asOf()));
            List<TelemetryTimeWindow> periods = new ArrayList<>();
            for (var interval : grant.intervals()) {
                long from = Math.max(0, interval.effectiveFrom().toEpochMilli());
                long to = Math.min(grant.asOf() + 1, interval.effectiveTo() == null
                        ? grant.asOf() + 1 : interval.effectiveTo().toEpochMilli());
                if (from < to && (productId == null || matchesProduct(grant, interval, productId)))
                    periods.add(new TelemetryTimeWindow(from, to));
            }
            // 未匹配设备也重验，避免产品筛选期间归属变化后仍返回旧的统计总数。
            grants.put(id.toString(), grant);
            if (!periods.isEmpty()) windows.put(id.toString(), periods);
        }
        HistoricalScope result = new HistoricalScope(tenant, grants, windows);
        recheck(result);
        return result;
    }

    private boolean matchesProduct(Grant grant, HistoryOwnershipRow interval, String productId) {
        if (interval.effectiveTo() != null) {
            String snapshot = interval.metadataSnapshot();
            if (snapshot == null || snapshot.isBlank()) return false;
            try {
                Map<String, Object> metadata = JSON.parseObject(snapshot);
                return metadata != null && productId.equals(metadata.get("productId"));
            } catch (RuntimeException invalidSnapshot) {
                // 不记录完整快照或解析器消息，避免历史内容进入日志。
                return false;
            }
        }
        // 只有本租户开放区间允许安全详情读取；详情服务内部校验当前归属版本和展示白名单。
        history.recheck(grant);
        var detail = history.detail(grant.deviceId());
        history.recheck(grant);
        return detail != null && "CURRENT".equals(detail.getOwnershipStatus())
                && productId.equals(Objects.toString(detail.getProductId(), null));
    }

    /** 先验证最初的请求窗口；两次发生/处理时间分别可见，不代表它们可以跨区间组合返回。 */
    public void requireSameWindow(HistoricalScope captured, String deviceId, long... times) {
        Grant grant = captured.grants().get(deviceId);
        List<TelemetryTimeWindow> windows = captured.windows().get(deviceId);
        if (grant == null || windows == null || times.length == 0)
            throw new ServiceException("告警返回超出本次请求的设备范围");
        for (long time : times) {
            if (!grant.contains(time)) throw new ServiceException("告警返回超出本次请求的归属时间范围");
        }
        for (var window : windows) {
            boolean containsAll = true;
            for (long time : times) containsAll &= time >= window.from() && time < window.to();
            if (containsAll) return;
        }
        throw new ServiceException("告警发生或处理时间跨越归属区间，拒绝返回");
    }

    /** RPC 返回后再次读取归属，只判断原凭据是否仍有效，绝不扩展原请求时间上界。 */
    public void recheck(HistoricalScope captured) {
        if (!captured.tenant().equals(actor.tenantId())) throw changed();
        for (Grant grant : captured.grants().values()) history.recheck(grant);
    }

    public boolean visible(String deviceId, long occurredAt) {
        return history.capture(Long.valueOf(deviceId)).contains(occurredAt);
    }

    private static ServiceException changed() {
        return new ServiceException("设备归属已变化，请重新查询告警历史");
    }
}
