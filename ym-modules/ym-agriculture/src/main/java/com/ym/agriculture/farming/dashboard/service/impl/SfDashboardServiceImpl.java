package com.ym.agriculture.farming.dashboard.service.impl;

import cn.hutool.core.date.DateUtil;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.iot.api.domain.bo.RemoteDeviceQueryBo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteSeriesVo;
import com.ym.iot.api.RemoteIotTelemetryService;
import com.ym.iot.api.RemoteIotDeviceService;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchDashboardStatsVo;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
import com.ym.agriculture.farming.dashboard.model.vo.DeviceCategoryVo;
import com.ym.agriculture.farming.dashboard.model.vo.ProductGroupVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardMapDataVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardMapPlotVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardSensorMarkerVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardSensorSummaryItemVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardSensorSummaryVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardWeatherCardVo;
import com.ym.agriculture.farming.dashboard.service.ISfDashboardService;
import com.ym.agriculture.farming.dashboard.support.DashboardSensorOnline;
import com.ym.agriculture.farming.field.dao.SfFieldIotMapper;
import com.ym.agriculture.farming.field.model.bo.SfFieldBo;
import com.ym.agriculture.farming.field.model.entity.SfFieldIot;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.agriculture.farming.field.service.ISfFieldService;
import com.ym.agriculture.farming.field.support.SfFieldMasterDictAccessor;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherForecastVo;
import com.ym.agriculture.farming.weather.service.ISfWeatherService;
import com.ym.system.api.domain.vo.RemoteDictDataVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 仪表盘数据聚合：地块权限与批次、IoT、天气服务组合。
 * <p>传感器汇总 {@link #getSensorSummary()} 仅按当前租户查询 {@code iot_device} 正常档案设备（含无人机/机场产品），与地块绑定无关。</p>
 *
 * @author ym-cloud
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SfDashboardServiceImpl implements ISfDashboardService {

    /** 主库字典 {@code iot_device_type}，与 {@code iot_device.device_category}（dictValue）一致 */
    private static final String IOT_DEVICE_TYPE_DICT = "iot_device_type";

    /** 字典中未命中时的兜底分类编码 */
    private static final String CATEGORY_OTHER = "OTHER";

    /** 字典中未命中时的兜底分类展示名 */
    private static final String CATEGORY_OTHER_LABEL = "其它设备";

    private final ISfFieldService fieldService;
    private final SfFieldIotMapper fieldIotMapper;
    private final SfPlantingBatchMapper plantingBatchMapper;
    @DubboReference
    private RemoteIotDeviceService iotDeviceService;
    @DubboReference
    private RemoteIotTelemetryService dataPointService;
    private final ISfWeatherService weatherService;
    private final ISfPlantingBatchService plantingBatchService;
    private final SfFieldMasterDictAccessor masterDictAccessor;

    @Override
    public SfDashboardMapDataVo getMapData() {
        List<SfFieldVo> fields = fieldService.queryList(new SfFieldBo());
        SfDashboardMapDataVo root = new SfDashboardMapDataVo();
        if (fields.isEmpty()) {
            return root;
        }
        List<Long> fieldIds = fields.stream().map(SfFieldVo::getFieldId).toList();
        List<SfFieldIot> links = fieldIotMapper.selectNormalListByFieldIds(fieldIds);
        Map<Long, List<String>> snsByField = links.stream()
            .filter(l -> StringUtils.isNotBlank(l.getDeviceSn()))
            .collect(Collectors.groupingBy(SfFieldIot::getFieldId,
                Collectors.mapping(SfFieldIot::getDeviceSn, Collectors.toList())));
        Set<String> allSns = links.stream()
            .map(SfFieldIot::getDeviceSn)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
        Map<String, RemoteDeviceSummaryVo> bySn = loadDevicesBySn(allSns);
        Date now = new Date();
        for (SfFieldVo f : fields) {
            SfDashboardMapPlotVo p = new SfDashboardMapPlotVo();
            p.setFieldId(f.getFieldId());
            p.setFieldName(f.getFieldName());
            p.setBoundaryGeojson(f.getBoundaryGeojson());
            p.setCenterLng(f.getCenterLng());
            p.setCenterLat(f.getCenterLat());
            p.setMapDisplayStatus(f.getMapDisplayStatus());
            p.setPrimaryBatchId(plantingBatchMapper.selectActiveBatchIdByFieldId(f.getFieldId()));
            for (String sn : snsByField.getOrDefault(f.getFieldId(), List.of())) {
                RemoteDeviceSummaryVo d = bySn.get(sn != null ? sn.trim() : null);
                if (d == null || !SystemConstants.NORMAL.equals(d.getStatus())) {
                    continue;
                }
                SfDashboardSensorMarkerVo m = new SfDashboardSensorMarkerVo();
                m.setDeviceId(d.getDeviceId());
                m.setDeviceCode(d.getDeviceCode());
                m.setDeviceName(d.getDeviceName());
                m.setDeviceCategory(d.getDeviceCategory());
                m.setProductName(d.getProductName());
                BigDecimal lng = d.getLng() != null ? d.getLng() : f.getCenterLng();
                BigDecimal lat = d.getLat() != null ? d.getLat() : f.getCenterLat();
                m.setLng(lng);
                m.setLat(lat);
                m.setOnline(DashboardSensorOnline.isOnline(d, now));
                m.setOnlineStatus(DashboardSensorOnline.status(d));
                m.setLastReportTime(d.getLastReportTime());
                m.setFieldId(f.getFieldId());
                p.getSensors().add(m);
            }
            root.getPlots().add(p);
        }
        return root;
    }

    @Override
    public SfDashboardWeatherCardVo getWeatherCard() {
        var latest = weatherService.getLatest();
        SfDashboardWeatherCardVo card = new SfDashboardWeatherCardVo();
        card.setAdcode(latest.getAdcode());
        card.setLive(latest.getLive());
        List<SfWeatherForecastVo> fc = latest.getForecasts();
        if (fc != null && !fc.isEmpty()) {
            String today = DateUtil.format(new Date(), "yyyy-MM-dd");
            SfWeatherForecastVo todayForecast = fc.stream()
                .filter(item -> today.equals(item.getCastDate()))
                .findFirst()
                .orElseGet(() -> fc.stream()
                    .filter(item -> item.getCastDate() != null && item.getCastDate().compareTo(today) >= 0)
                    .findFirst()
                    .orElse(null));
            SfWeatherForecastVo nameSource = todayForecast != null ? todayForecast : fc.get(0);
            card.setCityName(nameSource.getCityName());
            card.setToday(todayForecast);
            card.setForecasts(fc);
        }
        return card;
    }

    @Override
    public SfDashboardSensorSummaryVo getSensorSummary() {
        List<RemoteDeviceSummaryVo> inScope = listDashboardIotDevices();
        Date now = new Date();

        // 加载 iot_device_type 字典，构建 dictValue → (dictLabel, dictSort) 映射
        List<RemoteDictDataVo> dictRows = masterDictAccessor.getDictDataList(IOT_DEVICE_TYPE_DICT);
        Map<String, String> dictValueToLabel = new LinkedHashMap<>();
        Map<String, Integer> dictValueToSort = new HashMap<>();
        for (RemoteDictDataVo row : dictRows) {
            if (StringUtils.isNotBlank(row.getDictValue())) {
                String v = row.getDictValue().trim();
                dictValueToLabel.put(v, StringUtils.isNotBlank(row.getDictLabel())
                    ? row.getDictLabel().trim() : v);
                dictValueToSort.put(v, row.getDictSort() != null ? row.getDictSort() : Integer.MAX_VALUE);
            }
        }

        // 按 (categoryCode, productId) 两级分组聚合
        Map<String, Map<String, ProductAccum>> categoryProductMap = new LinkedHashMap<>();
        // 记录每个大类关联的最小 dictSort，用于排序
        Map<String, Integer> categoryMinSort = new HashMap<>();

        for (RemoteDeviceSummaryVo d : inScope) {
            String categoryCode = resolveCategoryCode(d.getDeviceCategory(), dictValueToLabel.keySet());
            String categoryLabel = dictValueToLabel.getOrDefault(categoryCode,
                CATEGORY_OTHER.equals(categoryCode) ? CATEGORY_OTHER_LABEL : categoryCode);

            boolean online = DashboardSensorOnline.isOnline(d, now);
            SfDashboardSensorSummaryItemVo item = new SfDashboardSensorSummaryItemVo();
            item.setDeviceId(d.getDeviceId());
            item.setDeviceCode(d.getDeviceCode());
            item.setDeviceName(d.getDeviceName());
            item.setOnline(online);
            item.setOnlineStatus(DashboardSensorOnline.status(d));

            String productKey = buildProductKey(d.getProductId(), d.getProductName());
            String productName = StringUtils.isNotBlank(d.getProductName())
                ? d.getProductName().trim() : "";

            Map<String, ProductAccum> productMap = categoryProductMap.computeIfAbsent(
                categoryCode, k -> new LinkedHashMap<>());
            ProductAccum acc = productMap.computeIfAbsent(productKey,
                k -> new ProductAccum(d.getProductId(), productName));
            if (online) {
                acc.onlineCount++;
            } else if ("OFFLINE".equals(item.getOnlineStatus())) {
                acc.offlineCount++;
            } else {
                acc.unknownCount++;
            }
            acc.devices.add(item);

            // 记录大类的最小 dictSort
            int sort = dictValueToSort.getOrDefault(categoryCode, Integer.MAX_VALUE);
            categoryMinSort.merge(categoryCode, sort, Math::min);
        }

        // 构建 VO：大类按 dictSort 排序，然后按 categoryCode 兜底
        List<Map.Entry<String, Map<String, ProductAccum>>> sortedCategories =
            categoryProductMap.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Map<String, ProductAccum>>, Integer>
                    comparing(e -> categoryMinSort.getOrDefault(e.getKey(), Integer.MAX_VALUE))
                    .thenComparing(Map.Entry::getKey))
                .toList();

        SfDashboardSensorSummaryVo vo = new SfDashboardSensorSummaryVo();
        for (var entry : sortedCategories) {
            String categoryCode = entry.getKey();
            String categoryLabel = dictValueToLabel.getOrDefault(categoryCode,
                CATEGORY_OTHER.equals(categoryCode) ? CATEGORY_OTHER_LABEL : categoryCode);
            Map<String, ProductAccum> productMap = entry.getValue();

            DeviceCategoryVo catVo = new DeviceCategoryVo();
            catVo.setCategoryCode(categoryCode);
            catVo.setCategoryLabel(categoryLabel);

            long catOnline = 0;
            long catOffline = 0;
            long catUnknown = 0;

            // 产品按 productName 排序
            List<ProductAccum> sortedProducts = productMap.values().stream()
                .sorted(Comparator.comparing(p -> p.productName,
                    Comparator.nullsLast(String::compareTo)))
                .toList();

            for (ProductAccum pa : sortedProducts) {
                // 设备按 deviceName 排序
                pa.devices.sort(Comparator.comparing(SfDashboardSensorSummaryItemVo::getDeviceName,
                    Comparator.nullsLast(String::compareTo)));

                ProductGroupVo pgVo = new ProductGroupVo();
                pgVo.setProductId(pa.productId);
                pgVo.setProductName(StringUtils.isNotBlank(pa.productName) ? pa.productName : "未知产品");
                pgVo.setOnlineCount(pa.onlineCount);
                pgVo.setOfflineCount(pa.offlineCount);
                pgVo.setUnknownCount(pa.unknownCount);
                pgVo.setTotalCount(pa.onlineCount + pa.offlineCount + pa.unknownCount);
                pgVo.setHasOffline(pa.offlineCount > 0);
                pgVo.getDevices().addAll(pa.devices);

                catVo.getProducts().add(pgVo);
                catOnline += pa.onlineCount;
                catOffline += pa.offlineCount;
                catUnknown += pa.unknownCount;
            }

            catVo.setOnlineCount(catOnline);
            catVo.setOfflineCount(catOffline);
            catVo.setUnknownCount(catUnknown);
            catVo.setTotalCount(catOnline + catOffline + catUnknown);
            catVo.setHasOffline(catOffline > 0);

            vo.getCategories().add(catVo);
        }

        return vo;
    }

    @Override
    public SfPlantingBatchDashboardStatsVo getBatchStatusStats() {
        return plantingBatchService.statsForDashboard();
    }

    @Override
    public RemoteSeriesVo getSensorSeries(Long deviceId, String metricCode, Date from, Date to, Integer step) {
        if (deviceId == null) {
            throw new ServiceException("设备ID不能为空");
        }
        if (StringUtils.isBlank(metricCode)) {
            throw new ServiceException("测点编码 metric_code 不能为空");
        }
        assertSensorDeviceAccessible(deviceId);
        return dataPointService.getSeries(deviceId, metricCode, from, to, step);
    }

    private void assertSensorDeviceAccessible(Long deviceId) {
        RemoteDeviceSummaryVo d = iotDeviceService.getDevice(deviceId);
        if (d == null || StringUtils.isBlank(d.getDeviceCode())) {
            throw new ServiceException("设备不存在");
        }
        Set<Long> allowedFieldIds = fieldService.queryList(new SfFieldBo()).stream()
            .map(SfFieldVo::getFieldId)
            .collect(Collectors.toSet());
        if (allowedFieldIds.isEmpty()) {
            throw new ServiceException("无权限访问该设备");
        }
        long n = fieldIotMapper.countNormalByFieldIdsAndDeviceSn(allowedFieldIds, d.getDeviceCode().trim());
        if (n <= 0) {
            throw new ServiceException("设备未绑定到您负责的地块");
        }
    }

    /**
     * 仪表盘传感器汇总用：当前租户下档案状态正常的物联网设备列表（不经地块、sf_field_iot）。
     * <p>走 MyBatis 租户插件，<strong>不</strong>使用 {@link TenantHelper#ignore}，仅查询当前 {@code tenant_id} 下 {@code iot_device}。</p>
     */
    private List<RemoteDeviceSummaryVo> listDashboardIotDevices() {
        RemoteDeviceQueryBo bo = new RemoteDeviceQueryBo();
        bo.setStatus(SystemConstants.NORMAL);
        return iotDeviceService.listDevices(bo);
    }

    private Map<String, RemoteDeviceSummaryVo> loadDevicesBySn(Set<String> sns) {
        if (sns == null || sns.isEmpty()) {
            return Map.of();
        }
        RemoteDeviceQueryBo ibo = new RemoteDeviceQueryBo();
        ibo.setDeviceCodeList(new ArrayList<>(sns));
        // iot_device 视为全局总表（地块可跨租户绑定 SN），跨租户取设备避免落在其它租户时为空。
        List<RemoteDeviceSummaryVo> devs = iotDeviceService.listDevices(ibo);
        return devs.stream()
            .filter(d -> StringUtils.isNotBlank(d.getDeviceCode()))
            .collect(Collectors.toMap(d -> d.getDeviceCode().trim(), Function.identity(), (a, b) -> a));
    }

    /**
     * 将设备类别映射为分类编码：命中字典则返回 dictValue，否则归入 {@link #CATEGORY_OTHER}。
     */
    private static String resolveCategoryCode(String deviceCategory, Set<String> dictValues) {
        if (StringUtils.isBlank(deviceCategory)) {
            return CATEGORY_OTHER;
        }
        String t = deviceCategory.trim();
        return dictValues.contains(t) ? t : CATEGORY_OTHER;
    }

    /**
     * 构建产品分组键：优先使用 productId，若为空则回退到 productName。
     */
    private static String buildProductKey(Long productId, String productName) {
        if (productId != null) {
            return "PID:" + productId;
        }
        String name = StringUtils.isNotBlank(productName) ? productName.trim() : "";
        if (name.isEmpty()) {
            name = "_UNNAMED_";
        }
        return "PN:" + name;
    }

    /**
     * 产品分组聚合累积器（内部使用）。
     */
    private static final class ProductAccum {
        final Long productId;
        final String productName;
        long onlineCount;
        long offlineCount;
        long unknownCount;
        final List<SfDashboardSensorSummaryItemVo> devices = new ArrayList<>();

        ProductAccum(Long productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }
    }
}
