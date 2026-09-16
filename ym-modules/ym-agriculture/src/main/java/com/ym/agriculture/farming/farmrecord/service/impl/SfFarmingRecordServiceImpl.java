package com.ym.agriculture.farming.farmrecord.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.batch.model.constants.PlantingBatchStatus;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.support.GrowthStageConfigSupport;
import com.ym.agriculture.farming.crop.support.SfTaskCropSnapshot;
import com.ym.agriculture.farming.crop.support.SfTaskCropSnapshotFiller;
import com.ym.agriculture.farming.farmrecord.dao.SfFarmingRecordFieldMapper;
import com.ym.agriculture.farming.farmrecord.dao.SfFarmingRecordMapper;
import com.ym.agriculture.farming.farmrecord.dao.SfFarmingRecordMediaMapper;
import com.ym.agriculture.farming.farmrecord.dao.SfFarmingRecordWorkItemMapper;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordMediaGeoPatchBo;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordMediaItemBo;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordSaveBo;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordWorkItemBo;
import com.ym.agriculture.farming.farmrecord.model.constant.FarmingMediaKind;
import com.ym.agriculture.farming.farmrecord.model.constant.FarmingRecordStatus;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecord;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecordField;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecordMedia;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecordWorkItem;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingGrowthStagePreviewVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingLatestRecordImagesVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordFieldVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordMediaVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordWorkItemVo;
import com.ym.agriculture.farming.farmrecord.service.ISfFarmingRecordService;
import com.ym.agriculture.farming.farmrecord.support.FarmingCustomFormSupport;
import com.ym.agriculture.farming.farmrecord.support.FarmingFieldSensorSnapshotBuilder;
import com.ym.agriculture.farming.farmrecord.support.SfFarmingFieldGate;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.field.support.SfFieldMasterDictAccessor;
import com.ym.agriculture.farming.field.support.SfFieldMasterUserAccessor;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.constants.FarmWorkNodeType;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farming.solarterms.service.ISfSolarTermService;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherForecastVo;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherLatestVo;
import com.ym.agriculture.farming.weather.service.ISfWeatherService;
import com.ym.system.api.domain.vo.RemoteUserVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 移动端农事记录服务实现。
 *
 * @author ym-cloud
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SfFarmingRecordServiceImpl implements ISfFarmingRecordService {

    private static final int LIST_PREVIEW_MEDIA_MAX = 4;
    private static final int LATEST_RECORD_IMAGE_SCAN_DEPTH = 15;
    private static final int LATEST_RECORD_IMAGE_DEFAULT_LIMIT = 9;
    private static final int LATEST_RECORD_IMAGE_MAX_LIMIT = 20;
    private static final String DICT_MACHINE_TYPE = "sf_farming_machine_type";
    private static final String DICT_MATERIAL_NAME = "sf_farming_material_name";
    private static final String WEATHER_SOURCE_FORECAST = "FORECAST";
    private static final String WEATHER_SOURCE_MANUAL = "MANUAL";
    private static final String WEATHER_SOURCE_NONE = "NONE";
    private static final String TEMP_UNIT_CELSIUS = "℃";
    private static final String FEEDBACK_PLANT_HEIGHT = "plantHeight";
    private static final String FEEDBACK_PLANT_HEIGHT_UNIT = "plantHeightUnit";
    private static final String PLANT_HEIGHT_UNIT_CM = "cm";
    private static final String WORK_PERIOD_PATTERN_MINUTE = "yyyy-MM-dd HH:mm";
    private static final String WORK_PERIOD_PATTERN_SECOND = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter WORK_PERIOD_MINUTE_FORMATTER = DateTimeFormatter.ofPattern(WORK_PERIOD_PATTERN_MINUTE);
    private static final DateTimeFormatter WORK_PERIOD_SECOND_FORMATTER = DateTimeFormatter.ofPattern(WORK_PERIOD_PATTERN_SECOND);
    private static final Duration WORK_PERIOD_MAX_DURATION = Duration.ofHours(24);

    private final SfFarmingRecordMapper farmingRecordMapper;
    private final SfFarmingRecordFieldMapper recordFieldMapper;
    private final SfFarmingRecordWorkItemMapper recordWorkItemMapper;
    private final SfFarmingRecordMediaMapper mediaMapper;
    private final SfFieldMapper fieldMapper;
    private final SfPlantingBatchMapper plantingBatchMapper;
    private final SfFarmWorkDictMapper farmWorkDictMapper;
    private final SfFieldMasterDictAccessor masterDictAccessor;
    private final SfFieldMasterUserAccessor masterUserAccessor;
    private final ISfWeatherService weatherService;
    private final SfFarmingFieldGate fieldGate;
    private final FarmingFieldSensorSnapshotBuilder sensorSnapshotBuilder;
    private final SfTaskCropSnapshotFiller cropSnapshotFiller;
    private final SfCropSpeciesMapper cropSpeciesMapper;
    @Autowired
    private ISfSolarTermService solarTermService;

    @Override
    public PageResult<SfFarmingRecordVo> pageForMobile(Long fieldId,
                                                          Long workItemId,
                                                          Long categoryId,
                                                          String status,
                                                          Date fromHappenedAt,
                                                          Date toHappenedAt,
                                                          PageQuery pageQuery) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        Set<Long> recordIdScope = buildRecordIdScope(tenantId, fieldId, workItemId, categoryId);

        Page<SfFarmingRecord> p = pageQuery.build();
        Page<SfFarmingRecord> page = farmingRecordMapper.selectPage(
            p,
            farmingRecordMapper.wrapMobileScope(tenantId, userId, recordIdScope, status, fromHappenedAt, toHappenedAt)
                .orderByDesc(SfFarmingRecord::getHappenedAt)
                .orderByDesc(SfFarmingRecord::getRecordId)
        );
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(buildListVoPage(tenantId, page));
    }

    @Override
    public SfFarmingLatestRecordImagesVo getLatestRecordImages(Long fieldId, Integer imageLimit) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        int maxImages = resolveLatestImageLimit(imageLimit);
        Set<Long> recordIdScope = buildRecordIdScope(tenantId, fieldId, null, null);
        if (recordIdScope != null && recordIdScope.isEmpty()) {
            return new SfFarmingLatestRecordImagesVo();
        }

        Page<SfFarmingRecord> page = new Page<>(1, LATEST_RECORD_IMAGE_SCAN_DEPTH);
        List<SfFarmingRecord> candidates = farmingRecordMapper.selectPage(
            page,
            farmingRecordMapper.wrapMobileScope(tenantId, userId, recordIdScope, FarmingRecordStatus.SUBMITTED, null, null)
                .orderByDesc(SfFarmingRecord::getSubmitTime)
                .orderByDesc(SfFarmingRecord::getRecordId)
        ).getRecords();

        for (SfFarmingRecord record : candidates) {
            List<SfFarmingRecordMediaVo> images = mediaMapper.selectByRecordId(tenantId, record.getRecordId()).stream()
                .filter(media -> FarmingMediaKind.IMAGE.equalsIgnoreCase(media.getKind()))
                .limit(maxImages)
                .map(media -> MapstructUtils.convert(media, SfFarmingRecordMediaVo.class))
                .toList();
            if (CollUtil.isEmpty(images)) {
                continue;
            }
            SfFarmingLatestRecordImagesVo vo = new SfFarmingLatestRecordImagesVo();
            vo.setRecordId(record.getRecordId());
            vo.setHappenedAt(record.getHappenedAt());
            vo.setSubmitTime(record.getSubmitTime());
            vo.setSummary(record.getSummary());
            vo.setFields(recordFieldMapper.selectByRecordId(tenantId, record.getRecordId()).stream()
                .map(this::toFieldVo)
                .toList());
            vo.setImages(images);
            return vo;
        }
        return new SfFarmingLatestRecordImagesVo();
    }

    @Override
    public PageResult<SfFarmingRecordVo> pageSubmittedForAdmin(Long fieldId,
                                                                  Long workItemId,
                                                                  Long categoryId,
                                                                  Date fromHappenedAt,
                                                                  Date toHappenedAt,
                                                                  PageQuery pageQuery) {
        String tenantId = requireTenantId();
        Set<Long> recordIdScope = buildRecordIdScope(tenantId, fieldId, workItemId, categoryId);
        Page<SfFarmingRecord> page = farmingRecordMapper.selectPage(
            pageQuery.build(),
            farmingRecordMapper.wrapAdminSubmittedScope(tenantId, recordIdScope, fromHappenedAt, toHappenedAt)
                .orderByDesc(SfFarmingRecord::getHappenedAt)
                .orderByDesc(SfFarmingRecord::getRecordId)
        );
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(buildListVoPage(tenantId, page));
    }

    private static int resolveLatestImageLimit(Integer imageLimit) {
        if (imageLimit == null || imageLimit <= 0) {
            return LATEST_RECORD_IMAGE_DEFAULT_LIMIT;
        }
        return Math.min(imageLimit, LATEST_RECORD_IMAGE_MAX_LIMIT);
    }

    private Set<Long> buildRecordIdScope(String tenantId, Long fieldId, Long workItemId, Long categoryId) {
        Set<Long> recordIdScope = null;
        if (fieldId != null) {
            fieldGate.assertAccessible(fieldId, tenantId);
            recordIdScope = intersect(recordIdScope, recordFieldMapper.selectRecordIdsByFieldId(tenantId, fieldId));
        }
        if (workItemId != null) {
            recordIdScope = intersect(recordIdScope, recordWorkItemMapper.selectRecordIdsByWorkItemId(tenantId, workItemId));
        }
        if (categoryId != null) {
            recordIdScope = intersect(recordIdScope, recordWorkItemMapper.selectRecordIdsByCategoryId(tenantId, categoryId));
        }
        return recordIdScope;
    }

    @Override
    public Map<String, Object> buildSensorPreview(List<Long> fieldIds) {
        String tenantId = requireTenantId();
        List<Long> ids = normalizeIds(fieldIds, "地块不能为空");
        ids.forEach(id -> fieldGate.assertAccessible(id, tenantId));
        return buildSensorSnapshotPayload(ids);
    }

    @Override
    public String buildSensorPreviewJson(List<Long> fieldIds) {
        return JSON.toJSONString(buildSensorPreview(fieldIds));
    }

    @Override
    public Map<String, Object> buildSensorSimplePreview(List<Long> fieldIds) {
        return buildSensorPreview(fieldIds);
    }

    @Override
    public Map<String, Object> buildWeatherPreview(Date happenedAt) {
        return buildForecastWeatherSnapshot(happenedAt, true);
    }

    @Override
    public SfFarmingGrowthStagePreviewVo buildGrowthStagePreview(List<Long> fieldIds, Date happenedAt) {
        String tenantId = requireTenantId();
        List<Long> ids = normalizeIds(fieldIds, "地块不能为空");
        List<SfField> fields = loadAndValidateFields(ids, tenantId);
        SfField firstField = fields.stream()
            .filter(field -> Objects.equals(field.getFieldId(), ids.get(0)))
            .findFirst()
            .orElse(fields.get(0));
        SfFarmingGrowthStagePreviewVo vo = new SfFarmingGrowthStagePreviewVo();
        vo.setManual(false);
        vo.setMissingFieldIds(List.of());
        vo.setMissingFieldNames(List.of());

        SfPlantingBatch batch = loadActiveBatchByField(List.of(firstField.getFieldId())).get(firstField.getFieldId());
        if (batch == null) {
            return unknownGrowthStage(firstField);
        }
        SfTaskCropSnapshot snap = cropSnapshotFiller.resolve(firstField.getFieldId(), batch.getBatchId(), tenantId, null);
        if (snap == null || snap.getSpeciesId() == null) {
            return unknownGrowthStage(firstField);
        }
        SfCropSpecies species = cropSpeciesMapper.selectById(snap.getSpeciesId());
        if (species == null || !SystemConstants.NORMAL.equals(species.getDelFlag())) {
            return unknownGrowthStage(firstField);
        }
        List<Map<String, Object>> stages = GrowthStageConfigSupport.listStages(species.getGrowthStageConfigJson());
        Map<String, Object> stage = GrowthStageConfigSupport.findDefaultStage(species.getGrowthStageConfigJson());
        if (stage.isEmpty()) {
            return unknownGrowthStage(firstField);
        }
        vo.setStages(stages);
        vo.setCode(objectToString(stage.get("code")));
        vo.setName(objectToString(stage.get("name")));
        vo.setReliable(true);
        if (StringUtils.isBlank(vo.getCode()) || StringUtils.isBlank(vo.getName())) {
            return unknownGrowthStage(firstField);
        }
        return vo;
    }

    private SfFarmingGrowthStagePreviewVo unknownGrowthStage(SfField field) {
        SfFarmingGrowthStagePreviewVo vo = new SfFarmingGrowthStagePreviewVo();
        vo.setCode("UNKNOWN");
        vo.setName("未配置生长阶段");
        vo.setReliable(false);
        vo.setManual(false);
        if (field == null) {
            vo.setMissingFieldIds(List.of());
            vo.setMissingFieldNames(List.of());
            return vo;
        }
        vo.setMissingFieldIds(List.of(field.getFieldId()));
        vo.setMissingFieldNames(List.of(field.getFieldName()));
        return vo;
    }

    @Override
    public SfFarmingRecordVo getDetail(Long recordId) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        SfFarmingRecord record = loadVisible(recordId, tenantId, userId);
        return toVo(record, true);
    }

    @Override
    public SfFarmingRecordVo getSubmittedDetailForAdmin(Long recordId) {
        String tenantId = requireTenantId();
        SfFarmingRecord record = farmingRecordMapper.selectById(recordId);
        if (record == null || !SystemConstants.NORMAL.equals(record.getDelFlag())) {
            throw new ServiceException("农事记录不存在");
        }
        if (!tenantId.equals(record.getTenantId())) {
            throw new ServiceException("无权查看该记录");
        }
        if (!FarmingRecordStatus.SUBMITTED.equals(record.getStatus())) {
            throw new ServiceException("农事记录不存在");
        }
        SfFarmingRecordVo vo = toVo(record, true);
        enrichAdminFieldCrop(tenantId, vo.getFields());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addSubmittedForAdmin(SfFarmingRecordSaveBo bo) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        Long recordId = IdWorker.getId();
        Date now = new Date();
        PersistPayload payload = preparePayload(recordId, tenantId, bo, true, Map.of(), true);

        SfFarmingRecord record = new SfFarmingRecord();
        record.setRecordId(recordId);
        record.setTenantId(tenantId);
        record.setStatus(FarmingRecordStatus.SUBMITTED);
        record.setSubmitTime(now);
        fillRecord(record, bo, payload.sensorSnapshotJson(), payload.fields());
        record.setDelFlag(SystemConstants.NORMAL);
        record.setCreateBy(userId);
        record.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        record.setUpdateBy(userId);
        record.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        farmingRecordMapper.insert(record);
        replaceFields(tenantId, recordId, payload.fields());
        replaceWorkItems(tenantId, recordId, payload.workItems());
        replaceMedia(tenantId, recordId, bo.getMedia());
        return recordId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSubmittedForAdmin(Long recordId, SfFarmingRecordSaveBo bo) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        SfFarmingRecord old = LoginHelper.isSuperAdmin()
            ? farmingRecordMapper.selectSubmittedInTenant(recordId, tenantId)
            : farmingRecordMapper.selectSubmittedOwned(recordId, tenantId, userId);
        if (old == null) {
            throw new ServiceException("已提交记录不存在或无权修改");
        }
        PersistPayload payload = preparePayload(recordId, tenantId, bo, true,
            loadExistingWorkItemSnapshotMap(tenantId, recordId), old.getSensorSnapshotJson());
        fillRecord(old, bo, old.getSensorSnapshotJson(), payload.fields());
        old.setUpdateBy(userId);
        old.setUpdateTime(LocalDateTime.now());
        farmingRecordMapper.updateById(old);
        replaceFields(tenantId, recordId, payload.fields());
        replaceWorkItems(tenantId, recordId, payload.workItems());
        replaceMedia(tenantId, recordId, bo.getMedia());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeSubmittedForAdmin(Long recordId) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        SfFarmingRecord old = farmingRecordMapper.selectSubmittedOwned(recordId, tenantId, userId);
        if (old == null) {
            throw new ServiceException("已提交记录不存在或无权删除");
        }
        removeRecordChildren(tenantId, recordId);
        farmingRecordMapper.deleteById(recordId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addDraft(SfFarmingRecordSaveBo bo) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        Long recordId = IdWorker.getId();
        Date now = new Date();
        PersistPayload payload = preparePayload(recordId, tenantId, bo, false, Map.of());

        SfFarmingRecord record = new SfFarmingRecord();
        record.setRecordId(recordId);
        record.setTenantId(tenantId);
        record.setStatus(FarmingRecordStatus.DRAFT);
        fillRecord(record, bo, payload.sensorSnapshotJson(), payload.fields());
        record.setDelFlag(SystemConstants.NORMAL);
        record.setCreateBy(userId);
        record.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        record.setUpdateBy(userId);
        record.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        farmingRecordMapper.insert(record);
        replaceFields(tenantId, recordId, payload.fields());
        replaceWorkItems(tenantId, recordId, payload.workItems());
        replaceMedia(tenantId, recordId, bo.getMedia());
        return recordId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDraft(Long recordId, SfFarmingRecordSaveBo bo) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        SfFarmingRecord old = farmingRecordMapper.selectDraftOwned(recordId, tenantId, userId);
        if (old == null) {
            throw new ServiceException("草稿不存在或无权修改");
        }
        PersistPayload payload = preparePayload(recordId, tenantId, bo, false, loadExistingWorkItemSnapshotMap(tenantId, recordId));
        fillRecord(old, bo, payload.sensorSnapshotJson(), payload.fields());
        old.setUpdateBy(userId);
        old.setUpdateTime(LocalDateTime.now());
        farmingRecordMapper.updateById(old);
        replaceFields(tenantId, recordId, payload.fields());
        replaceWorkItems(tenantId, recordId, payload.workItems());
        replaceMedia(tenantId, recordId, bo.getMedia());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(Long recordId) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        SfFarmingRecord old = farmingRecordMapper.selectDraftOwned(recordId, tenantId, userId);
        if (old == null) {
            throw new ServiceException("草稿不存在或无权提交");
        }
        if (CollUtil.isEmpty(recordWorkItemMapper.selectByRecordId(tenantId, recordId))) {
            throw new ServiceException("农事项目不能为空");
        }
        List<Long> fieldIds = recordFieldMapper.selectByRecordId(tenantId, recordId).stream()
            .map(SfFarmingRecordField::getFieldId)
            .toList();
        old.setStatus(FarmingRecordStatus.SUBMITTED);
        old.setSubmitTime(new Date());
        if (StringUtils.isBlank(old.getSensorSnapshotJson())) {
            old.setSensorSnapshotJson(buildSensorSnapshotJson(fieldIds));
        }
        old.setUpdateBy(userId);
        old.setUpdateTime(LocalDateTime.now());
        farmingRecordMapper.updateById(old);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDraft(Long recordId) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        SfFarmingRecord old = farmingRecordMapper.selectDraftOwned(recordId, tenantId, userId);
        if (old == null) {
            throw new ServiceException("草稿不存在或无权删除");
        }
        removeRecordChildren(tenantId, recordId);
        farmingRecordMapper.deleteById(recordId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSubmitted(Long recordId, SfFarmingRecordSaveBo bo) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        SfFarmingRecord old = farmingRecordMapper.selectSubmittedOwned(recordId, tenantId, userId);
        if (old == null) {
            throw new ServiceException("已提交记录不存在或无权修改");
        }
        PersistPayload payload = preparePayload(recordId, tenantId, bo, true, loadExistingWorkItemSnapshotMap(tenantId, recordId));
        fillRecord(old, bo, payload.sensorSnapshotJson(), payload.fields());
        old.setUpdateBy(userId);
        old.setUpdateTime(LocalDateTime.now());
        farmingRecordMapper.updateById(old);
        replaceFields(tenantId, recordId, payload.fields());
        replaceWorkItems(tenantId, recordId, payload.workItems());
        replaceMedia(tenantId, recordId, bo.getMedia());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeSubmitted(Long recordId) {
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        SfFarmingRecord old = farmingRecordMapper.selectSubmittedOwned(recordId, tenantId, userId);
        if (old == null) {
            throw new ServiceException("已提交记录不存在或无权删除");
        }
        removeRecordChildren(tenantId, recordId);
        farmingRecordMapper.deleteById(recordId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfFarmingRecordMediaVo patchMediaGeo(Long recordId, Long mediaId, SfFarmingRecordMediaGeoPatchBo body) {
        if (body == null) {
            throw new ServiceException("请求体不能为空");
        }
        if (body.getLat() == null && body.getLng() == null && body.getCapturedAt() == null
            && StringUtils.isBlank(body.getCaption())) {
            throw new ServiceException("请至少提供 lat、lng、capturedAt、caption 之一");
        }
        String tenantId = requireTenantId();
        Long userId = LoginHelper.getUserId();
        SfFarmingRecord record = loadOwnedDraftOrSubmitted(recordId, tenantId, userId);
        if (record == null) {
            throw new ServiceException("记录不存在或无权修改");
        }
        SfFarmingRecordMedia row = mediaMapper.selectByTenantRecordAndMediaId(tenantId, recordId, mediaId);
        if (row == null) {
            throw new ServiceException("媒体不存在或不属于该记录");
        }
        if (body.getLat() != null) {
            row.setLat(body.getLat());
        }
        if (body.getLng() != null) {
            row.setLng(body.getLng());
        }
        if (body.getCapturedAt() != null) {
            row.setCapturedAt(body.getCapturedAt());
        }
        if (body.getCaption() != null) {
            row.setCaption(body.getCaption().trim());
        }
        mediaMapper.updateById(row);
        return MapstructUtils.convert(row, SfFarmingRecordMediaVo.class);
    }

    private PersistPayload preparePayload(Long recordId,
                                          String tenantId,
                                          SfFarmingRecordSaveBo bo,
                                          boolean requireWorkItems,
                                          Map<Long, SfFarmingRecordWorkItem> existingWorkItemMap) {
        return preparePayload(recordId, tenantId, bo, requireWorkItems, existingWorkItemMap, false);
    }

    private PersistPayload preparePayload(Long recordId,
                                          String tenantId,
                                          SfFarmingRecordSaveBo bo,
                                          boolean requireWorkItems,
                                          Map<Long, SfFarmingRecordWorkItem> existingWorkItemMap,
                                          boolean forceBuildSensorSnapshot) {
        return preparePayload(recordId, tenantId, bo, requireWorkItems, existingWorkItemMap,
            forceBuildSensorSnapshot, bo.getSensorSnapshotJson(), true);
    }

    private PersistPayload preparePayload(Long recordId,
                                          String tenantId,
                                          SfFarmingRecordSaveBo bo,
                                          boolean requireWorkItems,
                                          Map<Long, SfFarmingRecordWorkItem> existingWorkItemMap,
                                          String sensorSnapshotJson) {
        return preparePayload(recordId, tenantId, bo, requireWorkItems, existingWorkItemMap,
            false, sensorSnapshotJson, false);
    }

    private PersistPayload preparePayload(Long recordId,
                                          String tenantId,
                                          SfFarmingRecordSaveBo bo,
                                          boolean requireWorkItems,
                                          Map<Long, SfFarmingRecordWorkItem> existingWorkItemMap,
                                          boolean forceBuildSensorSnapshot,
                                          String sensorSnapshotJson,
                                          boolean buildWhenBlank) {
        List<Long> fieldIds = normalizeIds(bo.getFieldIds(), "地块不能为空");
        List<SfField> fields = loadAndValidateFields(fieldIds, tenantId);
        Map<Long, Date> sowingByField = loadActiveSowingDates(fieldIds);
        List<SfFarmingRecordField> fieldRows = buildFieldRows(recordId, tenantId, fields, sowingByField, fieldIds);
        List<SfFarmingRecordWorkItem> workItemRows = buildWorkItemRows(recordId, tenantId, bo.getWorkItems(), requireWorkItems, existingWorkItemMap);
        String sensorJson = forceBuildSensorSnapshot || (buildWhenBlank && StringUtils.isBlank(sensorSnapshotJson))
            ? buildSensorSnapshotJson(fieldIds)
            : sensorSnapshotJson;
        return new PersistPayload(fieldRows, workItemRows, sensorJson);
    }

    private Map<Long, SfFarmingRecordWorkItem> loadExistingWorkItemSnapshotMap(String tenantId, Long recordId) {
        return recordWorkItemMapper.selectByRecordId(tenantId, recordId).stream()
            .filter(row -> row.getWorkItemId() != null)
            .collect(Collectors.toMap(SfFarmingRecordWorkItem::getWorkItemId, Function.identity(), (first, ignored) -> first));
    }

    private void fillRecord(SfFarmingRecord record,
                            SfFarmingRecordSaveBo bo,
                            String sensorSnapshotJson,
                            List<SfFarmingRecordField> fieldRows) {
        record.setHappenedAt(bo.getHappenedAt());
        record.setWorkPeriodJson(toWorkPeriodJson(normalizeWorkPeriod(bo.getWorkPeriod())));
        record.setSummary(StringUtils.isNotBlank(bo.getSummary()) ? bo.getSummary().trim() : "");
        record.setWeatherJson(toJson(resolveWeatherForSave(bo.getWeather(), bo.getHappenedAt())));
        record.setGrowthStageJson(toJson(enrichGrowthStage(bo.getGrowthStage(), fieldRows)));
        record.setResourceJson(toJson(enrichResource(bo.getResource())));
        record.setFeedbackJson(toJson(normalizeFeedback(bo.getFeedback())));
        record.setEnvironmentSummaryJson(toJson(bo.getEnvironmentSummary()));
        record.setSensorSnapshotJson(sensorSnapshotJson);
        record.setRemark(bo.getRemark());
    }

    private List<SfFarmingRecordField> buildFieldRows(Long recordId,
                                                       String tenantId,
                                                       List<SfField> fields,
                                                       Map<Long, Date> sowingByField,
                                                       List<Long> orderedFieldIds) {
        Map<Long, SfField> fieldMap = fields.stream().collect(Collectors.toMap(SfField::getFieldId, Function.identity()));
        List<SfFarmingRecordField> rows = new ArrayList<>();
        for (int i = 0; i < orderedFieldIds.size(); i++) {
            Long fieldId = orderedFieldIds.get(i);
            SfField field = fieldMap.get(fieldId);
            SfFarmingRecordField row = new SfFarmingRecordField();
            row.setId(IdWorker.getId());
            row.setTenantId(tenantId);
            row.setRecordId(recordId);
            row.setFieldId(fieldId);
            row.setFieldCodeSnapshot(field.getFieldCode());
            row.setFieldNameSnapshot(field.getFieldName());
            row.setSowingDateSnapshot(sowingByField.get(fieldId));
            row.setSortOrder(i);
            rows.add(row);
        }
        return rows;
    }

    private List<SfFarmingRecordWorkItem> buildWorkItemRows(Long recordId,
                                                            String tenantId,
                                                            List<SfFarmingRecordWorkItemBo> inputItems,
                                                            boolean requireNonEmpty,
                                                            Map<Long, SfFarmingRecordWorkItem> existingWorkItemMap) {
        if (CollUtil.isEmpty(inputItems)) {
            if (requireNonEmpty) {
                throw new ServiceException("农事项目不能为空");
            }
            return List.of();
        }
        List<Long> itemIds = inputItems.stream()
            .map(SfFarmingRecordWorkItemBo::getWorkItemId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<SfFarmWorkDict> items = farmWorkDictMapper.selectNormalByIds(tenantId, itemIds);
        if (items.size() != itemIds.size()) {
            throw new ServiceException("农事项目不存在或无权访问");
        }
        Map<Long, SfFarmWorkDict> itemMap = items.stream().collect(Collectors.toMap(SfFarmWorkDict::getDictId, Function.identity()));
        List<Long> categoryIds = items.stream().map(SfFarmWorkDict::getParentId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SfFarmWorkDict> categoryMap = farmWorkDictMapper.selectNormalByIds(tenantId, categoryIds).stream()
            .collect(Collectors.toMap(SfFarmWorkDict::getDictId, Function.identity()));

        List<SfFarmingRecordWorkItem> rows = new ArrayList<>();
        for (int i = 0; i < inputItems.size(); i++) {
            SfFarmingRecordWorkItemBo input = inputItems.get(i);
            SfFarmWorkDict item = itemMap.get(input.getWorkItemId());
            if (item == null || !FarmWorkNodeType.isItem(item.getNodeType())) {
                throw new ServiceException("请选择具体农事项目，不能选择分类");
            }
            if (!SystemConstants.NORMAL.equals(item.getStatus())) {
                throw new ServiceException("农事项目已停用：" + item.getDictName());
            }
            SfFarmWorkDict category = categoryMap.get(item.getParentId());
            if (category == null || !FarmWorkNodeType.isCategory(category.getNodeType())) {
                throw new ServiceException("农事项目所属分类不存在：" + item.getDictName());
            }
            SfFarmingRecordWorkItem row = new SfFarmingRecordWorkItem();
            row.setItemId(IdWorker.getId());
            row.setTenantId(tenantId);
            row.setRecordId(recordId);
            row.setWorkItemId(item.getDictId());
            row.setWorkItemCode(item.getDictCode());
            row.setWorkItemName(item.getDictName());
            row.setCategoryId(category.getDictId());
            row.setCategoryCode(category.getDictCode());
            row.setCategoryName(category.getDictName());
            String templateJson = resolveWorkItemTemplateJson(item, existingWorkItemMap.get(input.getWorkItemId()));
            row.setCustomFormTemplateJson(templateJson);
            row.setCustomFormDataJson(FarmingCustomFormSupport.validateDataJson(templateJson, input.getCustomFormData()));
            row.setSortOrder(input.getSortOrder() != null ? input.getSortOrder() : i);
            rows.add(row);
        }
        return rows;
    }

    private static String resolveWorkItemTemplateJson(SfFarmWorkDict item, SfFarmingRecordWorkItem existingItem) {
        if (existingItem != null && StringUtils.isNotBlank(existingItem.getCustomFormTemplateJson())) {
            return existingItem.getCustomFormTemplateJson();
        }
        return item.getCustomFormTemplateJson();
    }

    private List<SfField> loadAndValidateFields(List<Long> fieldIds, String tenantId) {
        fieldIds.forEach(id -> fieldGate.assertAccessible(id, tenantId));
        List<SfField> fields = fieldMapper.selectNormalEntitiesByFieldIds(fieldIds);
        if (fields.size() != fieldIds.size()) {
            throw new ServiceException("地块不存在或无权访问");
        }
        for (SfField field : fields) {
            if (!tenantId.equals(field.getTenantId())) {
                throw new ServiceException("地块不属于当前租户");
            }
        }
        return fields;
    }

    private Map<Long, Date> loadActiveSowingDates(Collection<Long> fieldIds) {
        Map<Long, SfPlantingBatch> batches = loadActiveBatchByField(fieldIds);
        Map<Long, Date> out = new LinkedHashMap<>();
        for (Map.Entry<Long, SfPlantingBatch> entry : batches.entrySet()) {
            out.put(entry.getKey(), entry.getValue().getSowingDate());
        }
        return out;
    }

    /**
     * 按地块取当前进行中种植批次；同一地块多条时保留 batchId 最大的一条。
     */
    private Map<Long, SfPlantingBatch> loadActiveBatchByField(Collection<Long> fieldIds) {
        if (CollUtil.isEmpty(fieldIds)) {
            return Map.of();
        }
        List<SfPlantingBatch> rows = plantingBatchMapper.selectList(Wrappers.<SfPlantingBatch>lambdaQuery()
            .in(SfPlantingBatch::getFieldId, fieldIds)
            .in(SfPlantingBatch::getBatchStatus, PlantingBatchStatus.ACTIVE_STATUSES)
            .eq(SfPlantingBatch::getDelFlag, SystemConstants.NORMAL)
            .orderByDesc(SfPlantingBatch::getBatchId));
        Map<Long, SfPlantingBatch> out = new LinkedHashMap<>();
        for (SfPlantingBatch row : rows) {
            out.putIfAbsent(row.getFieldId(), row);
        }
        return out;
    }

    /**
     * 管理端详情：按地块解析种植批次并填充作物物种/品种展示字段（读时计算，不落库）。
     */
    private void enrichAdminFieldCrop(String tenantId, List<SfFarmingRecordFieldVo> fields) {
        if (CollUtil.isEmpty(fields)) {
            return;
        }
        List<Long> fieldIds = fields.stream()
            .map(SfFarmingRecordFieldVo::getFieldId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, SfPlantingBatch> activeBatchByField = loadActiveBatchByField(fieldIds);
        for (SfFarmingRecordFieldVo fieldVo : fields) {
            Long batchId = resolvePlantingBatchIdForAdminField(fieldVo, activeBatchByField);
            if (batchId == null) {
                continue;
            }
            SfTaskCropSnapshot snap = cropSnapshotFiller.resolve(fieldVo.getFieldId(), batchId, tenantId, null);
            applyCropSnapshotToFieldVo(fieldVo, snap);
        }
    }

    private Long resolvePlantingBatchIdForAdminField(SfFarmingRecordFieldVo fieldVo,
                                                     Map<Long, SfPlantingBatch> activeBatchByField) {
        if (fieldVo.getFieldId() == null) {
            return null;
        }
        if (fieldVo.getSowingDateSnapshot() != null) {
            SfPlantingBatch matched = findBatchByFieldAndSowingDate(fieldVo.getFieldId(), fieldVo.getSowingDateSnapshot());
            if (matched != null) {
                return matched.getBatchId();
            }
        }
        SfPlantingBatch active = activeBatchByField.get(fieldVo.getFieldId());
        return active != null ? active.getBatchId() : null;
    }

    private SfPlantingBatch findBatchByFieldAndSowingDate(Long fieldId, Date sowingDate) {
        Date dayStart = DateUtil.beginOfDay(sowingDate);
        Date dayEnd = DateUtil.endOfDay(sowingDate);
        return plantingBatchMapper.selectOne(Wrappers.<SfPlantingBatch>lambdaQuery()
            .eq(SfPlantingBatch::getFieldId, fieldId)
            .eq(SfPlantingBatch::getDelFlag, SystemConstants.NORMAL)
            .ge(SfPlantingBatch::getSowingDate, dayStart)
            .le(SfPlantingBatch::getSowingDate, dayEnd)
            .orderByDesc(SfPlantingBatch::getBatchId)
            .last("LIMIT 1"));
    }

    private void applyCropSnapshotToFieldVo(SfFarmingRecordFieldVo fieldVo, SfTaskCropSnapshot snap) {
        if (snap == null) {
            return;
        }
        fieldVo.setPlantingBatchId(snap.getPlantingBatchId());
        fieldVo.setVarietyId(snap.getVarietyId());
        fieldVo.setVarietyName(snap.getVarietyName());
        fieldVo.setSpeciesId(snap.getSpeciesId());
        fieldVo.setSpeciesName(snap.getSpeciesName());
        if (snap.getSpeciesId() == null) {
            return;
        }
        SfCropSpecies species = cropSpeciesMapper.selectById(snap.getSpeciesId());
        if (species != null && SystemConstants.NORMAL.equals(species.getDelFlag())) {
            fieldVo.setSpeciesImageUrl(species.getMapIconUrl());
        }
    }

    private String buildSensorSnapshotJson(List<Long> fieldIds) {
        return JSON.toJSONString(buildSensorSnapshotPayload(fieldIds));
    }

    private Map<String, Object> buildSensorSnapshotPayload(List<Long> fieldIds) {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Object> fields = new ArrayList<>();
        for (Long fieldId : fieldIds) {
            fields.add(toSimpleFieldSnapshot(sensorSnapshotBuilder.buildRawFieldSnapshot(fieldId)));
        }
        root.put("fields", fields);
        return root;
    }

    private Map<String, Object> toSimpleFieldSnapshot(Map<String, Object> rawField) {
        Map<String, Object> simpleField = new LinkedHashMap<>();
        simpleField.put("fieldId", rawField.get("fieldId"));
        List<Object> simpleDevices = new ArrayList<>();
        Object devicesObj = rawField.get("devices");
        if (devicesObj instanceof List<?> devices) {
            for (Object deviceObj : devices) {
                Map<String, Object> device = toObjectMap(deviceObj);
                Map<String, Object> simpleDevice = new LinkedHashMap<>();
                simpleDevice.put("deviceName", device.get("deviceName"));
                List<Map<String, Object>> points = buildSimpleSensorPoints(device);
                simpleDevice.put("points", points);
                Date deviceCollectTime = resolveMaxDeviceCollectTime(device);
                if (deviceCollectTime != null) {
                    simpleDevice.put("collectTime", DateUtil.format(deviceCollectTime, "yyyy-MM-dd HH:mm:ss"));
                }
                simpleDevices.add(simpleDevice);
            }
        }
        simpleField.put("devices", simpleDevices);
        return simpleField;
    }

    private List<Map<String, Object>> buildSimpleSensorPoints(Map<String, Object> device) {
        Map<String, Object> metrics = toObjectMap(device.get("metrics"));
        Map<String, Object> names = toObjectMap(device.get("names"));
        Map<String, Object> texts = toObjectMap(device.get("texts"));
        Map<String, Object> units = toObjectMap(device.get("units"));
        Map<String, Object> collectTimes = toObjectMap(device.get("collectTimes"));
        List<Map<String, Object>> points = new ArrayList<>();
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            String metricCode = entry.getKey();
            String displayValue = resolveSimpleSensorValue(metricCode, entry.getValue(), texts, units);
            if (StringUtils.isBlank(displayValue)) {
                continue;
            }
            Map<String, Object> point = new LinkedHashMap<>();
            String name = objectToString(names.get(metricCode));
            point.put("name", StringUtils.isBlank(name) ? metricCode : name);
            point.put("value", displayValue);
            Date collectTime = parseCollectTimeMillis(collectTimes.get(metricCode));
            if (collectTime != null) {
                point.put("collectTime", DateUtil.format(collectTime, "yyyy-MM-dd HH:mm:ss"));
            }
            points.add(point);
        }
        return points;
    }

    private static Date resolveMaxDeviceCollectTime(Map<String, Object> device) {
        Map<String, Object> collectTimes = toObjectMap(device.get("collectTimes"));
        Date max = null;
        for (Object raw : collectTimes.values()) {
            Date time = parseCollectTimeMillis(raw);
            if (time != null && (max == null || time.after(max))) {
                max = time;
            }
        }
        return max;
    }

    private static Date parseCollectTimeMillis(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = String.valueOf(raw).trim();
        if (StringUtils.isBlank(text) || !NumberUtil.isLong(text)) {
            return null;
        }
        return new Date(Long.parseLong(text));
    }

    private static String resolveSimpleSensorValue(String metricCode,
                                                   Object metricValue,
                                                   Map<String, Object> texts,
                                                   Map<String, Object> units) {
        String text = objectToString(texts.get(metricCode));
        if (StringUtils.isNotBlank(text)) {
            return text;
        }
        String value = objectToString(metricValue);
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String unit = objectToString(units.get(metricCode));
        return StringUtils.isBlank(unit) ? value : value + unit;
    }

    private static Map<String, Object> toObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> raw) || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return out;
    }

    private Map<String, Object> resolveWeatherForSave(Map<String, Object> input, Date happenedAt) {
        if (input != null && !input.isEmpty()) {
            if (WEATHER_SOURCE_FORECAST.equals(objectToString(input.get("source")))) {
                Map<String, Object> forecast = copyMap(input);
                forecast.put("manual", false);
                forecast.putIfAbsent("temperatureUnit", TEMP_UNIT_CELSIUS);
                forecast.putIfAbsent("reliable", true);
                return forecast;
            }
            Map<String, Object> manual = copyMap(input);
            manual.put("source", WEATHER_SOURCE_MANUAL);
            manual.put("manual", true);
            manual.put("reliable", false);
            manual.putIfAbsent("temperatureUnit", TEMP_UNIT_CELSIUS);
            return manual;
        }
        Map<String, Object> auto = buildForecastWeatherSnapshot(happenedAt, false);
        return WEATHER_SOURCE_FORECAST.equals(auto.get("source")) ? auto : Map.of();
    }

    private Map<String, Object> buildForecastWeatherSnapshot(Date happenedAt, boolean includeNonePayload) {
        String date = happenedAt == null ? null : DateUtil.formatDate(happenedAt);
        if (StringUtils.isBlank(date)) {
            return noneWeatherPayload(null);
        }
        try {
            SfWeatherLatestVo latest = weatherService.getLatest();
            SfWeatherForecastVo forecast = latest == null || latest.getForecasts() == null ? null : latest.getForecasts().stream()
                .filter(Objects::nonNull)
                .filter(row -> date.equals(row.getCastDate()))
                .findFirst()
                .orElse(null);
            if (forecast == null) {
                return includeNonePayload ? noneWeatherPayload(date) : Map.of();
            }
            boolean dayTime = DateUtil.hour(happenedAt, true) >= 6 && DateUtil.hour(happenedAt, true) < 18;
            String rawWeather = dayTime ? forecast.getDayWeather() : forecast.getNightWeather();
            String rawTemp = dayTime ? forecast.getDayTemp() : forecast.getNightTemp();
            String windPower = dayTime ? forecast.getDayPower() : forecast.getNightPower();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("source", WEATHER_SOURCE_FORECAST);
            out.put("manual", false);
            out.put("date", date);
            out.put("name", normalizeWeatherName(rawWeather, windPower));
            out.put("rawWeather", rawWeather);
            out.put("temperature", parseNumberValue(rawTemp));
            out.put("temperatureUnit", TEMP_UNIT_CELSIUS);
            out.put("windPower", windPower);
            out.put("adcode", forecast.getAdcode());
            out.put("cityName", forecast.getCityName());
            out.put("reliable", StringUtils.isNotBlank(rawWeather));
            return out;
        } catch (Exception e) {
            log.warn("生成农事天气快照失败，happenedAt={}", happenedAt, e);
            return includeNonePayload ? noneWeatherPayload(date) : Map.of();
        }
    }

    private Map<String, Object> noneWeatherPayload(String date) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("source", WEATHER_SOURCE_NONE);
        out.put("manual", false);
        out.put("date", date);
        out.put("reliable", false);
        return out;
    }

    private static String normalizeWeatherName(String rawWeather, String windPower) {
        if (isStrongWind(rawWeather, windPower)) {
            return "大风";
        }
        if (StringUtils.isBlank(rawWeather)) {
            return null;
        }
        if (rawWeather.contains("雨") || rawWeather.contains("雪") || rawWeather.contains("冰雹")) {
            return "雨";
        }
        if (rawWeather.contains("多云")) {
            return "多云";
        }
        if (rawWeather.contains("阴")) {
            return "阴";
        }
        if (rawWeather.contains("晴")) {
            return "晴";
        }
        return rawWeather;
    }

    private static boolean isStrongWind(String rawWeather, String windPower) {
        if (StringUtils.isNotBlank(rawWeather)
            && (rawWeather.contains("大风") || rawWeather.contains("强风") || rawWeather.contains("疾风"))) {
            return true;
        }
        String cleaned = windPower == null ? "" : windPower.replaceAll("[^0-9.\\-]", "");
        return StringUtils.isNotBlank(cleaned) && NumberUtil.isNumber(cleaned) && Double.parseDouble(cleaned) >= 6D;
    }

    private static Object parseNumberValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String cleaned = value.replaceAll("[^0-9.\\-]", "");
        if (StringUtils.isBlank(cleaned) || !NumberUtil.isNumber(cleaned)) {
            return value;
        }
        double number = Double.parseDouble(cleaned);
        return number == Math.rint(number) ? (int) number : number;
    }

    private Map<String, Object> enrichGrowthStage(Map<String, Object> input, List<SfFarmingRecordField> fieldRows) {
        Map<String, Object> out = copyMap(input);
        String code = objectToString(out.get("code"));
        if (StringUtils.isNotBlank(code) && StringUtils.isBlank(objectToString(out.get("name")))) {
            String name = resolveGrowthStageNameFromSpeciesConfig(code, fieldRows);
            if (StringUtils.isNotBlank(name)) {
                out.put("name", name);
            }
        }
        return out;
    }

    private String resolveGrowthStageNameFromSpeciesConfig(String code, List<SfFarmingRecordField> fieldRows) {
        if (StringUtils.isBlank(code) || CollUtil.isEmpty(fieldRows) || cropSnapshotFiller == null || cropSpeciesMapper == null) {
            return null;
        }
        SfFarmingRecordField first = fieldRows.get(0);
        if (first.getFieldId() == null) {
            return null;
        }
        SfPlantingBatch batch = loadActiveBatchByField(List.of(first.getFieldId())).get(first.getFieldId());
        if (batch == null) {
            return null;
        }
        SfTaskCropSnapshot snap = cropSnapshotFiller.resolve(first.getFieldId(), batch.getBatchId(), first.getTenantId(), null);
        if (snap == null || snap.getSpeciesId() == null) {
            return null;
        }
        SfCropSpecies species = cropSpeciesMapper.selectById(snap.getSpeciesId());
        if (species == null || !SystemConstants.NORMAL.equals(species.getDelFlag())) {
            return null;
        }
        Map<String, Object> stage = GrowthStageConfigSupport.findStageByCode(species.getGrowthStageConfigJson(), code);
        return objectToString(stage.get("name"));
    }

    private Map<String, Object> enrichResource(Map<String, Object> input) {
        Map<String, Object> out = copyMap(input);
        Map<String, String> machineTypeLabels = masterDictAccessor.getDictLabelMap(DICT_MACHINE_TYPE);
        Map<String, String> materialLabels = masterDictAccessor.getDictLabelMap(DICT_MATERIAL_NAME);
        enrichArray(out.get("machines"), row -> fillNameByCode(row, "machineTypeCode", "machineTypeName", machineTypeLabels));
        enrichArray(out.get("materials"), row -> fillNameByCode(row, "materialCode", "materialName", materialLabels));
        return out;
    }

    private Map<String, Object> normalizeFeedback(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = copyMap(input);
        Object plantHeight = out.get(FEEDBACK_PLANT_HEIGHT);
        if (plantHeight == null || StringUtils.isBlank(objectToString(plantHeight))) {
            return out;
        }
        BigDecimal height = parsePlantHeight(plantHeight);
        if (height.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("植株高度必须大于0");
        }
        out.put(FEEDBACK_PLANT_HEIGHT, height);
        out.put(FEEDBACK_PLANT_HEIGHT_UNIT, PLANT_HEIGHT_UNIT_CM);
        return out;
    }

    private BigDecimal parsePlantHeight(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = objectToString(value);
        if (StringUtils.isBlank(text)) {
            throw new ServiceException("植株高度不能为空");
        }
        String trimmed = text.trim();
        if (!NumberUtil.isNumber(trimmed)) {
            throw new ServiceException("植株高度必须为数字");
        }
        return new BigDecimal(trimmed);
    }

    private void fillNameByCode(Map<String, Object> row, String codeKey, String nameKey, Map<String, String> dictLabels) {
        String code = objectToString(row.get(codeKey));
        if (StringUtils.isBlank(code) || StringUtils.isNotBlank(objectToString(row.get(nameKey)))) {
            return;
        }
        String label = dictLabels != null ? dictLabels.get(code) : null;
        row.put(nameKey, StringUtils.isBlank(label) ? code : label);
    }

    @SuppressWarnings("unchecked")
    private void enrichArray(Object value, java.util.function.Consumer<Map<String, Object>> consumer) {
        if (!(value instanceof Collection<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                consumer.accept((Map<String, Object>) map);
            }
        }
    }

    private SfFarmingRecord loadVisible(Long recordId, String tenantId, Long userId) {
        SfFarmingRecord record = farmingRecordMapper.selectById(recordId);
        if (record == null || !SystemConstants.NORMAL.equals(record.getDelFlag())) {
            throw new ServiceException("农事记录不存在");
        }
        if (!tenantId.equals(record.getTenantId())) {
            throw new ServiceException("无权查看该记录");
        }
        if (FarmingRecordStatus.SUBMITTED.equals(record.getStatus())) {
            return record;
        }
        if (FarmingRecordStatus.DRAFT.equals(record.getStatus()) && userId.equals(record.getCreateBy())) {
            return record;
        }
        throw new ServiceException("草稿仅创建者可查看");
    }

    private SfFarmingRecord loadOwnedDraftOrSubmitted(Long recordId, String tenantId, Long userId) {
        SfFarmingRecord draft = farmingRecordMapper.selectDraftOwned(recordId, tenantId, userId);
        if (draft != null) {
            return draft;
        }
        return farmingRecordMapper.selectSubmittedOwned(recordId, tenantId, userId);
    }

    private SfFarmingRecordVo toVo(SfFarmingRecord record, boolean withMedia) {
        SfFarmingRecordVo vo = new SfFarmingRecordVo();
        BeanUtil.copyProperties(record, vo);
        fillCalendarInfo(vo);
        vo.setWorkPeriod(parseWorkPeriod(record.getWorkPeriodJson()));
        vo.setWeather(parseJson(record.getWeatherJson()));
        vo.setGrowthStage(parseJson(record.getGrowthStageJson()));
        vo.setResource(parseJson(record.getResourceJson()));
        vo.setFeedback(parseJson(record.getFeedbackJson()));
        fillPlantHeight(vo);
        vo.setSensorSnapshot(parseJson(record.getSensorSnapshotJson()));
        vo.setEnvironmentSummary(parseJson(record.getEnvironmentSummaryJson()));
        vo.setFields(recordFieldMapper.selectByRecordId(record.getTenantId(), record.getRecordId()).stream().map(this::toFieldVo).toList());
        vo.setWorkItems(recordWorkItemMapper.selectByRecordId(record.getTenantId(), record.getRecordId()).stream().map(this::toWorkItemVo).toList());
        vo.setPreviewMedia(new ArrayList<>());
        vo.setMedia(new ArrayList<>());
        if (withMedia) {
            vo.setMedia(mediaMapper.selectByRecordId(record.getTenantId(), record.getRecordId()).stream()
                .map(row -> MapstructUtils.convert(row, SfFarmingRecordMediaVo.class))
                .toList());
        }
        if (record.getCreateBy() != null) {
            vo.setCreatorName(loadCreatorNames(Set.of(record.getCreateBy())).get(record.getCreateBy()));
        }
        return vo;
    }

    private Page<SfFarmingRecordVo> buildListVoPage(String tenantId, Page<SfFarmingRecord> page) {
        List<SfFarmingRecord> records = page.getRecords();
        Page<SfFarmingRecordVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (records.isEmpty()) {
            voPage.setRecords(List.of());
            return voPage;
        }
        List<Long> recordIds = records.stream().map(SfFarmingRecord::getRecordId).toList();
        Map<Long, List<SfFarmingRecordFieldVo>> fieldsByRecord = recordFieldMapper.selectByRecordIds(tenantId, recordIds).stream()
            .collect(Collectors.groupingBy(SfFarmingRecordField::getRecordId,
                LinkedHashMap::new,
                Collectors.mapping(this::toFieldVo, Collectors.toList())));
        Map<Long, List<SfFarmingRecordWorkItemVo>> worksByRecord = recordWorkItemMapper.selectByRecordIds(tenantId, recordIds).stream()
            .collect(Collectors.groupingBy(SfFarmingRecordWorkItem::getRecordId,
                LinkedHashMap::new,
                Collectors.mapping(this::toWorkItemVo, Collectors.toList())));
        Map<Long, List<SfFarmingRecordMediaVo>> previewByRecord = loadPreviewMedia(tenantId, recordIds);
        Map<Long, String> creatorNames = loadCreatorNames(records.stream().map(SfFarmingRecord::getCreateBy).filter(Objects::nonNull).collect(Collectors.toSet()));

        voPage.setRecords(records.stream().map(record -> {
            SfFarmingRecordVo vo = toVoWithoutChildren(record);
            vo.setFields(fieldsByRecord.getOrDefault(record.getRecordId(), List.of()));
            vo.setWorkItems(worksByRecord.getOrDefault(record.getRecordId(), List.of()));
            vo.setPreviewMedia(previewByRecord.getOrDefault(record.getRecordId(), List.of()));
            vo.setCreatorName(creatorNames.get(record.getCreateBy()));
            return vo;
        }).toList());
        return voPage;
    }

    private SfFarmingRecordVo toVoWithoutChildren(SfFarmingRecord record) {
        SfFarmingRecordVo vo = new SfFarmingRecordVo();
        BeanUtil.copyProperties(record, vo);
        vo.setWorkPeriod(parseWorkPeriod(record.getWorkPeriodJson()));
        vo.setWeather(parseJson(record.getWeatherJson()));
        vo.setGrowthStage(parseJson(record.getGrowthStageJson()));
        vo.setResource(parseJson(record.getResourceJson()));
        vo.setFeedback(parseJson(record.getFeedbackJson()));
        fillPlantHeight(vo);
        vo.setSensorSnapshot(parseJson(record.getSensorSnapshotJson()));
        vo.setEnvironmentSummary(parseJson(record.getEnvironmentSummaryJson()));
        vo.setMedia(new ArrayList<>());
        vo.setPreviewMedia(new ArrayList<>());
        return vo;
    }

    private void fillPlantHeight(SfFarmingRecordVo vo) {
        if (!(vo.getFeedback() instanceof Map<?, ?> feedback)) {
            vo.setPlantHeightUnit(PLANT_HEIGHT_UNIT_CM);
            return;
        }
        Object height = feedback.get(FEEDBACK_PLANT_HEIGHT);
        if (height != null && StringUtils.isNotBlank(objectToString(height))) {
            try {
                vo.setPlantHeight(parsePlantHeight(height));
            } catch (ServiceException ignored) {
                vo.setPlantHeight(null);
            }
        }
        Object unit = feedback.get(FEEDBACK_PLANT_HEIGHT_UNIT);
        vo.setPlantHeightUnit(StringUtils.isNotBlank(objectToString(unit)) ? objectToString(unit) : PLANT_HEIGHT_UNIT_CM);
    }

    private SfFarmingRecordFieldVo toFieldVo(SfFarmingRecordField row) {
        SfFarmingRecordFieldVo vo = new SfFarmingRecordFieldVo();
        BeanUtil.copyProperties(row, vo);
        return vo;
    }

    private SfFarmingRecordWorkItemVo toWorkItemVo(SfFarmingRecordWorkItem row) {
        SfFarmingRecordWorkItemVo vo = new SfFarmingRecordWorkItemVo();
        BeanUtil.copyProperties(row, vo);
        vo.setCustomFormTemplate(parseJson(row.getCustomFormTemplateJson()));
        vo.setCustomFormData(parseJson(row.getCustomFormDataJson()));
        return vo;
    }

    private void fillCalendarInfo(SfFarmingRecordVo vo) {
        if (vo.getHappenedAt() == null || solarTermService == null) {
            return;
        }
        try {
            vo.setCalendarInfo(solarTermService.getCalendarDateInfo(vo.getHappenedAt()));
        } catch (Exception ex) {
            log.warn("农事记录农历节气信息生成失败, recordId={}, happenedAt={}",
                vo.getRecordId(), vo.getHappenedAt(), ex);
        }
    }

    private void replaceFields(String tenantId, Long recordId, List<SfFarmingRecordField> rows) {
        recordFieldMapper.deleteByRecordId(tenantId, recordId);
        for (SfFarmingRecordField row : rows) {
            recordFieldMapper.insert(row);
        }
    }

    private void replaceWorkItems(String tenantId, Long recordId, List<SfFarmingRecordWorkItem> rows) {
        recordWorkItemMapper.deleteByRecordId(tenantId, recordId);
        for (SfFarmingRecordWorkItem row : rows) {
            recordWorkItemMapper.insert(row);
        }
    }

    private void replaceMedia(String tenantId, Long recordId, List<SfFarmingRecordMediaItemBo> items) {
        mediaMapper.deleteByRecordId(tenantId, recordId);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Long userId = LoginHelper.getUserId();
        Date now = new Date();
        int autoSeq = 0;
        for (SfFarmingRecordMediaItemBo item : items) {
            if (!FarmingMediaKind.isValid(item.getKind())) {
                throw new ServiceException("不支持的媒体类型：" + item.getKind());
            }
            String url = item.getUrl() == null ? "" : item.getUrl().trim();
            if (StringUtils.isBlank(url)) {
                throw new ServiceException("媒体 URL 不能为空");
            }
            SfFarmingRecordMedia row = new SfFarmingRecordMedia();
            row.setMediaId(IdWorker.getId());
            row.setTenantId(tenantId);
            row.setRecordId(recordId);
            row.setKind(item.getKind().trim().toUpperCase());
            row.setUrl(url);
            row.setSeq(item.getSeq() != null ? item.getSeq() : autoSeq++);
            row.setLat(item.getLat());
            row.setLng(item.getLng());
            row.setCaption(item.getCaption());
            row.setCapturedAt(item.getCapturedAt());
            row.setCreateBy(userId);
            row.setCreateTime(now);
            mediaMapper.insert(row);
        }
    }

    private void removeRecordChildren(String tenantId, Long recordId) {
        recordFieldMapper.deleteByRecordId(tenantId, recordId);
        recordWorkItemMapper.deleteByRecordId(tenantId, recordId);
        mediaMapper.deleteByRecordId(tenantId, recordId);
    }

    private Map<Long, List<SfFarmingRecordMediaVo>> loadPreviewMedia(String tenantId, List<Long> recordIds) {
        if (CollUtil.isEmpty(recordIds)) {
            return Map.of();
        }
        Map<Long, List<SfFarmingRecordMediaVo>> out = new LinkedHashMap<>();
        for (SfFarmingRecordMedia media : mediaMapper.selectByRecordIds(tenantId, recordIds)) {
            List<SfFarmingRecordMediaVo> list = out.computeIfAbsent(media.getRecordId(), k -> new ArrayList<>());
            if (list.size() < LIST_PREVIEW_MEDIA_MAX) {
                list.add(MapstructUtils.convert(media, SfFarmingRecordMediaVo.class));
            }
        }
        return out;
    }

    private Map<Long, String> loadCreatorNames(Set<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Map.of();
        }
        Map<Long, String> map = new LinkedHashMap<>();
        for (RemoteUserVo user : masterUserAccessor.selectUserByIds(new ArrayList<>(userIds), null)) {
            if (user.getUserId() != null) {
                map.put(user.getUserId(), user.getNickName());
            }
        }
        return map;
    }

    private static List<Long> normalizeIds(List<Long> ids, String emptyMessage) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException(emptyMessage);
        }
        List<Long> out = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (out.isEmpty()) {
            throw new ServiceException(emptyMessage);
        }
        return out;
    }

    private static Set<Long> intersect(Set<Long> base, Collection<Long> next) {
        Set<Long> nextSet = next == null ? Set.of() : new HashSet<>(next);
        if (base == null) {
            return nextSet;
        }
        base.retainAll(nextSet);
        return base;
    }

    private static Map<String, Object> copyMap(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return new LinkedHashMap<>();
        }
        JSONObject object = JSON.parseObject(JSON.toJSONString(input));
        return new LinkedHashMap<>(object);
    }

    private static String objectToString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(value);
    }

    private static String toWorkPeriodJson(List<String> workPeriod) {
        if (CollUtil.isEmpty(workPeriod)) {
            return null;
        }
        return JSON.toJSONString(workPeriod);
    }

    private static List<String> normalizeWorkPeriod(List<String> input) {
        if (CollUtil.isEmpty(input)) {
            return List.of();
        }
        if (input.size() != 2) {
            throw new ServiceException("作业时段必须包含开始和结束时间");
        }
        LocalDateTime start = parseWorkPeriodTime(input.get(0));
        LocalDateTime end = parseWorkPeriodTime(input.get(1));
        if (!end.isAfter(start)) {
            throw new ServiceException("作业时段结束时间必须晚于开始时间");
        }
        if (Duration.between(start, end).compareTo(WORK_PERIOD_MAX_DURATION) > 0) {
            throw new ServiceException("作业时段跨度不能超过 24 小时");
        }
        return List.of(start.format(WORK_PERIOD_MINUTE_FORMATTER), end.format(WORK_PERIOD_MINUTE_FORMATTER));
    }

    private static LocalDateTime parseWorkPeriodTime(String value) {
        if (StringUtils.isBlank(value)) {
            throw new ServiceException("作业时段必须包含开始和结束时间");
        }
        String text = value.trim();
        try {
            return LocalDateTime.parse(text, WORK_PERIOD_MINUTE_FORMATTER).withSecond(0).withNano(0);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(text, WORK_PERIOD_SECOND_FORMATTER).withSecond(0).withNano(0);
            } catch (DateTimeParseException ex) {
                throw new ServiceException("作业时段格式必须为 yyyy-MM-dd HH:mm");
            }
        }
    }

    private static List<String> parseWorkPeriod(String json) {
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> values = JSON.parseArray(json, String.class);
            return values == null ? new ArrayList<>() : new ArrayList<>(normalizeWorkPeriod(values));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static Object parseJson(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JSON.parse(json);
        } catch (Exception e) {
            return json;
        }
    }

    private static String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("租户上下文缺失");
        }
        return tenantId;
    }

    private record PersistPayload(List<SfFarmingRecordField> fields,
                                  List<SfFarmingRecordWorkItem> workItems,
                                  String sensorSnapshotJson) {
    }
}
