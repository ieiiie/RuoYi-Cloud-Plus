package com.ym.iot.alarm.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.await;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.alarm.service.INativeAlarmService;
import com.ym.iot.alarm.support.NativeAlarmScope;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.ownership.domain.dto.DeviceOwnershipSnapshot;
import com.ym.jetlinks.rpc.*;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** 原生告警门面：保持原生 ID 与角色规则，负责请求授权、返回复验及传输适配。 */
@Service
@ConditionalOnJetLinks
public class NativeAlarmServiceImpl implements INativeAlarmService {
    private static final Set<String> FILTERS =
            Set.of(
                    "name",
                    "alarmName",
                    "alarmRecordId",
                    "alarmConfigId",
                    "targetId",
                    "targetType",
                    "sourceId",
                    "state",
                    "handleType",
                    "handleState",
                    "level",
                    "beginTime",
                    "endTime",
                    "productId");
    private final JetLinksRpcClient rpc;
    private final JetLinksAccess access;
    private final NativeAlarmScope scope;

    public NativeAlarmServiceImpl(
            JetLinksRpcClient rpc, JetLinksAccess access, NativeAlarmScope scope) {
        this.rpc = rpc;
        this.access = access;
        this.scope = scope;
    }

    @Override
    public PageResult<Map<String, Object>> configurations(Map<String, String> parameters) {
        PageDto<RecordDto> result = await(rpc.getAlarm().configurations(query(parameters, false)));
        return PageResult.build(
                result.records().stream().map(this::settingsView).toList(), result.total());
    }

    @Override
    public Map<String, Object> configuration(String id) {
        return settings(id);
    }

    private RecordDto configurationRecord(String id) {
        QueryDto q = query(Map.of(), false);
        return one(
                await(
                        rpc.getAlarm()
                                .configurations(
                                        new QueryDto(
                                                List.of(required(id, "告警配置ID")),
                                                q.filters(),
                                                1,
                                                1,
                                                null,
                                                null))),
                "告警配置不存在或无权访问");
    }

    @Override
    public Map<String, Object> save(String pathId, RecordDto input, String requestId) {
        throw new ServiceException("规则由平台维护，请使用租户设置接口");
    }

    @Override
    public Map<String, Object> settings(String id) {
        RecordDto row = configurationRecord(id);
        Map<String, Object> result = settingsView(row);
        String product = string(row.data().get("productId"));
        List<Map<String, Object>> choices = new ArrayList<>();
        for (String device : access.ids()) {
            RecordDto d = await(rpc.getDevice().device(device));
            if (d != null && Objects.equals(product, string(d.data().get("productId")))) {
                Map<String, Object> option = new LinkedHashMap<>();
                option.put("value", device);
                option.put(
                        "label",
                        Objects.toString(
                                d.data().get("name"),
                                Objects.toString(d.data().get("deviceName"), device)));
                choices.add(option);
            }
        }
        result.put("deviceOptions", choices);
        return result;
    }

    private Map<String, Object> settingsView(RecordDto row) {
        if (!(row.data().get("tenantSettings") instanceof Map<?, ?>))
            throw new ServiceException("平台设置接口尚未就绪，请稍后重试");
        Map<String, Object> result = new LinkedHashMap<>(map(row.data().get("tenantSettings")));
        result.put("id", row.id());
        result.put("version", row.version());
        return result;
    }

    @Override
    public Map<String, Object> updateSettings(
            String id, Map<String, Object> input, String requestId) {
        if (input == null
                || !Set.of("version", "enabled", "deviceIds", "parameters", "recipients")
                        .containsAll(input.keySet())) throw new ServiceException("只能修改规则开放的设置");
        RecordDto old = configurationRecord(id);
        long version = number(input.get("version"), "配置版本");
        // 版本最终由平台事务校验；成功请求重试可命中平台幂等结果。
        Map<String, Object> patch = new LinkedHashMap<>(input);
        patch.remove("version");
        if (patch.containsKey("enabled") && !(patch.get("enabled") instanceof Boolean))
            throw new ServiceException("启停状态无效");
        if (patch.containsKey("deviceIds") && ids(patch.get("deviceIds")).isEmpty())
            throw new ServiceException("请至少选择一台适用设备");
        Map<String, Object> data = new LinkedHashMap<>(old.data());
        data.put("tenantPatch", patch);
        data.put(
                "deviceIds",
                patch.getOrDefault("deviceIds", old.data().get("authorizedDeviceIds")));
        saveInternal(id, new RecordDto(id, data, version), requestId);
        return settings(id);
    }

