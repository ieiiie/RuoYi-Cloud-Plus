package com.ym.agriculture.farming.farmrecord.support;

import cn.hutool.json.JSONUtil;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.iot.api.domain.bo.RemoteDeviceQueryBo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteLatestTelemetryVo;
import com.ym.agriculture.farming.field.dao.SfFieldIotMapper;
import com.ym.agriculture.farming.field.support.SfFieldIotDataPointAccessor;
import com.ym.agriculture.farming.field.support.SfFieldIotDeviceAccessor;
import com.ym.agriculture.farming.field.model.entity.SfFieldIot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按地块绑定 SN 拉取绑定设备档案与最新测点，组装 {@code sf_farming_record.sensor_snapshot_json}。
 *
 * @author ym-cloud
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FarmingFieldSensorSnapshotBuilder {

    private final SfFieldIotMapper fieldIotMapper;
    private final SfFieldIotDeviceAccessor iotDeviceAccessor;
    private final SfFieldIotDataPointAccessor iotDataPointAccessor;

    /**
     * 生成地块原始传感器快照（含 metrics/names/texts 等），供 Service 层转换为极简结构。
     */
    public Map<String, Object> buildRawFieldSnapshot(Long fieldId) {
        if (fieldId == null) {
            return Map.of();
        }
        try {
            List<SfFieldIot> links = fieldIotMapper.selectNormalListByFieldId(fieldId);
            Set<String> sns = links.stream()
                .map(SfFieldIot::getDeviceSn)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toCollection(HashSet::new));
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("fieldId", fieldId);
            if (sns.isEmpty()) {
                root.put("devices", List.of());
                return root;
            }
            RemoteDeviceQueryBo bo = new RemoteDeviceQueryBo();
            bo.setDeviceCodeList(new ArrayList<>(sns));
            List<RemoteDeviceSummaryVo> devs =
                TenantHelper.ignore(() -> iotDeviceAccessor.queryList(bo));
            if (devs == null || devs.isEmpty()) {
                root.put("devices", List.of());
                return root;
            }
            List<RemoteDeviceSummaryVo> normal =
                devs.stream().filter(d -> SystemConstants.NORMAL.equals(d.getStatus())).toList();
            Set<Long> ids = normal.stream().map(RemoteDeviceSummaryVo::getDeviceId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            Map<Long, RemoteLatestTelemetryVo> latest =
                ids.isEmpty() ? Map.of() : iotDataPointAccessor.getLatestMapFresh(ids);

            List<Map<String, Object>> devices = new ArrayList<>();
            for (RemoteDeviceSummaryVo d : normal) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("deviceId", d.getDeviceId());
                row.put("deviceCode", d.getDeviceCode());
                row.put("deviceName", d.getDeviceName());
                row.put("deviceCategory", d.getDeviceCategory());
                row.put("productName", d.getProductName());
                RemoteLatestTelemetryVo lv = latest.get(d.getDeviceId());
                if (lv != null) {
                    row.put("metrics", copyNumberMap(lv.getMetrics()));
                    row.put("collectTimes", formatTimes(lv.getCollectTimes()));
                    row.put("units", copyStringMap(lv.getUnits()));
                    row.put("names", copyStringMap(lv.getNames()));
                    row.put("texts", copyStringMap(lv.getTexts()));
                }
                devices.add(row);
            }
            root.put("devices", devices);
            return root;
        } catch (Exception e) {
            log.warn("组装农事传感器快照失败 fieldId={}", fieldId, e);
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("fieldId", fieldId);
            empty.put("devices", List.of());
            return empty;
        }
    }

    /**
     * 生成快照 JSON：无绑定或设备查询失败时返回含空 devices 的结构，不抛异常。
     */
    public String buildSnapshotJson(Long fieldId) {
        if (fieldId == null) {
            return "{}";
        }
        return JSONUtil.toJsonStr(buildRawFieldSnapshot(fieldId));
    }

    private static Map<String, String> copyStringMap(Map<String, ?> in) {
        if (in == null || in.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, ?> e : in.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            out.put(e.getKey(), String.valueOf(e.getValue()));
        }
        return out;
    }

    private static Map<String, String> formatTimes(Map<String, Date> collectTimes) {
        if (collectTimes == null || collectTimes.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Date> e : collectTimes.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                out.put(e.getKey(), String.valueOf(e.getValue().getTime()));
            }
        }
        return out;
    }

    private static Map<String, ?> copyNumberMap(Map<String, ?> in) {
        return in == null ? Map.of() : new LinkedHashMap<>(in);
    }
}
