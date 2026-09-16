package com.ym.agriculture.farmtask.inspectionphotoarchive.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.core.utils.file.MimeTypeUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.batch.model.constants.PlantingBatchStatus;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farmtask.inspectionphotoarchive.dao.SfInspectionPhotoArchiveFieldMapper;
import com.ym.agriculture.farmtask.inspectionphotoarchive.dao.SfInspectionPhotoArchiveMapper;
import com.ym.agriculture.farmtask.inspectionphotoarchive.dao.SfInspectionPhotoArchivePhotoMapper;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo.SfInspectionPhotoArchiveCreateBo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo.SfInspectionPhotoArchiveSyncBo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchive;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchiveField;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchivePhoto;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveDateVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveDetailVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveFieldVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchivePhotoVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveSyncPreviewVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveSyncResultVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveUploadFailVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveUploadVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.service.ISfInspectionPhotoArchiveService;
import com.ym.agriculture.farmtask.inspectionphotoarchive.support.SfInspectionPhotoArchiveMasterOssAccessor;
import com.ym.resource.api.domain.RemoteFileBatchUploadVo;
import com.ym.resource.api.domain.RemoteFileUploadFailVo;
import com.ym.resource.api.domain.RemoteFileUploadVo;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 巡查照片归档服务实现。
 *
 * @author ym-cloud
 */
@Service
@RequiredArgsConstructor
public class SfInspectionPhotoArchiveServiceImpl implements ISfInspectionPhotoArchiveService {

    /** 单次归档图片上传上限，与 OSS 批量上传能力保持一致。 */
    private static final int MAX_UPLOAD_COUNT = 20;

    /** 无有效种植批次时的固定展示文案。 */
    private static final String NO_CROP_LABEL = "未种植";

    /** 归档日期数据访问层。 */
    private final SfInspectionPhotoArchiveMapper archiveMapper;

    /** 归档大棚快照数据访问层。 */
    private final SfInspectionPhotoArchiveFieldMapper archiveFieldMapper;

    /** 归档照片关联数据访问层。 */
    private final SfInspectionPhotoArchivePhotoMapper archivePhotoMapper;

    /** 当前大棚主数据读取器。 */
    private final SfFieldMapper fieldMapper;

    /** 当前种植批次读取服务，负责批量装配物种与品种名称。 */
    private final ISfPlantingBatchService plantingBatchService;

    /** 访问 master 数据源 OSS 的明确桥接。 */
    private final SfInspectionPhotoArchiveMasterOssAccessor masterOssAccessor;

    /** {@inheritDoc} */
    @Override
    public List<SfInspectionPhotoArchiveDateVo> listDates() {
        String tenantId = requireTenantId();
        return archiveMapper.selectDateList(tenantId).stream()
            .map(this::toDateVo)
            .toList();
    }