    private Map<String, Object> saveInternal(String pathId, RecordDto input, String requestId) {
        if (input == null || input.data() == null) throw new ServiceException("告警配置不能为空");
        if (pathId == null && input.id() != null) throw new ServiceException("新增告警不能指定已有配置ID");
        String id = pathId;
        if (pathId != null && input.id() != null && !pathId.equals(input.id()))
            throw new ServiceException("告警配置ID不一致");
        if (id != null) {
            RecordDto before = configurationRecord(id);
            if (!input.data().containsKey("tenantPatch") && before.version() != input.version())
                throw new ServiceException("告警配置已变更，请刷新后重试");
            if (!scope.key().equals(before.data().get("scopeKey")))
                throw new ServiceException("此告警配置由内部运维维护");
        }
        Map<String, Object> data = new LinkedHashMap<>(input.data());
        data.put("scopeKey", scope.key());
        data.put("targetType", "device");
        data.remove("creatorId");
        data.remove("modifierId");
        data.remove("ownerId");
        String product = string(data.get("productId"));
        if (product == null)
            product =
                    string(
                            map(map(map(data.get("scene")).get("trigger")).get("device"))
                                    .get("productId"));
        product = required(product, "产品ID");
        List<String> selected = ids(data.get("deviceIds"));
        Set<String> allowed = new LinkedHashSet<>(access.ids());
        if (!allowed.containsAll(selected)) throw new ServiceException("包含未授权设备");
        List<String> candidates = selected.isEmpty() ? List.copyOf(allowed) : selected;
        List<String> deviceIds = new ArrayList<>();
        for (String device : candidates) {
            RecordDto row = await(rpc.getDevice().device(device));
            if (row == null) throw new ServiceException("JetLinks设备不存在");
            if (product.equals(string(row.data().get("productId")))) deviceIds.add(device);
            else if (!selected.isEmpty()) throw new ServiceException("设备不属于所选产品");
        }
        if (deviceIds.isEmpty()) throw new ServiceException("所选产品没有可配置告警的授权设备");
        Map<Long, DeviceOwnershipSnapshot> owners =
                access.capture(deviceIds.stream().map(NativeAlarmServiceImpl::deviceId).toList());
        Map<String, Long> versions = new LinkedHashMap<>();
        owners.forEach(
                (device, owner) -> versions.put(device.toString(), owner.getAssignmentVersion()));
        data.put("productId", product);
        data.put("deviceIds", deviceIds);
        data.put("authorizedDeviceIds", deviceIds);
        data.put("assignmentVersions", versions);
        access.recheck(owners);
        RecordDto saved =
                await(
                        rpc.getAlarm()
                                .saveConfiguration(
                                        rpc.context(request(requestId)),
                                        new RecordDto(id, data, input.version())));
        if (saved == null) throw new ServiceException("JetLinks未确认告警配置保存");
        access.recheck(owners);
        return view(saved);
    }

    @Override
    public boolean delete(String id, long version, String requestId) {
        throw new ServiceException("规则由平台维护，租户不能删除规则");
    }

    @Override
    public Map<String, Object> enabled(String id, boolean enabled, long version, String requestId) {
        requireOwnedConfiguration(id, version);
        return view(
                await(
                        rpc.getAlarm()
                                .setEnabled(
                                        rpc.context(request(requestId)), id, enabled, version)));
    }

