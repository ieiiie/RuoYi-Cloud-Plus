package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.*;

import com.alibaba.fastjson2.JSON;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.device.domain.IotDataPoint;
import com.ym.iot.device.domain.vo.IotDataPointRecordVo;
import com.ym.iot.device.domain.vo.IotLatestVo;
import com.ym.iot.device.domain.vo.IotPestChartVo;
import com.ym.iot.device.domain.vo.IotPestNightChartVo;
import com.ym.iot.device.domain.vo.IotSeriesBatchVo;
import com.ym.iot.device.domain.vo.IotSeriesVo;
import com.ym.iot.device.telemetry.adapter.HfzkPestItems;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksDeviceService;
import com.ym.iot.jetlinks.service.IJetLinksTelemetryService;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.jetlinks.support.JetLinksMapping;
import com.ym.iot.jetlinks.support.JetLinksPestCharts;
import com.ym.iot.ownership.service.impl.DeviceHistoryAccessService;
import com.ym.iot.ownership.service.impl.DeviceHistoryAccessService.Grant;
import com.ym.jetlinks.rpc.*;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@Primary
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksTelemetryServiceImpl implements IJetLinksTelemetryService {
    private final JetLinksRpcClient rpc;
    private final JetLinksAccess access;
    private final IJetLinksDeviceService devices;
    private final DeviceHistoryAccessService history;

    @Override
    public IotLatestVo getLatest(Long id) {
        return getLatestMap(List.of(id)).get(id);
    }

    @Override
    public Map<Long, IotLatestVo> getLatestMap(Collection<Long> ids) {
        return latest(ids, false);
    }

    @Override
    public Map<Long, IotLatestVo> getLatestMapFresh(Collection<Long> ids) {
        return latest(ids, true);
    }

    @Override
    public Map<Long, IotLatestVo> getLatestMapCached(Collection<Long> ids) {
        return latest(ids, false);
    }

    @Override
    public Map<Long, IotLatestVo> warmupLatestMap(Collection<Long> ids) {
        return latest(ids, false);
    }

    private Map<Long, IotLatestVo> latest(Collection<Long> ids, boolean bypassCache) {
        if (ids == null || ids.isEmpty()) return Map.of();
        var snapshots = access.capture(ids);
        Map<String, Long> visible = access.visibleFrom(ids);
        List<TelemetryLatest> values =
                await(rpc.getTelemetry().latest(JetLinksMapping.ids(ids), bypassCache, visible));
        if (values == null) throw new ServiceException("JetLinks returned null telemetry");
        Map<Long, IotLatestVo> result = new LinkedHashMap<>();
        for (TelemetryLatest value : values) {
            if (!visible.containsKey(value.deviceId()))
                throw new ServiceException("JetLinks returned an unauthorized telemetry device");
            IotLatestVo vo =
                    mapLatest(
                            legacyLatest(value, properties(JetLinksMapping.id(value.deviceId()))),
                            visible.get(value.deviceId()));
            result.put(vo.getDeviceId(), vo);
        }
        access.recheck(snapshots);
        return result;
    }

    /** Old DTO preserves numerics in metrics and all other typed values in texts. */
    static IotLatestVo mapLatest(TelemetryLatest value, long visibleFrom) {
        IotLatestVo vo = new IotLatestVo();
        vo.setDeviceId(JetLinksMapping.id(value.deviceId()));
        vo.setMetrics(new LinkedHashMap<>());
        vo.setTexts(new LinkedHashMap<>());
        vo.setCollectTimes(new LinkedHashMap<>());
        vo.setUnits(new LinkedHashMap<>());
        vo.setNames(new LinkedHashMap<>());
        if (value.properties() == null) return vo;
        value.properties()
                .forEach(
                        (code, v) -> {
                            Long time =
                                    value.collectTimes() == null
                                            ? null
                                            : value.collectTimes().get(code);
                            if (time == null || time < visibleFrom) return;
                            if (v instanceof Number n)
                                vo.getMetrics().put(code, new BigDecimal(n.toString()));
                            else if (v != null)
                                vo.getTexts()
                                        .put(
                                                code,
                                                v instanceof String s ? s : JSON.toJSONString(v));
                            vo.getCollectTimes().put(code, new Date(time));
                            if (value.units() != null && value.units().get(code) != null)
                                vo.getUnits().put(code, value.units().get(code));
                            if (value.names() != null && value.names().get(code) != null)
                                vo.getNames().put(code, value.names().get(code));
                        });
        return vo;
    }

    @Override
    public IotSeriesVo getSeries(Long id, String code, Date from, Date to, Integer step) {
        Grant grant = history.capture(id);
        IotSeriesVo result = series(grant, code, from, to, step);
        history.recheck(grant);
        return result;
    }

    private IotSeriesVo series(Grant grant, String code, Date from, Date to, Integer step) {
        if (code == null || code.isBlank()) throw new ServiceException("指标编码不能为空");
        if (from != null && to != null && from.after(to))
            throw new ServiceException("起始时间不能晚于结束时间");
        long end = to == null ? grant.lastVisibleTime() : Math.min(to.getTime(), grant.asOf());
        long start = from == null ? Math.max(0, end - 86400000L) : Math.max(0, from.getTime());
        IotSeriesVo vo = new IotSeriesVo();
        vo.setMetricCode(code.trim());
        vo.setPoints(List.of());
        if (end < start) return vo;
        List<IotSeriesVo.SeriesPoint> points = new ArrayList<>();
        Set<String> units = new HashSet<>();
        // 每段按自己的历史属性映射查询，先隔离再聚合，不能把 A→B→A 中间的数据带回来。
        for (TelemetryTimeWindow window : grant.scope().windows()) {
            long low = Math.max(start, window.from()), high = Math.min(end, window.to() - 1);
            if (high < low) continue;
            var props = history.properties(grant, low);
            String metric = identifier(code.trim(), props);
            SeriesDto part =
                    await(
                            rpc.getTelemetry()
                                    .seriesScoped(
                                            new TelemetryReadScope(
                                                    grant.deviceId().toString(), List.of(window)),
                                            metric,
                                            low,
                                            high,
                                            step));
            if (part == null || part.points() == null)
                throw new ServiceException("JetLinks未返回历史曲线");
            for (var point : part.points()) {
                if (point.time() < low || point.time() > high || !grant.contains(point.time()))
                    throw new ServiceException("JetLinks返回了授权区间以外的曲线数据");
                points.add(new IotSeriesVo.SeriesPoint(new Date(point.time()), point.value()));
            }
            props.stream()
                    .filter(
                            r ->
                                    code.trim().equals(metric(r))
                                            || metric.equals(r.data().get("identifier")))
                    .map(r -> r.data().get("unit"))
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .forEach(units::add);
        }
        points.sort(Comparator.comparing(IotSeriesVo.SeriesPoint::getTime));
        vo.setPoints(points);
        vo.setUnit(units.size() == 1 ? units.iterator().next() : null);
        return vo;
    }

    @Override
    public IotSeriesBatchVo getSeriesBatch(
            Long id, List<String> codes, Date from, Date to, Integer step) {
        Grant grant = history.capture(id);
        LinkedHashSet<String> requested = new LinkedHashSet<>();
        if (codes != null)
            codes.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(c -> !c.isBlank())
                    .forEach(requested::add);
        if (requested.isEmpty()) {
            for (var interval : grant.scope().windows())
                for (var prop : history.properties(grant, interval.from())) {
                    String code = metric(prop);
                    if (code != null) requested.add(code);
                }
            // 缺少历史物模型快照时仍可查询保存的数据，使用源属性标识，不猜测当前产品映射。
            if (requested.isEmpty()) {
                var rows = samples(grant, 0, grant.asOf(), 1);
                if (!rows.isEmpty() && rows.getFirst().properties() != null)
                    requested.addAll(rows.getFirst().properties().keySet());
            }
        }
        if (requested.size() > 100) throw new ServiceException("查询指标数量不得超过100");
        IotSeriesBatchVo vo = new IotSeriesBatchVo();
        vo.setSeries(requested.stream().map(c -> series(grant, c, from, to, step)).toList());
        history.recheck(grant);
        return vo;
    }

    @Override
    public IotDataPointRecordVo getRecord(Long id, Date time) {
        Grant grant = history.capture(id);
        long from = time == null ? 0 : time.getTime(),
                to = time == null ? grant.asOf() : time.getTime();
        var rows = samples(grant, from, to, 1);
        var result = record(grant, rows.isEmpty() ? null : rows.getFirst());
        history.recheck(grant);
        return result;
    }

    @Override
    public IotDataPointRecordVo getAdjacentRecord(Long id, Date time, boolean previous) {
        if (time == null) throw new ServiceException("当前采集时间不能为空");
        Grant grant = history.capture(id);
        long current = time.getTime();
        TelemetrySample row = null;
        if (previous) {
            var found =
                    current <= 0 ? List.<TelemetrySample>of() : samples(grant, 0, current - 1, 1);
            if (!found.isEmpty()) row = found.getFirst();
        } else if (current < grant.asOf()) {
            long low = Math.max(0, current + 1), high = grant.asOf();
            var found = samples(grant, low, high, 1);
            if (!found.isEmpty()) {
                high = found.getFirst().collectTime();
                while (low < high) {
                    long mid = low + (high - low) / 2;
                    var candidate = samples(grant, low, mid, 1);
                    if (candidate.isEmpty()) low = mid + 1;
                    else high = candidate.getFirst().collectTime();
                }
                var nearest = samples(grant, low, low, 1);
                if (!nearest.isEmpty()) row = nearest.getFirst();
            }
        }
        var result = record(grant, row);
        history.recheck(grant);
        return result;
    }

    @Override
    public Date getAdjacentCollectTime(Long id, Date time, boolean previous) {
        return getAdjacentRecord(id, time, previous).getCollectTime();
    }

    private List<TelemetrySample> samples(Grant grant, long from, long to, int limit) {
        long start = Math.max(0, from), end = Math.min(to, grant.asOf());
        if (start > end
                || grant.scope().windows().stream()
                        .noneMatch(w -> w.from() <= end && w.to() > start)) return List.of();
        var rows = await(rpc.getTelemetry().recordsScoped(grant.scope(), start, end, limit));
        if (rows == null) throw new ServiceException("JetLinks未返回历史采集记录");
        Map<Long, List<RecordDto>> metadata = new HashMap<>();
        List<TelemetrySample> result = new ArrayList<>();
        for (var row : rows) {
            if (!grant.deviceId().toString().equals(row.deviceId())
                    || row.collectTime() < start
                    || row.collectTime() > end
                    || !grant.contains(row.collectTime()))
                throw new ServiceException("JetLinks返回了授权设备或区间以外的采集记录");
            var period =
                    grant.intervals().stream()
                            .filter(
                                    r ->
                                            row.collectTime() >= r.effectiveFrom().toEpochMilli()
                                                    && (r.effectiveTo() == null
                                                            || row.collectTime()
                                                                    < r.effectiveTo()
                                                                            .toEpochMilli()))
                            .findFirst()
                            .orElseThrow();
            var props =
                    metadata.computeIfAbsent(
                            period.assignmentVersion(),
                            v -> history.properties(grant, row.collectTime()));
            result.add(
                    new TelemetrySample(
                            row.deviceId(),
                            row.sourceRecordId(),
                            row.collectTime(),
                            row.receivedAt(),
                            legacyValues(legacyReadValues(row.properties()), props)));
        }
        history.recheck(grant);
        return result.stream()
                .sorted(Comparator.comparingLong(TelemetrySample::collectTime).reversed())
                .limit(limit)
                .toList();
    }

    private IotDataPointRecordVo record(Grant grant, TelemetrySample row) {
        IotDataPointRecordVo vo = new IotDataPointRecordVo();
        vo.setDeviceId(grant.deviceId());
        if (row == null) return vo;
        vo.setCollectTime(new Date(row.collectTime()));
        Map<String, String> units = new HashMap<>();
        for (var property : history.properties(grant, row.collectTime()))
            if (metric(property) != null && property.data().get("unit") != null)
                units.put(metric(property), property.data().get("unit").toString());
        vo.setPoints(
                points(List.of(row)).stream()
                        .map(
                                p -> {
                                    var point = new IotDataPointRecordVo.PointVo();
                                    point.setMetricCode(p.getMetricCode());
                                    point.setValue(p.getMetricValue());
                                    point.setTextValue(p.getValueText());
                                    point.setCollectTime(p.getCollectTime());
                                    point.setUnit(units.get(p.getMetricCode()));
                                    return point;
                                })
                        .toList());
        return vo;
    }

    private List<RecordDto> properties(Long id) {
        var device = devices.queryById(id);
        if (device == null) throw new ServiceException("设备不存在");
        if (device.getProductId() == null) return List.of();
        List<RecordDto> properties =
                await(rpc.getCatalog().properties(device.getProductId().toString()));
        if (properties == null) throw new ServiceException("核心未返回物模型属性");
        return properties;
    }

    static String metric(RecordDto property) {
        Object code = property.data().get("metricCode");
        if (code == null || code.toString().isBlank()) code = property.data().get("identifier");
        return code == null ? null : code.toString();
    }

    static String identifier(String code, List<RecordDto> properties) {
        List<String> matching =
                properties.stream()
                        .filter(p -> code.equals(metric(p)))
                        .map(p -> Objects.toString(p.data().get("identifier"), null))
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (matching.size() > 1) throw new ServiceException("物模型metricCode不唯一: " + code);
        return matching.isEmpty() ? code : matching.get(0);
    }

    static <T> Map<String, T> legacyValues(Map<String, T> values, List<RecordDto> properties) {
        if (values == null) return Map.of();
        Map<String, String> names = new LinkedHashMap<>();
        for (RecordDto property : properties)
            if (property.data().get("identifier") != null && metric(property) != null)
                names.put(property.data().get("identifier").toString(), metric(property));
        Map<String, T> mapped = new LinkedHashMap<>();
        values.forEach(
                (key, value) -> {
                    String code = names.getOrDefault(key, key);
                    if (mapped.containsKey(code)) throw new ServiceException("物模型指标映射冲突: " + code);
                    mapped.put(code, value);
                });
        return mapped;
    }

    static TelemetryLatest legacyLatest(TelemetryLatest row, List<RecordDto> properties) {
        return new TelemetryLatest(
                row.deviceId(),
                legacyValues(legacyReadValues(row.properties()), properties),
                legacyValues(row.collectTimes(), properties),
                legacyValues(row.units(), properties),
                legacyValues(row.names(), properties));
    }

    private static Map<String, Object> legacyReadValues(Map<String, Object> values) {
        if (values == null) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>(values);
        if (values.get("hfzk_bugerList") != null)
            result.put("hfzk_bugerList", HfzkPestItems.normalizeRead(values.get("hfzk_bugerList")));
        return result;
    }

    static List<IotDataPoint> points(List<TelemetrySample> rows) {
        List<IotDataPoint> result = new ArrayList<>();
        for (TelemetrySample row : rows)
            if (row.properties() != null)
                row.properties()
                        .forEach(
                                (code, value) -> {
                                    IotDataPoint point = new IotDataPoint();
                                    point.setDeviceId(JetLinksMapping.id(row.deviceId()));
                                    point.setMetricCode(code);
                                    point.setCollectTime(new Date(row.collectTime()));
                                    if (value instanceof Number n)
                                        point.setMetricValue(new BigDecimal(n.toString()));
                                    else if (value != null)
                                        point.setValueText(
                                                value instanceof String text
                                                        ? text
                                                        : JSON.toJSONString(value));
                                    result.add(point);
                                });
        return result;
    }

    private List<IotDataPoint> chartPoints(Long id, Date from, Date to) {
        Grant grant = history.capture(id);
        if (from != null && to != null && from.after(to))
            throw new ServiceException("开始时间不能晚于结束时间");
        long end = to == null ? grant.lastVisibleTime() : Math.min(to.getTime(), grant.asOf());
        long start = from == null ? Math.max(0, end - 86400000L) : from.getTime();
        var result = points(samples(grant, start, end, 2000));
        history.recheck(grant);
        return result;
    }

    @Override
    public IotPestChartVo getPestChart(Long id, Date from, Date to) {
        return JetLinksPestCharts.chart(id, chartPoints(id, from, to));
    }

    @Override
    public IotPestNightChartVo getPestNightChart(Long id, Date from, Date to) {
        var grant = history.capture(id);
        if (from == null && to == null) to = new Date(grant.lastVisibleTime());
        Date[] range = JetLinksPestCharts.nightRange(from, to);
        var result = JetLinksPestCharts.night(id, chartPoints(id, range[0], range[1]), from, to);
        history.recheck(grant);
        return result;
    }

    @Override
    public void appendPoint(IotDataPoint point) {
        appendDataPointsStorageOnly(List.of(point));
    }

    @Override
    public void appendDataPointStorageOnly(IotDataPoint point) {
        appendDataPointsStorageOnly(List.of(point));
    }

    @Override
    public void appendDataPointsStorageOnly(List<IotDataPoint> points) {
        if (points == null || points.isEmpty()) return;
        List<Long> ids = points.stream().map(IotDataPoint::getDeviceId).distinct().toList();
        access.require(ids);
        if (ids.size() != 1) throw new ServiceException("遥测批次必须归属同一设备以锁定归属版本");
        access.write(
                ids.get(0),
                () -> {
                    List<RecordDto> properties = properties(ids.get(0));
                    List<TelemetrySample> samples = new ArrayList<>();
                    for (IotDataPoint point : points) {
                        if (point.getCollectTime() == null) throw new ServiceException("遥测缺少源采集时间");
                        if (point.getCollectTime().getTime() < access.visibleFrom(ids.get(0)))
                            throw new ServiceException("拒绝写入当前归属期之前的遥测");
                        Object value =
                                point.getTypedValue() != null
                                        ? point.getTypedValue()
                                        : point.getMetricValue() != null
                                                ? point.getMetricValue()
                                                : point.getValueText();
                        if (value == null) continue;
                        samples.add(
                                new TelemetrySample(
                                        point.getDeviceId().toString(),
                                        point.getPointId() == null
                                                ? null
                                                : point.getPointId().toString(),
                                        point.getCollectTime().getTime(),
                                        point.getReceivedTime() == null
                                                ? System.currentTimeMillis()
                                                : point.getReceivedTime().getTime(),
                                        Map.of(
                                                point.getPropertyIdentifier() != null
                                                                && !point.getPropertyIdentifier()
                                                                        .isBlank()
                                                        ? identifier(
                                                                point.getPropertyIdentifier(),
                                                                properties)
                                                        : identifier(
                                                                point.getMetricCode(), properties),
                                                value)));
                    }
                    return await(
                            rpc.getTelemetry().ingest(access.context(rpc, ids.get(0)), samples));
                });
    }
}
