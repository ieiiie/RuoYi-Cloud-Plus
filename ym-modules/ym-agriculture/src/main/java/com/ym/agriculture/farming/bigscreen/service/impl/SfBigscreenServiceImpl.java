package com.ym.agriculture.farming.bigscreen.service.impl;

import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.iot.api.domain.bo.RemoteAlertQueryBo;
import com.ym.iot.api.domain.vo.RemoteAlertRecordVo;
import com.ym.iot.api.domain.bo.RemoteDeviceQueryBo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteLatestTelemetryVo;
import com.ym.iot.api.domain.vo.RemoteFertilizerStateVo;
import com.ym.iot.api.domain.bo.RemoteFertilizerRecordQueryBo;
import com.ym.iot.api.domain.vo.RemoteFertilizerRecordVo;
import com.ym.iot.api.domain.vo.RemoteValveSessionVo;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchDashboardStatsVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
import com.ym.agriculture.farming.bigscreen.config.BigscreenCaliber;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenAlertItemVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenAlertVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenDeviceLayerTabVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenDeviceVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenFertilizerHistoryVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenFertilizerVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenFieldDetailVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenFieldStatsVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenFieldVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenMapFieldVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenMapVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenOnlineRateVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenOverviewVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenRsAnalysisVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenSensorTelemetryVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenTimelineItemVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenUavLatestVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenValveHistoryVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenValveItemVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenValveSummaryVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenWeatherVo;
import com.ym.agriculture.farming.bigscreen.service.ISfBigscreenService;
import com.ym.agriculture.farming.bigscreen.support.BigscreenDeviceLayerMapper;
import com.ym.agriculture.farming.bigscreen.support.BigscreenMapCoordinateSupport;
import com.ym.agriculture.farming.bigscreen.support.BigscreenMapIconResolver;
import com.ym.agriculture.farming.bigscreen.support.BigscreenFarmingTimelineMapper;
import com.ym.agriculture.farming.bigscreen.support.BigscreenFertilizerAssembler;
import com.ym.agriculture.farming.bigscreen.support.BigscreenSatelliteAssembler;
import com.ym.agriculture.farming.bigscreen.support.BigscreenSensorSupport;
import com.ym.agriculture.farming.bigscreen.support.BigscreenSensorTelemetryAssembler;
import com.ym.agriculture.farming.bigscreen.support.BigscreenUavPhotoAssembler;
import com.ym.agriculture.farming.bigscreen.support.BigscreenValveAssembler;
import com.ym.agriculture.farming.bigscreen.support.SfBigscreenIotAccessor;
import com.ym.agriculture.farming.dashboard.service.ISfDashboardService;
import com.ym.agriculture.farming.dashboard.support.DashboardSensorOnline;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordVo;
import com.ym.agriculture.farming.farmrecord.service.ISfFarmingRecordService;
import com.ym.agriculture.farming.field.model.bo.SfFieldBo;
import com.ym.agriculture.farming.field.model.vo.SfFieldBatchInfoVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.agriculture.farming.field.service.ISfFieldService;
import com.ym.agriculture.farming.field.support.SfFieldIotDeviceAccessor;
import com.ym.agriculture.farming.uav.dao.SfUavFlightTaskMapper;
import com.ym.agriculture.farming.uav.dao.SfUavMediaFileMapper;
import com.ym.agriculture.farming.uav.support.UavIotDeviceSupport;
import com.ym.agriculture.farming.uav.model.entity.SfUavFlightTask;
import com.ym.agriculture.farming.uav.model.entity.SfUavMediaFile;
import com.ym.system.api.RemoteTenantService;
import com.ym.system.api.domain.vo.RemoteTenantInfoVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.dubbo.config.annotation.DubboReference;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 大屏 BFF 聚合实现。
 */
@RequiredArgsConstructor
@Service
public class SfBigscreenServiceImpl implements ISfBigscreenService {