    private void requireOwnedConfiguration(String id, long version) {
        RecordDto row = configurationRecord(id);
        if (!scope.key().equals(row.data().get("scopeKey")))
            throw new ServiceException("此告警配置由内部运维维护");
        if (row.version() != version) throw new ServiceException("告警配置已变更，请刷新后重试");
        List<Long> devices =
                ids(row.data().get("authorizedDeviceIds")).stream()
                        .map(NativeAlarmServiceImpl::deviceId)
                        .toList();
        access.require(devices);
    }

    @Override
    public PageResult<Map<String, Object>> records(Map<String, String> parameters) {
        return page(readHistory(historicalQuery(parameters), HistoryKind.RECORD, rpc.getAlarm()::records));
    }

    @Override
    public Map<String, Object> record(String id) {
        return view(recordInScope(id));
    }

    private RecordDto recordInScope(String id) {
        // 单条查询同样必须携带原生记录 ID 和有限历史窗口，不能只信任远程返回一行。
        HistoricalQuery request = historicalQuery(Map.of(
                "id", required(id, "告警记录ID"), "pageNum", "1", "pageSize", "1"));
        return one(readHistory(request, HistoryKind.RECORD, rpc.getAlarm()::records),
                "告警记录不存在或无权访问");
    }

    @Override
    public PageResult<Map<String, Object>> history(Map<String, String> parameters) {
        return page(readHistory(historicalQuery(parameters), HistoryKind.OCCURRENCE, rpc.getAlarm()::history));
    }

    @Override
    public PageResult<Map<String, Object>> handlingHistory(Map<String, String> parameters) {
        return page(readHistory(historicalQuery(parameters), HistoryKind.HANDLING, rpc.getAlarm()::handlingHistory));
    }

    private enum HistoryKind { RECORD, OCCURRENCE, HANDLING }

    /** 请求对象与授权凭据成对保存，返回后仍校验最初的 asOf 和产品筛选窗口。 */
    private record HistoricalQuery(QueryDto query, NativeAlarmScope.HistoricalScope scope) {}

    private HistoricalQuery historicalQuery(Map<String, String> parameters) {
        String product = parameters == null ? null : parameters.get("productId");
        if (product != null && product.isBlank()) product = null;
        var captured = scope.captureHistorical(product);
        return new HistoricalQuery(query(parameters, true, captured.filters()), captured);
    }

    /**
     * 不过滤掉越界行后继续透传远程 total：那会留下错误统计或分页信息。
     * 任一行越界则拒绝整次响应；空页也重验归属，避免并发转出后的旧统计继续返回。
     */
    private PageDto<RecordDto> readHistory(
            HistoricalQuery request, HistoryKind kind,
            Function<QueryDto, CompletableFuture<PageDto<RecordDto>>> fetch) {
        if (request.scope().windows().isEmpty()) {
            scope.recheck(request.scope());
            return new PageDto<>(List.of(), 0, request.query().page(), request.query().size());
        }
        PageDto<RecordDto> result = await(fetch.apply(request.query()));
        if (result == null || result.records() == null || result.total() < result.records().size()
                || result.records().size() > request.query().size())
            throw new ServiceException("JetLinks未返回有效的告警分页结果");
        for (RecordDto row : result.records()) requireHistoricalRow(request, row, kind);
        scope.recheck(request.scope());
        return result;
    }

