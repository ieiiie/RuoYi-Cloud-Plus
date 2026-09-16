package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.support.JetLinksMapping.*;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.device.domain.vo.IotDeviceVo;
import com.ym.iot.hfzk.domain.bo.IotHfzkUserDeviceBo;
import com.ym.iot.hfzk.domain.vo.IotHfzkUserDeviceVo;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksHfzkService;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.jetlinks.rpc.RecordDto;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;

/** Legacy registration rows come exclusively from the authorized core device catalog. */
@Service
@Primary
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksHfzkServiceImpl implements IJetLinksHfzkService {
    private final JetLinksRpcClient rpc;
    private final JetLinksAccess access;

    static List<Map<String, Object>> registrations(RecordDto device) {
        if (!(device.data().get("legacyRegistrations") instanceof Collection<?> values))
            throw new ServiceException("核心缺少legacyRegistrations登记目录契约");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof Map<?, ?> map)) throw new ServiceException("核心登记记录格式无效");
            Map<String, Object> row = new LinkedHashMap<>();
            map.forEach((k, v) -> row.put(k.toString(), v));
            for (String key : List.of("id", "deviceSn", "externalDeviceId", "deviceType"))
                if (row.get(key) == null || row.get(key).toString().isBlank())
                    throw new ServiceException("核心登记缺少字段: " + key);
            if (id(row.get("id").toString()) <= 0) throw new ServiceException("核心登记ID无效");
            row.remove("tenantId");
            rows.add(row);
        }
        return rows;
    }

    private static long time(Object value) {
        if (value == null) return Long.MIN_VALUE;
        if (value instanceof Date date) return date.getTime();
        return Long.parseLong(value.toString());
    }

    static boolean matches(Map<String, Object> row, Map<String, Object> filters) {
        if (filters == null) return true;
        for (var entry : filters.entrySet()) {
            if (!Set.of("externalDeviceId", "deviceSn", "deviceType", "deviceName")
                    .contains(entry.getKey()))
                throw new ServiceException("不支持的登记筛选字段: " + entry.getKey());
            Object value = entry.getValue();
            if (value == null || value.toString().isBlank()) continue;
            String actual = Objects.toString(row.get(entry.getKey()), null),
                    expected = value.toString();
            if (actual == null
                    || (entry.getKey().equals("deviceName")
                            ? !actual.contains(expected.trim())
                            : !actual.equals(expected))) return false;
        }
        return true;
    }

    private List<RecordDto> devices() {
        List<String> scope = access.ids();
        Set<String> allowed = new HashSet<>(scope);
        List<RecordDto> rows = all(rpc.getDevice()::devices, query(scope, Map.of(), null));
        if (rows.stream().anyMatch(row -> row == null || !allowed.contains(row.id())))
            throw new ServiceException("核心登记目录超出授权设备范围");
        return rows;
    }

    public List<RecordDto> records(Map<String, Object> filters) {
        List<RecordDto> devices = devices();
        var owners = access.capture(devices.stream().map(r -> id(r.id())).toList());
        List<RecordDto> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (RecordDto device : devices)
            for (var row : registrations(device)) {
                String registryId = row.get("id").toString();
                if (!seen.add(registryId)) throw new ServiceException("核心登记ID重复");
                row.put("tenantId", owners.get(id(device.id())).getTenantId());
                if (matches(row, filters))
                    result.add(new RecordDto(registryId, row, device.version()));
            }
        // Preserve the old ledger order. Two registrations for the same UAV remain two records.
        result.sort(
                Comparator.<RecordDto>comparingLong(r -> time(r.data().get("lastSyncTime")))
                        .reversed()
                        .thenComparing(r -> id(r.id()), Comparator.reverseOrder()));
        access.recheck(owners);
        return result;
    }

    @Override
    public List<IotHfzkUserDeviceVo> queryList(IotHfzkUserDeviceBo bo) {
        return beans(
                records(filters(bo, "externalDeviceId", "deviceSn", "deviceType", "deviceName")),
                "id",
                IotHfzkUserDeviceVo.class);
    }

    @Override
    public PageResult<IotHfzkUserDeviceVo> queryPageList(IotHfzkUserDeviceBo bo, PageQuery page) {
        return slice(queryList(bo), page);
    }

    @Override
    public List<IotDeviceVo> listByDeviceType(String type, String online) {
        return list(type, online, null);
    }

    private List<IotDeviceVo> list(String type, String online, String code) {
        List<RecordDto> devices = devices();
        var owners = access.capture(devices.stream().map(r -> id(r.id())).toList());
        List<RecordDto> matched = new ArrayList<>();
        for (var device : devices) {
            boolean registered =
                    registrations(device).stream()
                            .anyMatch(r -> Objects.equals(type, r.get("deviceType")));
            if (registered
                    && (online == null
                            || online.isBlank()
                            || online.equals(device.data().get("onlineStatus")))
                    && (code == null
                            || code.isBlank()
                            || Objects.toString(device.data().get("deviceCode"), "")
                                    .contains(code))) matched.add(device);
        }
        matched.sort(Comparator.comparing((RecordDto r) -> id(r.id())).reversed());
        access.recheck(owners);
        return beans(matched, "deviceId", IotDeviceVo.class);
    }

    @Override
    public PageResult<IotDeviceVo> pageByDeviceType(
            String type, String online, String code, PageQuery page) {
        return slice(list(type, online, code), page);
    }
}