    @DubboReference
    private RemoteTenantService tenantService;
    private final ISfFieldService fieldService;
    private final ISfDashboardService dashboardService;
    private final ISfPlantingBatchService plantingBatchService;
    private final SfPlantingBatchMapper plantingBatchMapper;
    private final ISfFarmingRecordService farmingRecordService;
    private final SfUavFlightTaskMapper uavFlightTaskMapper;
    private final SfUavMediaFileMapper uavMediaFileMapper;
    private final SfBigscreenIotAccessor iotAccessor;
    private final SfFieldIotDeviceAccessor iotDeviceAccessor;
    private final BigscreenSensorSupport sensorSupport;
    private final BigscreenSensorTelemetryAssembler sensorTelemetryAssembler;
    private final BigscreenSatelliteAssembler satelliteAssembler;
    private final BigscreenFarmingTimelineMapper farmingTimelineMapper;
    private final BigscreenFertilizerAssembler fertilizerAssembler;
    private final BigscreenValveAssembler valveAssembler;
    private final BigscreenMapIconResolver mapIconResolver;
    private final BigscreenUavPhotoAssembler uavPhotoAssembler;

    @Override
    public SfBigscreenOverviewVo getOverview() {
        SfBigscreenOverviewVo vo = new SfBigscreenOverviewVo();
        vo.setCockpitTitle(BigscreenCaliber.COCKPIT_TITLE);
        vo.setRainForecastMode(BigscreenCaliber.RAIN_FORECAST_MODE);
        String tenantId = TenantHelper.getTenantId();
        vo.setTenantId(tenantId);
        RemoteTenantInfoVo tenant = tenantService.getTenant(tenantId);
        if (tenant != null) {
            vo.setFarmName(tenant.getCompanyName());
            vo.setRegion(resolveRegionLabel(tenant));
        }
        vo.setFieldStats(buildFieldStats());
        vo.setOnlineRate(buildOnlineRate());
        vo.setWeather(dashboardService.getWeatherCard());
        return vo;
    }

    @Override
    public SfBigscreenWeatherVo getWeather() {
        SfBigscreenWeatherVo vo = new SfBigscreenWeatherVo();
        vo.setWeather(dashboardService.getWeatherCard());
        vo.setRainForecastMode(BigscreenCaliber.RAIN_FORECAST_MODE);
        return vo;
    }