    /** 在转成对外 Map 前检查原生 ID、请求目标及所有可能携带告警/处理内容的时间。 */
    private void requireHistoricalRow(HistoricalQuery request, RecordDto row, HistoryKind kind) {
        if (row == null || row.id() == null || row.id().isBlank() || row.data() == null)
            throw new ServiceException("JetLinks未返回有效的告警记录");
        QueryDto query = request.query();
        Map<String, Object> data = row.data();
        if ((!query.ids().isEmpty() && !query.ids().contains(row.id()))
                || data.get("id") != null && !row.id().equals(string(data.get("id"))))
            throw new ServiceException("告警返回与请求记录ID不一致");
        if (!"device".equals(data.get("targetType")))
            throw new ServiceException("告警返回包含未授权目标类型");
        String device = deviceId(string(data.get("targetId"))).toString();
        Map<String, Object> filters = query.filters();
        for (String field : List.of("targetId", "targetType", "sourceId", "alarmRecordId", "alarmConfigId")) {
            String actual = kind == HistoryKind.HANDLING && "alarmConfigId".equals(field)
                    ? "alarmId" : field;
            if (filters.get(field) != null && !Objects.equals(string(filters.get(field)), string(data.get(actual))))
                throw new ServiceException("告警返回超出本次请求的筛选范围");
        }
        long alarmTime = number(data.get("alarmTime"), "告警发生时间");
        if (alarmTime < 0 || filters.get("beginTime") != null && alarmTime < number(filters.get("beginTime"), "开始时间")
                || filters.get("endTime") != null && alarmTime > number(filters.get("endTime"), "结束时间"))
            throw new ServiceException("告警返回超出本次请求的查询时间范围");
        List<Long> times = new ArrayList<>();
        times.add(alarmTime);
        // 聚合记录首末时间必须同窗；A→B→A 的两个 A 区间也不能合并为一段。
        if (kind == HistoryKind.RECORD || data.get("lastAlarmTime") != null) {
            long last = number(data.get("lastAlarmTime"), "最后告警时间");
            if (last < alarmTime) throw new ServiceException("告警聚合时间顺序异常");
            times.add(last);
        }
        // 处理历史不能只校验 alarmTime：旧告警若由新租户处理，handleTime 会落在另一个区间。
        // 聚合/发生记录若附带处理时间，也必须校验；零仅作为非处理历史的“尚未处理”占位。
        if (kind == HistoryKind.HANDLING || data.get("handleTime") != null) {
            long handled = number(data.get("handleTime"), "告警处理时间");
            if (handled < 0 || kind == HistoryKind.HANDLING && handled < alarmTime)
                throw new ServiceException("告警处理时间顺序异常");
            if (kind == HistoryKind.HANDLING || handled > 0) times.add(handled);
        }
        scope.requireSameWindow(request.scope(), device, times.stream().mapToLong(Long::longValue).toArray());
    }

    @Override
    public Map<String, Object> handle(String id, Map<String, Object> input, String requestId) {
        if (input == null) throw new ServiceException("告警处理内容不能为空");
        RecordDto row = recordInScope(id);
        if (!"device".equals(row.data().get("targetType")))
            throw new ServiceException("仅可处理授权设备的告警");
        Long device = deviceId(required(string(row.data().get("targetId")), "告警目标设备"));
        DeviceOwnershipSnapshot owner = access.snapshot(device);
        long time = number(row.data().get("alarmTime"), "告警发生时间");
        if (time < owner.getEffectiveFrom().toEpochMilli())
            throw new ServiceException("只能查看历史归属期间的告警，不能处理当前告警");
        Map<String, Object> command = new LinkedHashMap<>();
        for (String field : List.of("type", "describe", "state", "handleState"))
            if (input.containsKey(field)) command.put(field, input.get(field));
        String type = required(string(command.get("type")), "处理类型");
        command.put("type", type);
        if (command.get("state") != null
                && !Set.of("normal", "warning").contains(command.get("state")))
            throw new ServiceException("无效的原生告警状态");
        return access.write(
                device,
                () -> {
                    DeviceOwnershipSnapshot locked = access.snapshot(device);
                    if (!Objects.equals(owner.getAssignmentVersion(), locked.getAssignmentVersion())
                            || !Objects.equals(owner.getTenantId(), locked.getTenantId())
                            || !Objects.equals(owner.getEffectiveFrom(), locked.getEffectiveFrom()))
                        throw new ServiceException("设备归属已变化，请刷新告警后重试");
                    return view(
                            await(
                                    rpc.getAlarm()
                                            .handle(
                                                    rpc.deviceContext(
                                                            request(requestId),
                                                            locked.getAssignmentVersion()),
                                                    id,
                                                    command)));
                });
    }

    @Override
    public List<Map<String, Object>> notificationOptions() {
        List<RecordDto> rows = await(rpc.getAlarm().notificationOptions(query(Map.of(), false)));
        if (rows == null) throw new ServiceException("JetLinks未返回通知配置");
        return rows.stream().map(NativeAlarmServiceImpl::view).toList();
    }