    /** {@inheritDoc} */
    @Override
    public SfInspectionPhotoArchiveDetailVo getDetail(Long archiveId) {
        String tenantId = requireTenantId();
        return buildDetail(requireArchive(tenantId, archiveId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfInspectionPhotoArchiveDetailVo create(SfInspectionPhotoArchiveCreateBo bo) {
        String tenantId = requireTenantId();
        LocalDate archiveDate = bo.getArchiveDate();
        if (archiveDate.isAfter(LocalDate.now())) {
            throw new ServiceException("归档日期不能晚于当前日期");
        }
        if (archiveMapper.selectTenantByDate(tenantId, archiveDate) != null) {
            throw new ServiceException("该归档日期已存在");
        }

        SfInspectionPhotoArchive archive = new SfInspectionPhotoArchive();
        fillNewArchive(archive, tenantId, archiveDate);
        try {
            if (archiveMapper.insert(archive) <= 0) {
                throw new ServiceException("创建归档日期失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new ServiceException("该归档日期已存在");
        }

        List<SfInspectionPhotoArchiveField> snapshots = buildFieldSnapshots(
            tenantId, archive.getArchiveId(), listEnabledGreenhouses(tenantId));
        if (!snapshots.isEmpty() && !archiveFieldMapper.insertBatch(snapshots)) {
            throw new ServiceException("创建归档大棚快照失败");
        }
        return buildDetail(archive);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeArchive(Long archiveId) {
        String tenantId = requireTenantId();
        requireLockedArchive(tenantId, archiveId);
        List<SfInspectionPhotoArchiveField> fields = archiveFieldMapper.selectByArchiveId(tenantId, archiveId);
        List<Long> archiveFieldIds = fields.stream()
            .map(SfInspectionPhotoArchiveField::getArchiveFieldId)
            .toList();
        archivePhotoMapper.deleteByArchiveFieldIds(tenantId, archiveFieldIds);
        archiveFieldMapper.deleteByArchiveId(tenantId, archiveId);
        if (archiveMapper.deleteTenantById(tenantId, archiveId) <= 0) {
            throw new ServiceException("删除归档日期失败");
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfInspectionPhotoArchiveUploadVo uploadPhotos(Long archiveId, Long archiveFieldId, MultipartFile[] files) {
        String tenantId = requireTenantId();
        requireLockedArchive(tenantId, archiveId);
        requireArchiveField(tenantId, archiveId, archiveFieldId);
        if (files == null || files.length == 0) {
            throw new ServiceException("请至少选择一张图片");
        }
        if (files.length > MAX_UPLOAD_COUNT) {
            throw new ServiceException("单次最多上传 " + MAX_UPLOAD_COUNT + " 张图片");
        }

        SfInspectionPhotoArchiveUploadVo result = new SfInspectionPhotoArchiveUploadVo();
        List<SfInspectionPhotoArchiveUploadFailVo> failList = new ArrayList<>();
        List<MultipartFile> imageFiles = filterImageFiles(files, failList);
        if (imageFiles.isEmpty()) {
            result.setSuccessList(List.of());
            result.setFailList(failList);
            return result;
        }

        RemoteFileBatchUploadVo ossResult = masterOssAccessor.uploadBatch(
            tenantId, imageFiles.toArray(MultipartFile[]::new));
        failList.addAll(toUploadFailVos(ossResult.getFailList()));
        List<SfInspectionPhotoArchivePhoto> photos = buildUploadedPhotos(
            tenantId, archiveFieldId, ossResult.getSuccessList(), failList);
        if (!photos.isEmpty() && !archivePhotoMapper.insertBatch(photos)) {
            throw new ServiceException("关联归档照片失败");
        }
        Map<Long, String> uploadedUrlMap = ossResult.getSuccessList().stream()
            .filter(item -> parseOssId(item) != null)
            .collect(Collectors.toMap(
                item -> Objects.requireNonNull(parseOssId(item)),
                RemoteFileUploadVo::getUrl,
                (left, right) -> left));
        result.setSuccessList(photos.stream()
            .map(photo -> toPhotoVo(photo, uploadedUrlMap.get(photo.getOssId())))
            .toList());
        result.setFailList(failList);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removePhoto(Long archiveId, Long archiveFieldId, Long photoId) {
        String tenantId = requireTenantId();
        requireLockedArchive(tenantId, archiveId);
        requireArchiveField(tenantId, archiveId, archiveFieldId);
        if (archivePhotoMapper.selectTenantArchivePhoto(tenantId, archiveFieldId, photoId) == null) {
            throw new ServiceException("归档照片不存在");
        }
        if (archivePhotoMapper.deleteTenantArchivePhoto(tenantId, archiveFieldId, photoId) <= 0) {
            throw new ServiceException("删除归档照片失败");
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public SfInspectionPhotoArchiveSyncPreviewVo previewSync(Long archiveId) {
        String tenantId = requireTenantId();
        requireArchive(tenantId, archiveId);
        return toSyncPreview(calculateSyncCandidates(tenantId, archiveId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfInspectionPhotoArchiveSyncResultVo syncGreenhouses(Long archiveId, SfInspectionPhotoArchiveSyncBo bo) {
        String tenantId = requireTenantId();
        requireLockedArchive(tenantId, archiveId);
        SyncCandidates candidates = calculateSyncCandidates(tenantId, archiveId);
        boolean addRequested = "ADD".equals(bo.getAction()) || "ALL".equals(bo.getAction());
        boolean removeRequested = "REMOVE".equals(bo.getAction()) || "ALL".equals(bo.getAction());
        int addedCount = 0;
        int removedCount = 0;
        if (addRequested && !candidates.additions().isEmpty()) {
            List<SfInspectionPhotoArchiveField> snapshots = buildFieldSnapshots(
                tenantId, archiveId, candidates.additions());
            if (!archiveFieldMapper.insertBatch(snapshots)) {
                throw new ServiceException("同步新增大棚失败");
            }
            addedCount = snapshots.size();
        }
        if (removeRequested && !candidates.removals().isEmpty()) {
            List<Long> removableIds = candidates.removals().stream()
                .map(SfInspectionPhotoArchiveField::getArchiveFieldId)
                .toList();
            archivePhotoMapper.deleteByArchiveFieldIds(tenantId, removableIds);
            removedCount = archiveFieldMapper.deleteByArchiveFieldIds(tenantId, archiveId, removableIds);
        }
        SfInspectionPhotoArchiveSyncResultVo result = new SfInspectionPhotoArchiveSyncResultVo();
        result.setAddedCount(addedCount);
        result.setRemovedCount(removedCount);
        return result;
    }

    /** 读取当前租户全部启用大棚，排序与现有地块列表保持一致。 */
    private List<SfField> listEnabledGreenhouses(String tenantId) {
        LambdaQueryWrapper<SfField> query = Wrappers.<SfField>lambdaQuery()
            .eq(SfField::getTenantId, tenantId)
            .eq(SfField::getFieldType, FieldType.GREENHOUSE)
            .eq(SfField::getStatus, SystemConstants.NORMAL)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL);
        fieldMapper.applyFieldListOrder(query);
        return fieldMapper.selectList(query);
    }

    /** 将当前大棚及其优先级最高的活动批次转换为不可变快照。 */
    private List<SfInspectionPhotoArchiveField> buildFieldSnapshots(String tenantId, Long archiveId, List<SfField> greenhouses) {
        if (greenhouses == null || greenhouses.isEmpty()) {
            return List.of();
        }
        Map<Long, List<SfPlantingBatchVo>> activeBatches = plantingBatchService.mapActiveVoByFieldIds(
            greenhouses.stream().map(SfField::getFieldId).toList());
        Date now = new Date();
        Long userId = LoginHelper.getUserId();
        return greenhouses.stream()
            .map(field -> toFieldSnapshot(
                tenantId, archiveId, field, selectPreferredBatch(activeBatches.get(field.getFieldId())), now, userId))
            .toList();
    }

    /** 选取同大棚活动批次的固定优先级：采收中、生长期、种植中、计划中；同状态取最新批次。 */
    private SfPlantingBatchVo selectPreferredBatch(List<SfPlantingBatchVo> batches) {
        if (batches == null || batches.isEmpty()) {
            return null;
        }
        return batches.stream()
            .min(Comparator.comparing(SfPlantingBatchVo::getBatchStatus, PlantingBatchStatus.ACTIVE_PRIORITY)
                .thenComparing(SfPlantingBatchVo::getBatchId, Comparator.reverseOrder()))
            .orElse(null);
    }

    /** 组装一个大棚快照实体。 */
    private SfInspectionPhotoArchiveField toFieldSnapshot(String tenantId, Long archiveId, SfField field,
                                                           SfPlantingBatchVo batch, Date now, Long userId) {
        SfInspectionPhotoArchiveField snapshot = new SfInspectionPhotoArchiveField();
        snapshot.setArchiveFieldId(IdWorker.getId());
        snapshot.setTenantId(tenantId);
        snapshot.setArchiveId(archiveId);
        snapshot.setFieldId(field.getFieldId());
        snapshot.setFieldCode(field.getFieldCode());
        snapshot.setFieldName(field.getFieldName());
        snapshot.setSortOrder(field.getSortOrder());
        snapshot.setPlantingBatchId(batch == null ? null : batch.getBatchId());
        snapshot.setSpeciesId(batch == null ? null : batch.getSpeciesId());
        snapshot.setSpeciesName(batch == null ? null : batch.getSpeciesName());
        snapshot.setVarietyId(batch == null ? null : batch.getVarietyId());
        snapshot.setVarietyName(batch == null ? null : batch.getVarietyName());
        snapshot.setCreateBy(userId);
        snapshot.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        snapshot.setUpdateBy(userId);
        snapshot.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        return snapshot;
    }

    /** 汇总某个归档日期的详情、照片数量和当前 OSS 访问地址。 */
    private SfInspectionPhotoArchiveDetailVo buildDetail(SfInspectionPhotoArchive archive) {
        List<SfInspectionPhotoArchiveField> fields = archiveFieldMapper.selectByArchiveId(
            archive.getTenantId(), archive.getArchiveId());
        List<Long> archiveFieldIds = fields.stream()
            .map(SfInspectionPhotoArchiveField::getArchiveFieldId)
            .toList();
        List<SfInspectionPhotoArchivePhoto> photos = archivePhotoMapper.selectByArchiveFieldIds(
            archive.getTenantId(), archiveFieldIds);
        Map<Long, String> ossUrlMap = loadOssUrlMap(archive.getTenantId(), photos);
        Map<Long, List<SfInspectionPhotoArchivePhoto>> photosByField = photos.stream()
            .collect(Collectors.groupingBy(SfInspectionPhotoArchivePhoto::getArchiveFieldId));
        SfInspectionPhotoArchiveDetailVo detail = BeanUtil.copyProperties(archive, SfInspectionPhotoArchiveDetailVo.class);
        detail.setFields(fields.stream()
            .map(field -> toFieldVo(field, photosByField.getOrDefault(field.getArchiveFieldId(), List.of()), ossUrlMap))
            .toList());
        return detail;
    }

    /** 读取当前可访问的 OSS 地址；照片关联存在但对象已被运维删除时仍返回关联元数据。 */
    private Map<Long, String> loadOssUrlMap(String tenantId, Collection<SfInspectionPhotoArchivePhoto> photos) {
        List<Long> ossIds = photos.stream()
            .map(SfInspectionPhotoArchivePhoto::getOssId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (ossIds.isEmpty()) {
            return Map.of();
        }
        return masterOssAccessor.listByIds(tenantId, ossIds).stream()
            .collect(Collectors.toMap(RemoteFile::getOssId, RemoteFile::getUrl, (left, right) -> left));
    }

    /** 组装右侧大棚卡片。 */
    private SfInspectionPhotoArchiveFieldVo toFieldVo(SfInspectionPhotoArchiveField field,
                                                       List<SfInspectionPhotoArchivePhoto> photos,
                                                       Map<Long, String> ossUrlMap) {
        SfInspectionPhotoArchiveFieldVo vo = BeanUtil.copyProperties(field, SfInspectionPhotoArchiveFieldVo.class);
        vo.setCropDisplayName(resolveCropDisplayName(field));
        vo.setPhotoCount(photos.size());
        vo.setPhotos(photos.stream()
            .map(photo -> toPhotoVo(photo, ossUrlMap.get(photo.getOssId())))
            .toList());
        return vo;
    }

    /** 组装一张照片的返回对象。 */
    private SfInspectionPhotoArchivePhotoVo toPhotoVo(SfInspectionPhotoArchivePhoto photo, String url) {
        SfInspectionPhotoArchivePhotoVo vo = BeanUtil.copyProperties(photo, SfInspectionPhotoArchivePhotoVo.class);
        vo.setUrl(url);
        return vo;
    }

    /** 根据物种和品种快照生成固定展示名。 */
    private String resolveCropDisplayName(SfInspectionPhotoArchiveField field) {
        boolean hasSpecies = StringUtils.isNotBlank(field.getSpeciesName());
        boolean hasVariety = StringUtils.isNotBlank(field.getVarietyName());
        if (!hasSpecies && !hasVariety) {
            return NO_CROP_LABEL;
        }
        if (!hasSpecies) {
            return field.getVarietyName();
        }
        if (!hasVariety) {
            return field.getSpeciesName();
        }
        return field.getSpeciesName() + " / " + field.getVarietyName();
    }

    /** 对上传数组逐项筛选图片，非法文件以部分失败结果返回。 */
    private List<MultipartFile> filterImageFiles(MultipartFile[] files, List<SfInspectionPhotoArchiveUploadFailVo> failList) {
        List<MultipartFile> imageFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String originalName = file == null ? "" : StringUtils.blankToDefault(file.getOriginalFilename(), "未命名文件");
            if (file == null || file.isEmpty()) {
                failList.add(toUploadFailVo(originalName, "文件不能为空"));
                continue;
            }
            String extension = FileUtil.extName(file.getOriginalFilename());
            String contentType = file.getContentType();
            boolean imageExtension = StringUtils.equalsAnyIgnoreCase(extension, MimeTypeUtils.IMAGE_EXTENSION);
            boolean imageContentType = StringUtils.isBlank(contentType) || contentType.toLowerCase().startsWith("image/");
            if (!imageExtension || !imageContentType) {
                failList.add(toUploadFailVo(originalName, "仅支持图片文件"));
                continue;
            }
            imageFiles.add(file);
        }
        return imageFiles;
    }

    /** 将 OSS 的失败项转换为归档上传失败项。 */
    private List<SfInspectionPhotoArchiveUploadFailVo> toUploadFailVos(List<RemoteFileUploadFailVo> failItems) {
        if (failItems == null || failItems.isEmpty()) {
            return List.of();
        }
        return failItems.stream()
            .map(item -> toUploadFailVo(item.getFileName(), item.getMessage()))
            .toList();
    }

    /** 创建一项上传失败返回对象。 */
    private SfInspectionPhotoArchiveUploadFailVo toUploadFailVo(String fileName, String message) {
        SfInspectionPhotoArchiveUploadFailVo vo = new SfInspectionPhotoArchiveUploadFailVo();
        vo.setFileName(StringUtils.blankToDefault(fileName, "未命名文件"));
        vo.setMessage(StringUtils.blankToDefault(message, "上传失败"));
        return vo;
    }

    /** 将 OSS 成功结果批量转为已关联的归档照片实体。 */
    private List<SfInspectionPhotoArchivePhoto> buildUploadedPhotos(String tenantId, Long archiveFieldId,
                                                                      List<RemoteFileUploadVo> successItems,
                                                                      List<SfInspectionPhotoArchiveUploadFailVo> failList) {
        if (successItems == null || successItems.isEmpty()) {
            return List.of();
        }
        int nextSequence = archivePhotoMapper.selectByArchiveFieldIds(tenantId, List.of(archiveFieldId)).stream()
            .map(SfInspectionPhotoArchivePhoto::getSeq)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .map(value -> value + 1)
            .orElse(0);
        Date now = new Date();
        Long userId = LoginHelper.getUserId();
        List<SfInspectionPhotoArchivePhoto> photos = new ArrayList<>();
        for (RemoteFileUploadVo successItem : successItems) {
            Long ossId = parseOssId(successItem);
            if (ossId == null) {
                failList.add(toUploadFailVo(successItem == null ? null : successItem.getFileName(), "OSS 返回对象标识无效"));
                continue;
            }
            SfInspectionPhotoArchivePhoto photo = new SfInspectionPhotoArchivePhoto();
            photo.setPhotoId(IdWorker.getId());
            photo.setTenantId(tenantId);
            photo.setArchiveFieldId(archiveFieldId);
            photo.setOssId(ossId);
            photo.setOriginalName(successItem.getFileName());
            photo.setSeq(nextSequence++);
            photo.setCreateBy(userId);
            photo.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
            photo.setUpdateBy(userId);
            photo.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
            photos.add(photo);
        }
        return photos;
    }

    /** 解析 OSS 返回的字符串主键；异常格式不写入归档关联。 */
    private Long parseOssId(RemoteFileUploadVo item) {
        if (item == null || StringUtils.isBlank(item.getOssId())) {
            return null;
        }
        try {
            return Long.valueOf(item.getOssId());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** 计算同步可新增和可删除的快照集合，不将已有照片的历史快照纳入删除候选。 */
    private SyncCandidates calculateSyncCandidates(String tenantId, Long archiveId) {
        List<SfField> enabledGreenhouses = listEnabledGreenhouses(tenantId);
        List<SfInspectionPhotoArchiveField> archivedFields = archiveFieldMapper.selectByArchiveId(tenantId, archiveId);
        Map<Long, SfInspectionPhotoArchiveField> archivedBySourceFieldId = archivedFields.stream()
            .collect(Collectors.toMap(
                SfInspectionPhotoArchiveField::getFieldId,
                Function.identity(),
                (left, right) -> left));
        List<SfField> additions = enabledGreenhouses.stream()
            .filter(field -> !archivedBySourceFieldId.containsKey(field.getFieldId()))
            .toList();
        Set<Long> enabledFieldIds = enabledGreenhouses.stream()
            .map(SfField::getFieldId)
            .collect(Collectors.toSet());
        Set<Long> fieldsWithPhotos = archivePhotoMapper.selectByArchiveFieldIds(
                tenantId, archivedFields.stream().map(SfInspectionPhotoArchiveField::getArchiveFieldId).toList())
            .stream()
            .map(SfInspectionPhotoArchivePhoto::getArchiveFieldId)
            .collect(Collectors.toSet());
        List<SfInspectionPhotoArchiveField> removals = archivedFields.stream()
            .filter(field -> !enabledFieldIds.contains(field.getFieldId()))
            .filter(field -> !fieldsWithPhotos.contains(field.getArchiveFieldId()))
            .toList();
        return new SyncCandidates(additions, removals);
    }

    /** 将同步候选集合转换为前端可直接判断按钮的预览结果。 */
    private SfInspectionPhotoArchiveSyncPreviewVo toSyncPreview(SyncCandidates candidates) {
        int addCount = candidates.additions().size();
        int removeCount = candidates.removals().size();
        SfInspectionPhotoArchiveSyncPreviewVo preview = new SfInspectionPhotoArchiveSyncPreviewVo();
        preview.setAddCount(addCount);
        preview.setRemoveCount(removeCount);
        preview.setMode(resolveSyncMode(addCount, removeCount));
        return preview;
    }

    /** 确定同步预览状态。 */
    private String resolveSyncMode(int addCount, int removeCount) {
        if (addCount == 0 && removeCount == 0) {
            return "NONE";
        }
        if (addCount > 0 && removeCount > 0) {
            return "BOTH";
        }
        return addCount > 0 ? "ADD" : "REMOVE";
    }

    /** 创建归档头并补齐审计与租户字段。 */
    private void fillNewArchive(SfInspectionPhotoArchive archive, String tenantId, LocalDate archiveDate) {
        Date now = new Date();
        Long userId = LoginHelper.getUserId();
        archive.setArchiveId(IdWorker.getId());
        archive.setTenantId(tenantId);
        archive.setArchiveDate(archiveDate);
        archive.setCreateBy(userId);
        archive.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        archive.setUpdateBy(userId);
        archive.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
    }

    /** 将归档实体转换为日期列表项。 */
    private SfInspectionPhotoArchiveDateVo toDateVo(SfInspectionPhotoArchive archive) {
        return BeanUtil.copyProperties(archive, SfInspectionPhotoArchiveDateVo.class);
    }

    /** 校验并读取当前租户归档。 */
    private SfInspectionPhotoArchive requireArchive(String tenantId, Long archiveId) {
        SfInspectionPhotoArchive archive = archiveMapper.selectTenantById(tenantId, archiveId);
        if (archive == null) {
            throw new ServiceException("归档日期不存在");
        }
        return archive;
    }

    /** 以行锁校验并读取当前租户归档，用于与上传、删除、同步互斥。 */
    private SfInspectionPhotoArchive requireLockedArchive(String tenantId, Long archiveId) {
        SfInspectionPhotoArchive archive = archiveMapper.selectTenantByIdForUpdate(tenantId, archiveId);
        if (archive == null) {
            throw new ServiceException("归档日期不存在");
        }
        return archive;
    }

    /** 校验归档大棚快照归属于当前归档日期和当前租户。 */
    private SfInspectionPhotoArchiveField requireArchiveField(String tenantId, Long archiveId, Long archiveFieldId) {
        SfInspectionPhotoArchiveField field = archiveFieldMapper.selectTenantArchiveField(
            tenantId, archiveId, archiveFieldId);
        if (field == null) {
            throw new ServiceException("归档大棚不存在");
        }
        return field;
    }

    /** 从租户上下文读取明确的租户编号，禁止在无租户场景降级访问。 */
    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("未获取到当前租户");
        }
        return tenantId;
    }

    /** 同步时一次性持有新增来源与可删快照集合。 */
    private record SyncCandidates(List<SfField> additions, List<SfInspectionPhotoArchiveField> removals) {
    }
}