    @Override
    public List<SfBigscreenFieldVo> getFields() {
        List<SfFieldVo> fields = fieldService.queryList(new SfFieldBo());
        Map<Long, List<SfPlantingBatchVo>> activeBatchesByFieldId =
            plantingBatchService.mapActiveVoByFieldIds(fields.stream()
                .map(SfFieldVo::getFieldId)
                .filter(java.util.Objects::nonNull)
                .toList());
        List<SfBigscreenFieldVo> result = new ArrayList<>(fields.size());
        for (SfFieldVo field : fields) {
            SfBigscreenFieldVo item = new SfBigscreenFieldVo();
            item.setFieldId(field.getFieldId());
            item.setFieldName(field.getFieldName());
            item.setAreaMu(field.getAreaMu());
            item.setMapDisplayStatus(field.getMapDisplayStatus());
            List<SfPlantingBatchVo> activeBatches = activeBatchesByFieldId.get(field.getFieldId());
            if (activeBatches != null && !activeBatches.isEmpty()) {
                SfPlantingBatchVo activeBatch = activeBatches.get(0);
                item.setActiveBatchId(activeBatch.getBatchId());
                item.setCropSpeciesName(activeBatch.getSpeciesName());
                item.setCropVarietyName(activeBatch.getVarietyName());
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public SfBigscreenFieldDetailVo getFieldDetail(Long fieldId) {
        SfFieldVo field = fieldService.queryById(fieldId);
        if (field == null) {
            throw new ServiceException("地块不存在");
        }
        SfBigscreenFieldDetailVo vo = new SfBigscreenFieldDetailVo();
        vo.setFieldId(field.getFieldId());
        vo.setFieldName(field.getFieldName());
        vo.setAreaMu(field.getAreaMu());
        SfFieldBatchInfoVo batchInfo = fieldService.queryBatchInfoByFieldId(fieldId);
        if (batchInfo != null) {
            vo.setCropSpeciesName(batchInfo.getSpeciesName());
            vo.setCropVarietyName(batchInfo.getVarietyName());
        }
        vo.setTelemetry(getSensorTelemetry(fieldId));
        List<SfBigscreenTimelineItemVo> recent = getFarmOperations(1);
        if (!recent.isEmpty()) {
            vo.setRecentWork(recent.get(0));
        }
        return vo;
    }

    @Override
    public SfBigscreenMapVo getMapData() {
        List<SfFieldVo> fields = fieldService.queryList(new SfFieldBo());
        Map<Long, Long> activeBatchIdsByFieldId = mapLatestActiveBatchIdsByFieldId(fields);
        Date now = new Date();
        SfBigscreenMapVo map = new SfBigscreenMapVo();
        Map<Long, SfFieldVo> fieldById = fields.stream()
            .collect(Collectors.toMap(SfFieldVo::getFieldId, f -> f, (a, b) -> a));
        for (SfFieldVo field : fields) {
            SfBigscreenMapFieldVo plot = new SfBigscreenMapFieldVo();
            plot.setFieldId(field.getFieldId());
            plot.setFieldName(field.getFieldName());
            plot.setBoundaryGeojson(field.getBoundaryGeojson());
            plot.setCenterLng(field.getCenterLng());
            plot.setCenterLat(field.getCenterLat());
            plot.setMapDisplayStatus(field.getMapDisplayStatus());
            plot.setActiveBatchId(activeBatchIdsByFieldId.get(field.getFieldId()));
            map.getFields().add(plot);
        }
        List<RemoteDeviceSummaryVo> devices = iotAccessor.queryTenantNormalDevices();
        Map<Long, BigscreenMapIconResolver.ResolvedIcon> iconMap = mapIconResolver.resolveBatch(devices);
        Map<String, Integer> tabOnline = new HashMap<>();
        Map<String, Integer> tabTotal = new HashMap<>();
        Map<String, Integer> tabOffline = new HashMap<>();
        Map<String, Integer> tabUnknown = new HashMap<>();
        for (RemoteDeviceSummaryVo device : devices) {
            BigscreenMapIconResolver.ResolvedIcon icon = device.getDeviceId() == null
                ? BigscreenMapIconResolver.ResolvedIcon.empty()
                : iconMap.getOrDefault(device.getDeviceId(), BigscreenMapIconResolver.ResolvedIcon.empty());
            SfBigscreenDeviceVo point = toMapDevice(device, now, icon);
            map.getDevices().add(point);
            tabTotal.merge(point.getLayerType(), 1, Integer::sum);
            if (Boolean.TRUE.equals(point.getOnline())) {
                tabOnline.merge(point.getLayerType(), 1, Integer::sum);
            } else if ("OFFLINE".equals(point.getOnlineStatus())) {
                tabOffline.merge(point.getLayerType(), 1, Integer::sum);
            } else {
                tabUnknown.merge(point.getLayerType(), 1, Integer::sum);
            }
        }
        map.setLayerTabs(buildLayerTabs(tabOnline, tabTotal, tabOffline, tabUnknown));
        map.setDefaultFertilizerDeviceCode(devices.stream()
            .filter(BigscreenSensorSupport::isFertilizer)
            .map(RemoteDeviceSummaryVo::getDeviceCode)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .findFirst()
            .orElse(null));
        map.setDefaultUavDeviceCode(devices.stream()
            .filter(UavIotDeviceSupport::isLikelyUavDock)
            .map(RemoteDeviceSummaryVo::getDeviceCode)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .findFirst()
            .orElse(null));
        return map;
    }

    @Override
    public SfBigscreenRsAnalysisVo getRsAnalysis(Long fieldId) {
        SfFieldVo field = fieldService.queryById(fieldId);
        if (field == null) {
            throw new ServiceException("地块不存在");
        }
        Long batchId = plantingBatchMapper.selectActiveBatchIdByFieldId(fieldId);
        SfBigscreenRsAnalysisVo analysis = satelliteAssembler.assemble(
            fieldId, field.getFieldName(), batchId, field.getAreaMu());
        analysis.setCenterLng(field.getCenterLng());
        analysis.setCenterLat(field.getCenterLat());
        return analysis;
    }

    @Override
    public SfBigscreenUavLatestVo getUavLatest() {
        SfBigscreenUavLatestVo vo = new SfBigscreenUavLatestVo();
        SfUavFlightTask task = uavFlightTaskMapper.selectLatestSuccessForBigscreen();
        if (task == null) {
            return vo;
        }
        vo.setTaskId(task.getId());
        vo.setTaskName(task.getTaskName());
        vo.setFieldName(task.getFieldName());
        vo.setCompletedAt(task.getCompletedTime() != null ? task.getCompletedTime() : task.getEndTime());
        String tenantId = TenantHelper.getTenantId();
        String syncJobId = task.getUavJobId();
        List<SfUavMediaFile> media = syncJobId == null ? List.of()
            : uavMediaFileMapper.selectBySyncJobId(tenantId, syncJobId);
        List<SfUavMediaFile> images = media.stream()
            .filter(m -> !"VIDEO".equalsIgnoreCase(m.getFileType()))
            .toList();
        vo.setMediaCount(images.size());
        vo.setPhotos(uavPhotoAssembler.pickPhotos(images));
        return vo;
    }

    @Override
    public SfBigscreenSensorTelemetryVo getSensorTelemetry(Long fieldId) {
        if (fieldId == null) {
            throw new ServiceException("地块ID不能为空");
        }
        List<RemoteDeviceSummaryVo> sensors = sensorSupport.listFieldAgSensors(fieldId);
        Set<Long> ids = sensors.stream().map(RemoteDeviceSummaryVo::getDeviceId).collect(Collectors.toSet());
        Map<Long, RemoteLatestTelemetryVo> latest = ids.isEmpty() ? Map.of() : iotAccessor.getLatestMap(ids);
        return sensorTelemetryAssembler.assemble(fieldId, sensors, latest, new Date());
    }

    @Override
    public SfBigscreenAlertVo getAlerts(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        SfBigscreenAlertVo vo = new SfBigscreenAlertVo();
        vo.setPendingCount(iotAccessor.countAlarming());
        RemoteAlertQueryBo bo = new RemoteAlertQueryBo();
        PageQuery pageQuery = new PageQuery(safeLimit, 1);
        PageResult<RemoteAlertRecordVo> page = iotAccessor.queryAlertPage(bo, pageQuery);
        if (page.getRows() != null) {
            for (RemoteAlertRecordVo row : page.getRows()) {
                vo.getItems().add(toAlertItem(row));
            }
        }
        return vo;
    }

    @Override
    public List<SfBigscreenTimelineItemVo> getFarmOperations(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        PageQuery pageQuery = new PageQuery(safeLimit, 1);
        PageResult<SfFarmingRecordVo> page = farmingRecordService.pageSubmittedForAdmin(
            null, null, null, null, null, pageQuery);
        return farmingTimelineMapper.toTimelineItems(new ArrayList<>(page.getRows()));
    }

    @Override
    public SfBigscreenFertilizerVo getFertilizer(String deviceCode) {
        if (StringUtils.isBlank(deviceCode)) {
            throw new ServiceException("设备编号不能为空");
        }
        RemoteDeviceSummaryVo requested = findDeviceByCode(deviceCode.trim());
        if (requested == null) {
            throw new ServiceException("设备不存在");
        }
        RemoteDeviceSummaryVo device = resolveFertilizerTarget(requested);
        if (device == null) {
            return buildEmptyFertilizerVo(requested);
        }
        Date now = new Date();
        SfBigscreenFertilizerVo vo = new SfBigscreenFertilizerVo();
        vo.setDeviceCode(device.getDeviceCode());
        vo.setDeviceName(device.getDeviceName());
        vo.setProductKey(device.getProductKey());
        vo.setOnline(DashboardSensorOnline.isOnline(device, now));
        vo.setOnlineStatus(DashboardSensorOnline.status(device));
        RemoteFertilizerStateVo memorySnap = null;
        if (device.getDeviceId() != null) {
            memorySnap = iotAccessor.getFertilizerState(device.getDeviceId());
            if (Boolean.TRUE.equals(vo.getOnline()) && memorySnap != null && Boolean.TRUE.equals(memorySnap.getOnline())
                && "RUNNING".equals(memorySnap.getState())) {
                vo.setFertilizing(true);
            } else {
                vo.setFertilizing(false);
            }
        }
        RemoteLatestTelemetryVo latestTelemetry = null;
        if (device.getDeviceId() != null) {
            Map<Long, RemoteLatestTelemetryVo> latestMap = iotAccessor.getLatestMap(List.of(device.getDeviceId()));
            latestTelemetry = latestMap.get(device.getDeviceId());
        }
        RemoteFertilizerStateVo tankSnap = fertilizerAssembler.resolveTankSnapshot(memorySnap, latestTelemetry);
        vo.setTanks(fertilizerAssembler.buildTanks(tankSnap));
        RemoteFertilizerRecordQueryBo bo = new RemoteFertilizerRecordQueryBo();
        bo.setDeviceId(device.getDeviceId());
        List<RemoteFertilizerRecordVo> records = queryFertilizerRecordsForDevice(
            device, bo, BigscreenCaliber.FERTILIZER_RAW_RECORD_FETCH);
        vo.setRecentRecords(fertilizerAssembler.buildDailyRecords(
            records, BigscreenCaliber.FERTILIZER_RECENT_DAY_LIMIT));
        return vo;
    }

    @Override
    public SfBigscreenValveSummaryVo getValveSummary() {
        Date now = new Date();
        SfBigscreenValveSummaryVo vo = new SfBigscreenValveSummaryVo();
        List<RemoteDeviceSummaryVo> valves = iotAccessor.listValveDevices(null);
        if (valves.isEmpty()) {
            valves = iotAccessor.queryTenantNormalDevices().stream()
                .filter(BigscreenSensorSupport::isMotorValve)
                .toList();
        }
        List<Long> deviceIds = valves.stream()
            .map(RemoteDeviceSummaryVo::getDeviceId)
            .filter(id -> id != null)
            .toList();
        Map<Long, RemoteLatestTelemetryVo> latestMap = deviceIds.isEmpty()
            ? Map.of()
            : iotAccessor.getLatestMap(deviceIds);
        Map<Long, String> channelTagMap = deviceIds.isEmpty()
            ? Map.of()
            : iotAccessor.getChannelTagMap(deviceIds);
        Map<Long, RemoteDeviceSummaryVo> deviceById = valves.stream()
            .filter(v -> v.getDeviceId() != null)
            .collect(Collectors.toMap(RemoteDeviceSummaryVo::getDeviceId, v -> v, (a, b) -> a));
        List<RemoteValveSessionVo> openSessions = iotAccessor.listOpenValveSessions();
        if (openSessions == null) {
            openSessions = List.of();
        }
        List<RemoteValveSessionVo> effectiveOpenSessions = valveAssembler.filterEffectiveOpenSessions(
            openSessions, deviceById, latestMap, channelTagMap, now);

        int online = 0;
        int openCount = 0;
        boolean irrigating = false;
        for (RemoteDeviceSummaryVo valve : valves) {
            SfBigscreenValveItemVo item = new SfBigscreenValveItemVo();
            item.setDeviceId(valve.getDeviceId());
            item.setDeviceCode(valve.getDeviceCode());
            item.setDeviceName(valve.getDeviceName());
            item.setProductKey(valve.getProductKey());
            boolean isOnline = DashboardSensorOnline.isOnline(valve, now);
            item.setOnline(isOnline);
            item.setOnlineStatus(DashboardSensorOnline.status(valve));
            if (isOnline) {
                online++;
            }
            RemoteLatestTelemetryVo latest = valve.getDeviceId() == null ? null : latestMap.get(valve.getDeviceId());
            BigDecimal angleDeg = valveAssembler.resolveAngle(latest, BigscreenValveAssembler.DEFAULT_VALVE_NO);
            BigDecimal flowRateM3h = valveAssembler.resolveFlowRate(latest);
            String channelTagValue = valve.getDeviceId() == null
                ? null
                : channelTagMap.get(valve.getDeviceId());
            boolean summaryOpen = valveAssembler.isDeviceOpen(
                isOnline, valve, latest, channelTagValue, effectiveOpenSessions);
            boolean valveIrrigating = valveAssembler.isIrrigating(isOnline, flowRateM3h);
            item.setAngleDeg(angleDeg);
            item.setFlowRateM3h(flowRateM3h);
            item.setOpen(summaryOpen);
            item.setIrrigating(valveIrrigating);
            item.setSwitchStatus(valveAssembler.resolveDeviceSwitchStatus(
                isOnline, valve, latest, channelTagValue, effectiveOpenSessions));
            if (summaryOpen) {
                openCount++;
            }
            if (valveIrrigating) {
                irrigating = true;
            }
            vo.getValves().add(item);
        }
        int totalCount = vo.getValves().size();
        vo.setTotalCount(totalCount);
        vo.setOnlineCount(online);
        DashboardSensorOnline.Counts valveStates = DashboardSensorOnline.count(valves);
        vo.setOfflineCount(valveStates.offline());
        vo.setUnknownCount(valveStates.unknown());
        vo.setOpenCount(openCount);
        vo.setIrrigating(irrigating);

        vo.setMaxOpenDurationSeconds(
            valveAssembler.resolveMaxOpenDurationSeconds(effectiveOpenSessions, deviceById, now));

        List<RemoteValveSessionVo> endedSessions = iotAccessor.queryRecentEndedSessions(
            BigscreenCaliber.VALVE_CLOSED_SESSION_FETCH_SIZE);
        List<SfBigscreenValveHistoryVo> recentHistory = valveAssembler.buildRecentHistory(
            endedSessions, effectiveOpenSessions, deviceById, latestMap, now,
            BigscreenCaliber.VALVE_RECENT_HISTORY_LIMIT);
        vo.setRecentHistory(recentHistory);
        return vo;
    }

    private SfBigscreenFieldStatsVo buildFieldStats() {
        List<SfFieldVo> fields = fieldService.queryList(new SfFieldBo());
        SfBigscreenFieldStatsVo stats = new SfBigscreenFieldStatsVo();
        stats.setFieldCount(fields.size());
        BigDecimal totalArea = fields.stream()
            .map(SfFieldVo::getAreaMu)
            .filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalAreaMu(totalArea);
        SfPlantingBatchDashboardStatsVo batchStats = plantingBatchService.statsForDashboard();
        long batchCount = batchStats.getPlanningCount() + batchStats.getPlantingCount()
            + batchStats.getGrowingCount() + batchStats.getHarvestingCount()
            + batchStats.getFinishedCount() + batchStats.getFailedCount();
        stats.setBatchCount(batchCount);
        Map<Long, List<SfPlantingBatchVo>> activeBatchesByFieldId =
            plantingBatchService.mapActiveVoByFieldIds(fields.stream()
                .map(SfFieldVo::getFieldId)
                .filter(java.util.Objects::nonNull)
                .toList());
        for (SfFieldVo field : fields) {
            List<SfPlantingBatchVo> activeBatches = activeBatchesByFieldId.get(field.getFieldId());
            SfPlantingBatchVo batch = activeBatches == null || activeBatches.isEmpty()
                ? null : activeBatches.get(0);
            if (batch != null && StringUtils.isNotBlank(batch.getSpeciesName())) {
                stats.setPrimaryCropName(batch.getSpeciesName());
                stats.setPrimaryVarietyName(batch.getVarietyName());
                break;
            }
        }
        return stats;
    }

    /** 大屏地图只需要批次 ID，批量取数后保留每块地批次 ID 最大的一条。 */
    private Map<Long, Long> mapLatestActiveBatchIdsByFieldId(List<SfFieldVo> fields) {
        List<Long> fieldIds = fields.stream()
            .map(SfFieldVo::getFieldId)
            .filter(java.util.Objects::nonNull)
            .toList();
        if (fieldIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        for (SfPlantingBatchVo batch : plantingBatchMapper.selectActiveByFieldIds(fieldIds)) {
            if (batch.getFieldId() != null && batch.getBatchId() != null) {
                result.putIfAbsent(batch.getFieldId(), batch.getBatchId());
            }
        }
        return result;
    }

    private SfBigscreenOnlineRateVo buildOnlineRate() {
        List<RemoteDeviceSummaryVo> devices = iotAccessor.queryTenantNormalDevices();
        DashboardSensorOnline.Counts counts = DashboardSensorOnline.count(devices);
        SfBigscreenOnlineRateVo rate = new SfBigscreenOnlineRateVo();
        rate.setScope(BigscreenCaliber.ONLINE_RATE_SCOPE);
        rate.setOnline(counts.online());
        rate.setTotal(counts.total());
        rate.setOffline(counts.offline());
        rate.setUnknown(counts.unknown());
        rate.setConfirmed(counts.confirmed());
        rate.setPct(counts.pct());
        return rate;
    }

    private String resolveRegionLabel(RemoteTenantInfoVo tenant) {
        if (tenant == null) {
            return null;
        }
        if (StringUtils.isNotBlank(tenant.getRegionName())) {
            String name = tenant.getRegionName().trim();
            int idx = name.lastIndexOf(' ');
            return idx >= 0 ? name.substring(idx + 1).trim() : name;
        }
        return tenant.getAddress();
    }

    private SfBigscreenDeviceVo toMapDevice(RemoteDeviceSummaryVo device, Date now,
                                            BigscreenMapIconResolver.ResolvedIcon icon) {
        SfBigscreenDeviceVo vo = new SfBigscreenDeviceVo();
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceCode(device.getDeviceCode());
        vo.setDeviceName(device.getDeviceName());
        vo.setProductKey(device.getProductKey());
        vo.setDeviceCategory(device.getDeviceCategory());
        vo.setLayerType(BigscreenDeviceLayerMapper.resolveLayerType(device));
        if (BigscreenDeviceLayerMapper.LAYER_SENSOR.equals(vo.getLayerType())) {
            vo.setSensorSubType(BigscreenSensorSupport.resolveSensorSubType(device));
        }
        vo.setVirtualZone(BigscreenDeviceLayerMapper.resolveVirtualZone(device));
        vo.setVirtualZoneLabel(BigscreenDeviceLayerMapper.resolveVirtualZoneLabel(vo.getVirtualZone()));
        BigscreenMapCoordinateSupport.Coordinate coordinate =
            BigscreenMapCoordinateSupport.resolve(device.getLng(), device.getLat());
        vo.setLng(coordinate.lng());
        vo.setLat(coordinate.lat());
        vo.setOnline(DashboardSensorOnline.isOnline(device, now));
        vo.setOnlineStatus(DashboardSensorOnline.status(device));
        if (icon != null) {
            vo.setMapIconUrl(icon.mapIconUrl());
            vo.setMapSelectedIconUrl(icon.mapSelectedIconUrl());
        }
        return vo;
    }

    private List<SfBigscreenDeviceLayerTabVo> buildLayerTabs(Map<String, Integer> online,
                                                             Map<String, Integer> total,
                                                             Map<String, Integer> offline,
                                                             Map<String, Integer> unknown) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(BigscreenDeviceLayerMapper.LAYER_SENSOR, "传感器");
        labels.put(BigscreenDeviceLayerMapper.LAYER_CAMERA, "摄像头");
        labels.put(BigscreenDeviceLayerMapper.LAYER_VALVE, "阀门");
        labels.put(BigscreenDeviceLayerMapper.LAYER_FACILITY, "设施");
        labels.put(BigscreenDeviceLayerMapper.LAYER_UAV, "无人机");
        List<SfBigscreenDeviceLayerTabVo> tabs = new ArrayList<>();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            String type = entry.getKey();
            SfBigscreenDeviceLayerTabVo tab = new SfBigscreenDeviceLayerTabVo();
            tab.setLayerType(type);
            tab.setLayerLabel(entry.getValue());
            tab.setTotalCount(total.getOrDefault(type, 0));
            tab.setOnlineCount(online.getOrDefault(type, 0));
            tab.setOfflineCount(offline.getOrDefault(type, 0));
            tab.setUnknownCount(unknown.getOrDefault(type, 0));
            tabs.add(tab);
        }
        return tabs;
    }

    private SfBigscreenAlertItemVo toAlertItem(RemoteAlertRecordVo row) {
        SfBigscreenAlertItemVo item = new SfBigscreenAlertItemVo();
        item.setId(row.getId());
        item.setAlarmConfigId(row.getAlarmConfigId());
        item.setName(row.getName());
        item.setLevel(row.getLevel());
        item.setState(row.getState());
        item.setAlarmTime(row.getAlarmTime());
        item.setLastAlarmTime(row.getLastAlarmTime());
        item.setHandleTime(row.getHandleTime());
        item.setTargetId(row.getTargetId());
        item.setTargetName(row.getTargetName());
        item.setTargetType(row.getTargetType());
        item.setActualDesc(row.getActualDesc());
        item.setHandleType(row.getHandleType());
        item.setHandleState(row.getHandleState());
        return item;
    }

    private RemoteDeviceSummaryVo findDeviceByCode(String deviceCode) {
        RemoteDeviceQueryBo bo = new RemoteDeviceQueryBo();
        bo.setDeviceCode(deviceCode);
        bo.setStatus(SystemConstants.NORMAL);
        List<RemoteDeviceSummaryVo> devices = TenantHelper.ignore(() -> iotDeviceAccessor.queryList(bo));
        return devices == null || devices.isEmpty() ? null : devices.get(0);
    }

    /**
     * 解析水肥机详情目标设备：误传机场等非施肥机 SN 时回落租户第一台施肥机。
     *
     * @return 施肥机档案；租户无水肥机且请求为机场类设备时返回 null（由调用方返回空壳）
     */
    private RemoteDeviceSummaryVo resolveFertilizerTarget(RemoteDeviceSummaryVo requested) {
        if (BigscreenSensorSupport.isFertilizer(requested)) {
            return requested;
        }
        RemoteDeviceSummaryVo fallback = findFirstTenantFertilizer();
        if (fallback != null) {
            return fallback;
        }
        if (UavIotDeviceSupport.isLikelyUavDock(requested)) {
            return null;
        }
        throw new ServiceException("设备不是施肥机");
    }

    private RemoteDeviceSummaryVo findFirstTenantFertilizer() {
        return iotAccessor.queryTenantNormalDevices().stream()
            .filter(BigscreenSensorSupport::isFertilizer)
            .findFirst()
            .orElse(null);
    }

    private static SfBigscreenFertilizerVo buildEmptyFertilizerVo(RemoteDeviceSummaryVo requested) {
        SfBigscreenFertilizerVo vo = new SfBigscreenFertilizerVo();
        vo.setDeviceCode(requested.getDeviceCode());
        vo.setDeviceName(requested.getDeviceName());
        vo.setProductKey(requested.getProductKey());
        vo.setOnline(false);
        vo.setOnlineStatus("UNKNOWN");
        vo.setFertilizing(false);
        return vo;
    }

    /**
     * 按设备档案租户查询施肥流水（与 MQTT 入库租户一致）。
     * <p>
     * 仅按 {@code device_id} 过滤：流水表 {@code device_code} 为 MQTT 帧内裸码（如 {@code yx250110}），
     * 与档案 {@code pfyx250110} 可能不一致，不可与 {@code device_id} 做 AND。
     */
    private List<RemoteFertilizerRecordVo> queryFertilizerRecordsForDevice(RemoteDeviceSummaryVo device,
                                                                       RemoteFertilizerRecordQueryBo bo,
                                                                       int limit) {
        if (device == null || bo == null || bo.getDeviceId() == null) {
            return List.of();
        }
        String tenantId = device.getTenantId();
        if (StringUtils.isNotBlank(tenantId)) {
            List<RemoteFertilizerRecordVo> rows = TenantHelper.dynamic(
                tenantId.trim(), () -> iotAccessor.queryFertilizerRecords(bo, limit));
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        return TenantHelper.ignore(() -> iotAccessor.queryFertilizerRecords(bo, limit));
    }
}