    @Override
    public long warningCount() {
        return records(Map.of("state", "warning", "pageNum", "1", "pageSize", "1")).getTotal();
    }

    QueryDto query(Map<String, String> parameters, boolean history) {
        return history ? historicalQuery(parameters).query() : query(parameters, false, scope.current());
    }

    private QueryDto query(Map<String, String> parameters, boolean history, Map<String, Object> authorized) {
        Map<String, String> p = parameters == null ? Map.of() : parameters;
        Map<String, Object> filters = new LinkedHashMap<>();
        for (String key : FILTERS)
            if (p.containsKey(key) && p.get(key) != null && !p.get(key).isBlank()) {
                String value = p.get(key);
                filters.put(
                        key,
                        Set.of("level", "beginTime", "endTime").contains(key)
                                ? number(value, key)
                                : value);
            }
        // 历史产品筛选已在 scope 中按归属快照收窄到具体窗口，禁止在此读取当前设备。
        if (history) filters.remove("productId");
        filters.putAll(authorized);
        String sort = p.getOrDefault("sort", history ? "alarmTime" : "modifyTime");
        if (!Set.of(
                        "alarmTime",
                        "lastAlarmTime",
                        "handleTime",
                        "createTime",
                        "modifyTime",
                        "name",
                        "level")
                .contains(sort)) throw new ServiceException("不支持的排序字段");
        String order = p.getOrDefault("order", "desc");
        if (!Set.of("asc", "desc").contains(order)) throw new ServiceException("不支持的排序方向");
        int page = bounded(p.get("pageNum"), 1, 1, 1000000),
                size = bounded(p.get("pageSize"), 20, 1, 500);
        List<String> ids = p.get("id") == null ? List.of() : List.of(required(p.get("id"), "ID"));
        return new QueryDto(ids, filters, page, size, sort, order);
    }

    static PageResult<Map<String, Object>> page(PageDto<RecordDto> page) {
        if (page == null || page.records() == null) throw new ServiceException("JetLinks未返回分页结果");
        return PageResult.build(
                page.records().stream().map(NativeAlarmServiceImpl::view).toList(), page.total());
    }

    static RecordDto one(PageDto<RecordDto> page, String error) {
        if (page == null || page.records() == null || page.records().size() != 1)
            throw new ServiceException(error);
        return page.records().get(0);
    }

    public static Map<String, Object> view(RecordDto row) {
        if (row == null || row.data() == null) throw new ServiceException("JetLinks未返回原生记录");
        Map<String, Object> result = new LinkedHashMap<>(row.data());
        result.remove("scopeKey");
        result.remove("assignmentVersions");
        result.put("id", row.id());
        result.put("version", row.version());
        return result;
    }

    private static String request(String id) {
        return required(id, "请求ID");
    }

    private static String required(String s, String name) {
        if (s == null || s.isBlank() || s.length() > 256) throw new ServiceException(name + "无效");
        return s.trim();
    }

    private static String string(Object o) {
        return o == null ? null : o.toString();
    }

    private static Long deviceId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0 || !Long.toString(id).equals(value)) throw new NumberFormatException();
            return id;
        } catch (RuntimeException e) {
            throw new ServiceException("无效的设备ID");
        }
    }

    private static long number(Object value, String name) {
        try {
            return Long.parseLong(Objects.toString(value, ""));
        } catch (RuntimeException e) {
            throw new ServiceException(name + "必须为整数");
        }
    }

    private static int bounded(String value, int fallback, int min, int max) {
        long n = value == null ? fallback : number(value, "分页参数");
        if (n < min || n > max) throw new ServiceException("分页参数超出范围");
        return (int) n;
    }

    private static List<String> ids(Object value) {
        if (value == null) return List.of();
        if (!(value instanceof Collection<?> list)) throw new ServiceException("设备ID必须为数组");
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (Object id : list) ids.add(deviceId(Objects.toString(id, "")).toString());
        return List.copyOf(ids);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }
}
