package com.ym.agriculture.farming.dubbo;

import cn.hutool.core.convert.Convert;
import com.alibaba.fastjson2.JSON;
import com.ym.agriculture.api.farming.RemoteAgricultureMobileService;
import com.ym.agriculture.api.farming.domain.bo.RemoteAgricultureMobileCommandBo;
import com.ym.agriculture.api.farming.domain.bo.RemoteAgricultureMobileQueryBo;
import com.ym.agriculture.api.farming.domain.vo.RemoteAgricultureViewVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardMapDataVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardMapPlotVo;
import com.ym.agriculture.farming.dashboard.service.ISfDashboardService;
import com.ym.agriculture.shared.dubbo.support.RemoteCommandIdempotencyExecutor;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordMediaGeoPatchBo;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordSaveBo;
import com.ym.agriculture.farming.farmrecord.service.ISfFarmingRecordService;
import com.ym.agriculture.farming.news.service.ISfNewsService;
import com.ym.agriculture.farming.weatheralert.service.ISfWeatherAlertService;
import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** 农业小程序领域 Provider。 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteAgricultureMobileServiceImpl implements RemoteAgricultureMobileService {

    private final ISfDashboardService dashboardService;
    private final ISfWeatherAlertService weatherAlertService;
    private final ISfNewsService newsService;
    private final ISfFarmingRecordService farmingRecordService;
    private final RemoteCommandIdempotencyExecutor idempotencyExecutor;

    @Override
    public RemoteAgricultureViewVo homeOverview(Long fieldId) {
        RemoteAgricultureViewVo map = homeMap(fieldId);
        RemoteAgricultureViewVo overview = new RemoteAgricultureViewVo();
        overview.put("plots", map.getOrDefault("plots", List.of()));
        overview.put("weather", dashboardService.getWeatherCard());
        overview.put("deviceCards", deviceCards());
        overview.put("sensorSummary", null);
        return overview;
    }

    @Override
    public RemoteAgricultureViewVo homeMap(Long fieldId) {
        SfDashboardMapDataVo data = dashboardService.getMapData();
        if (fieldId == null) {
            return view(data);
        }
        SfDashboardMapDataVo filtered = new SfDashboardMapDataVo();
        filtered.setPlots(data.getPlots().stream()
            .filter(plot -> fieldId.equals(plot.getFieldId()))
            .toList());
        return view(filtered);
    }

    @Override
    public RemoteAgricultureViewVo homeWeather() {
        return view(dashboardService.getWeatherCard());
    }

    @Override
    public RemoteAgricultureViewVo sensorSummary() {
        return view(dashboardService.getSensorSummary());
    }

    @Override
    public RemoteAgricultureViewVo weatherAlertSummary() {
        return view(weatherAlertService.summaryForCurrentTenant());
    }

    @Override
    public PageResult<RemoteAgricultureViewVo> pageWeatherAlerts(RemoteAgricultureMobileQueryBo query) {
        Boolean activeOnly = Convert.toBool(value(query, "activeOnly"), true);
        return page(weatherAlertService.pageForCurrentTenant(activeOnly, pageQuery(query)));
    }

    @Override
    public RemoteAgricultureViewVo getWeatherAlert(String warningId) {
        return view(weatherAlertService.detail(warningId));
    }

    @Override
    public PageResult<RemoteAgricultureViewVo> pageNews(RemoteAgricultureMobileQueryBo query) {
        return page(newsService.queryMobilePage(text(query, "category"), text(query, "keyword"), pageQuery(query)));
    }

    @Override
    public RemoteAgricultureViewVo getNews(Long articleId) {
        return view(newsService.getMobileDetail(articleId));
    }

    @Override
    public RemoteAgricultureViewVo newsDashboard() {
        return view(newsService.getDashboard());
    }

    @Override
    public PageResult<RemoteAgricultureViewVo> pageFarmingRecords(RemoteAgricultureMobileQueryBo query) {
        return page(farmingRecordService.pageForMobile(longValue(query, "fieldId"),
            longValue(query, "workItemId"), longValue(query, "categoryId"), text(query, "status"),
            date(query, "from"), date(query, "to"), pageQuery(query)));
    }

    @Override
    public RemoteAgricultureViewVo getFarmingRecord(Long recordId) {
        return view(farmingRecordService.getDetail(recordId));
    }

    @Override
    public RemoteAgricultureViewVo latestFarmingImages(Long fieldId, Integer limit) {
        return view(farmingRecordService.getLatestRecordImages(fieldId, limit));
    }

    @Override
    public RemoteAgricultureViewVo farmingSensorPreview(RemoteAgricultureMobileQueryBo query, boolean simple) {
        List<Long> fieldIds = longList(query, "fieldIds");
        return view(simple ? farmingRecordService.buildSensorSimplePreview(fieldIds)
            : farmingRecordService.buildSensorPreview(fieldIds));
    }

    @Override
    public RemoteAgricultureViewVo farmingWeatherPreview(RemoteAgricultureMobileQueryBo query) {
        return view(farmingRecordService.buildWeatherPreview(requiredDate(query, "happenedAt")));
    }

    @Override
    public RemoteAgricultureViewVo farmingGrowthStagePreview(RemoteAgricultureMobileQueryBo query) {
        return view(farmingRecordService.buildGrowthStagePreview(longList(query, "fieldIds"),
            requiredDate(query, "happenedAt")));
    }

    @Override
    public RemoteAgricultureViewVo addFarmingDraft(RemoteAgricultureMobileCommandBo command) {
        return mutate("FARMING_DRAFT_CREATE", command, () -> {
            Long recordId = farmingRecordService.addDraft(payload(command, SfFarmingRecordSaveBo.class));
            return Map.of("recordId", recordId);
        });
    }

    @Override
    public RemoteAgricultureViewVo updateFarmingDraft(Long recordId, RemoteAgricultureMobileCommandBo command) {
        SfFarmingRecordSaveBo bo = payload(command, SfFarmingRecordSaveBo.class);
        bo.setRecordId(recordId);
        return mutate("FARMING_DRAFT_UPDATE", command,
            () -> result(farmingRecordService.updateDraft(recordId, bo)));
    }

    @Override
    public RemoteAgricultureViewVo submitFarmingDraft(Long recordId, RemoteAgricultureMobileCommandBo command) {
        return mutate("FARMING_DRAFT_SUBMIT", command,
            () -> result(farmingRecordService.submit(recordId)));
    }

    @Override
    public RemoteAgricultureViewVo updateSubmittedFarmingRecord(Long recordId,
                                                                 RemoteAgricultureMobileCommandBo command) {
        SfFarmingRecordSaveBo bo = payload(command, SfFarmingRecordSaveBo.class);
        bo.setRecordId(recordId);
        return mutate("FARMING_SUBMITTED_UPDATE", command,
            () -> result(farmingRecordService.updateSubmitted(recordId, bo)));
    }

    @Override
    public RemoteAgricultureViewVo deleteFarmingRecord(Long recordId, boolean submitted,
                                                        RemoteAgricultureMobileCommandBo command) {
        return mutate(submitted ? "FARMING_SUBMITTED_DELETE" : "FARMING_DRAFT_DELETE", command,
            () -> result(submitted ? farmingRecordService.removeSubmitted(recordId)
                : farmingRecordService.removeDraft(recordId)));
    }

    @Override
    public RemoteAgricultureViewVo patchFarmingMedia(Long recordId, Long mediaId,
                                                      RemoteAgricultureMobileCommandBo command) {
        return mutate("FARMING_MEDIA_GEO_PATCH", command,
            () -> farmingRecordService.patchMediaGeo(recordId, mediaId,
                payload(command, SfFarmingRecordMediaGeoPatchBo.class)));
    }

    private RemoteAgricultureViewVo deviceCards() {
        RemoteAgricultureViewVo summary = view(dashboardService.getSensorSummary());
        List<Map<String, Object>> devices = new ArrayList<>();
        collectDevices(summary, devices);
        long online = devices.stream().filter(device -> Boolean.TRUE.equals(device.get("online"))).count();
        long total = devices.size();
        Map<String, Object> sensorCard = card("SENSOR_CARD", "农业传感器", online, total - online);
        return new RemoteAgricultureViewVo(Map.of(
            "sensorCard", sensorCard,
            "machineCard", card("MACHINE_CARD", "综合设备", 0, 0),
            "cameraCard", card("CAMERA_CARD", "视频监控", 0, 0)));
    }

    @SuppressWarnings("unchecked")
    private static void collectDevices(Object node, List<Map<String, Object>> devices) {
        if (node instanceof Map<?, ?> map) {
            if (map.containsKey("deviceId") && map.containsKey("online")) {
                devices.add((Map<String, Object>) map);
            }
            map.values().forEach(value -> collectDevices(value, devices));
        } else if (node instanceof Iterable<?> iterable) {
            iterable.forEach(value -> collectDevices(value, devices));
        }
    }

    private static Map<String, Object> card(String kind, String title, long online, long offline) {
        return Map.of("kind", kind, "title", title, "onlineCount", online, "offlineCount", offline,
            "totalCount", online + offline, "hasOffline", offline > 0);
    }

    private RemoteAgricultureViewVo mutate(String action, RemoteAgricultureMobileCommandBo command,
                                            Supplier<?> operation) {
        AtomicReference<Object> result = new AtomicReference<>();
        idempotencyExecutor.execute(action, command.getRequestId(), command.getBusinessId(), () -> {
            result.set(operation.get());
            return 1;
        });
        if (result.get() != null) {
            return view(result.get());
        }
        return new RemoteAgricultureViewVo(Map.of("requestId", command.getRequestId(),
            "businessId", command.getBusinessId(), "idempotentReplay", true));
    }

    private static Map<String, Object> result(boolean success) {
        return Map.of("success", success);
    }

    private static PageQuery pageQuery(RemoteAgricultureMobileQueryBo query) {
        RemoteAgricultureMobileQueryBo safe = query == null ? new RemoteAgricultureMobileQueryBo() : query;
        return new PageQuery(safe.getPageSize(), safe.getPageNum());
    }

    private static Object value(RemoteAgricultureMobileQueryBo query, String key) {
        return query == null || query.getFilters() == null ? null : query.getFilters().get(key);
    }

    private static String text(RemoteAgricultureMobileQueryBo query, String key) {
        Object value = value(query, key);
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private static Long longValue(RemoteAgricultureMobileQueryBo query, String key) {
        return Convert.toLong(value(query, key));
    }

    private static Date date(RemoteAgricultureMobileQueryBo query, String key) {
        return Convert.toDate(value(query, key));
    }

    private static Date requiredDate(RemoteAgricultureMobileQueryBo query, String key) {
        Date date = date(query, key);
        if (date == null) {
            throw new IllegalArgumentException(key + "不能为空");
        }
        return date;
    }

    private static List<Long> longList(RemoteAgricultureMobileQueryBo query, String key) {
        Object raw = value(query, key);
        if (raw instanceof Iterable<?> values) {
            List<Long> ids = new ArrayList<>();
            values.forEach(value -> ids.add(Convert.toLong(value)));
            return ids;
        }
        String text = raw == null ? "" : raw.toString();
        List<Long> ids = new ArrayList<>();
        for (String part : text.split(",")) {
            if (!part.isBlank()) {
                ids.add(Long.valueOf(part.trim()));
            }
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException(key + "不能为空");
        }
        return ids;
    }

    private static <T> T payload(RemoteAgricultureMobileCommandBo command, Class<T> type) {
        return JSON.parseObject(JSON.toJSONString(command.getPayload()), type);
    }

    private static PageResult<RemoteAgricultureViewVo> page(PageResult<?> source) {
        return PageResult.build(source.getRows().stream()
            .map(RemoteAgricultureMobileServiceImpl::view).toList(), source.getTotal());
    }

    @SuppressWarnings("unchecked")
    private static RemoteAgricultureViewVo view(Object source) {
        if (source == null) {
            return new RemoteAgricultureViewVo();
        }
        if (source instanceof Map<?, ?> sourceMap) {
            return new RemoteAgricultureViewVo((Map<String, Object>) sourceMap);
        }
        LinkedHashMap<String, Object> fields = JSON.parseObject(JSON.toJSONString(source), LinkedHashMap.class);
        return new RemoteAgricultureViewVo(fields);
    }
}
