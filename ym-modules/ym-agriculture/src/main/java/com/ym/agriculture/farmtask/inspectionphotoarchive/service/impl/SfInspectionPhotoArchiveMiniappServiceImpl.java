package com.ym.agriculture.farmtask.inspectionphotoarchive.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.core.constant.HttpStatus;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.core.utils.file.MimeTypeUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.field.layout.model.vo.GreenhouseLayoutVo;
import com.ym.agriculture.farming.field.layout.service.IGreenhouseLayoutService;
import com.ym.agriculture.farmtask.inspectionphotoarchive.dao.SfInspectionPhotoArchiveFieldMapper;
import com.ym.agriculture.farmtask.inspectionphotoarchive.dao.SfInspectionPhotoArchiveMapper;
import com.ym.agriculture.farmtask.inspectionphotoarchive.dao.SfInspectionPhotoArchivePhotoMapper;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo.SfInspectionPhotoArchiveCreateBo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo.SfInspectionPhotoArchiveMiniappCreateBo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchive;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchiveField;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchivePhoto;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveFieldPhotoCountVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappDateVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappFieldDetailVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappFieldVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappGroupVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappPageVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappPhotoVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappSummaryVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappUploadVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.service.ISfInspectionPhotoArchiveMiniappService;
import com.ym.agriculture.farmtask.inspectionphotoarchive.service.ISfInspectionPhotoArchiveService;
import com.ym.agriculture.farmtask.inspectionphotoarchive.support.SfInspectionPhotoArchiveMasterOssAccessor;
import com.ym.agriculture.farmtask.inspectionphotoarchive.support.SfInspectionPhotoArchiveMasterPermissionAccessor;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 巡棚拍照小程序专用聚合服务实现。 */
@Service
@RequiredArgsConstructor
public class SfInspectionPhotoArchiveMiniappServiceImpl implements ISfInspectionPhotoArchiveMiniappService {

    /** 未配置当前布局的大棚归属分组。 */
    private static final String UNGROUPED_NAME = "未分组";

    /** 已成功上传照片的大棚状态。 */
    private static final String UPLOAD_STATUS_UPLOADED = "UPLOADED";

    /** 尚无成功上传照片的大棚状态。 */
    private static final String UPLOAD_STATUS_PENDING = "PENDING";

    private final SfInspectionPhotoArchiveMapper archiveMapper;
    private final SfInspectionPhotoArchiveFieldMapper archiveFieldMapper;
    private final SfInspectionPhotoArchivePhotoMapper archivePhotoMapper;
    private final ISfInspectionPhotoArchiveService archiveService;
    private final IGreenhouseLayoutService greenhouseLayoutService;
    private final SfInspectionPhotoArchiveMasterOssAccessor masterOssAccessor;
    private final SfInspectionPhotoArchiveMasterPermissionAccessor masterPermissionAccessor;

    @Override
    public SfInspectionPhotoArchiveMiniappPageVo getPage(Long archiveId) {
        String tenantId = requireMiniappAccess();
        return TenantHelper.dynamic(tenantId, () -> buildPage(tenantId, archiveId));
    }

    @Override
    public SfInspectionPhotoArchiveMiniappPageVo create(SfInspectionPhotoArchiveMiniappCreateBo bo) {
        String tenantId = requireMiniappAccess();
        return TenantHelper.dynamic(tenantId, () -> {
            SfInspectionPhotoArchiveCreateBo createBo = BeanUtil.copyProperties(bo, SfInspectionPhotoArchiveCreateBo.class);
            archiveService.create(createBo);
            SfInspectionPhotoArchive archive = archiveMapper.selectTenantByDate(tenantId, bo.getArchiveDate());
            if (archive == null) {
                throw new ServiceException("创建归档日期失败");
            }
            return buildPage(tenantId, archive.getArchiveId());
        });
    }

    @Override
    public SfInspectionPhotoArchiveMiniappFieldDetailVo getFieldDetail(Long archiveId, Long archiveFieldId) {
        String tenantId = requireMiniappAccess();
        return TenantHelper.dynamic(tenantId, () -> {
            SfInspectionPhotoArchive archive = requireArchive(tenantId, archiveId);
            SfInspectionPhotoArchiveField field = requireArchiveField(tenantId, archiveId, archiveFieldId);
            List<SfInspectionPhotoArchivePhoto> photos = archivePhotoMapper.selectByArchiveFieldIds(tenantId, List.of(archiveFieldId));
            Map<Long, String> urlByOssId = loadPhotoUrlMap(tenantId, photos);

            SfInspectionPhotoArchiveMiniappFieldDetailVo detail = BeanUtil.copyProperties(field,
                SfInspectionPhotoArchiveMiniappFieldDetailVo.class);
            detail.setArchiveId(archive.getArchiveId());
            detail.setArchiveDate(archive.getArchiveDate());
            detail.setPhotos(photos.stream().map(photo -> toPhotoVo(photo, urlByOssId.get(photo.getOssId()))).toList());
            detail.setSelectableDates(resolveSelectableDates(tenantId, archive, field));
            return detail;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfInspectionPhotoArchiveMiniappUploadVo uploadPhoto(Long archiveId, Long archiveFieldId,
                                                                 String clientUploadId, MultipartFile file) {
        String tenantId = requireMiniappAccess();
        return TenantHelper.dynamic(tenantId, () -> uploadPhotoForTenant(tenantId, archiveId, archiveFieldId,
            clientUploadId, file));
    }

    private SfInspectionPhotoArchiveMiniappUploadVo uploadPhotoForTenant(String tenantId, Long archiveId,
                                                                            Long archiveFieldId,
                                                                            String clientUploadId, MultipartFile file) {
        requireArchiveForUpdate(tenantId, archiveId);
        requireArchiveField(tenantId, archiveId, archiveFieldId);
        SfInspectionPhotoArchivePhoto existing = archivePhotoMapper.selectByClientUploadId(tenantId, archiveFieldId,
            clientUploadId);
        if (existing != null) {
            SfInspectionPhotoArchiveMiniappUploadVo result = new SfInspectionPhotoArchiveMiniappUploadVo();
            result.setPhoto(toPhotoVo(existing, loadPhotoUrlMap(tenantId, List.of(existing)).get(existing.getOssId())));
            result.setIdempotentHit(true);
            return result;
        }
        validateImage(file);
        RemoteFileUploadVo upload = uploadOne(tenantId, file);
        Long ossId = parseOssId(upload);
        if (ossId == null) {
            throw new ServiceException("上传图片失败：OSS 返回对象标识无效");
        }
        SfInspectionPhotoArchivePhoto photo = new SfInspectionPhotoArchivePhoto();
        photo.setPhotoId(IdWorker.getId());
        photo.setTenantId(tenantId);
        photo.setArchiveFieldId(archiveFieldId);
        photo.setOssId(ossId);
        photo.setOriginalName(upload.getFileName());
        photo.setClientUploadId(clientUploadId);
        photo.setSeq(nextPhotoSequence(tenantId, archiveFieldId));
        photo.setCreateBy(LoginHelper.getUserId());
        photo.setCreateTime(java.time.LocalDateTime.now());
        photo.setUpdateBy(LoginHelper.getUserId());
        photo.setUpdateTime(java.time.LocalDateTime.now());
        try {
            archivePhotoMapper.insert(photo);
        } catch (DuplicateKeyException exception) {
            SfInspectionPhotoArchivePhoto duplicated = archivePhotoMapper.selectByClientUploadId(tenantId, archiveFieldId,
                clientUploadId);
            if (duplicated == null) {
                throw exception;
            }
            SfInspectionPhotoArchiveMiniappUploadVo result = new SfInspectionPhotoArchiveMiniappUploadVo();
            result.setPhoto(toPhotoVo(duplicated,
                loadPhotoUrlMap(tenantId, List.of(duplicated)).get(duplicated.getOssId())));
            result.setIdempotentHit(true);
            return result;
        }
        SfInspectionPhotoArchiveMiniappUploadVo result = new SfInspectionPhotoArchiveMiniappUploadVo();
        result.setPhoto(toPhotoVo(photo, upload.getUrl()));
        result.setIdempotentHit(false);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeOwnPhoto(Long archiveId, Long archiveFieldId, Long photoId) {
        String tenantId = requireMiniappAccess();
        return TenantHelper.dynamic(tenantId,
            () -> removeOwnPhotoForTenant(tenantId, archiveId, archiveFieldId, photoId));
    }

    private boolean removeOwnPhotoForTenant(String tenantId, Long archiveId, Long archiveFieldId, Long photoId) {
        requireArchiveForUpdate(tenantId, archiveId);
        requireArchiveField(tenantId, archiveId, archiveFieldId);
        SfInspectionPhotoArchivePhoto photo = archivePhotoMapper.selectTenantArchivePhoto(tenantId, archiveFieldId, photoId);
        if (photo == null) {
            throw new ServiceException("归档照片不存在");
        }
        if (!Objects.equals(photo.getCreateBy(), LoginHelper.getUserId())) {
            throw new ServiceException("只能删除本人上传的照片");
        }
        if (archivePhotoMapper.deleteTenantArchivePhoto(tenantId, archiveFieldId, photoId) <= 0) {
            throw new ServiceException("删除归档照片失败");
        }
        return true;
    }

    private SfInspectionPhotoArchiveMiniappPageVo buildPage(String tenantId, Long requestedArchiveId) {
        List<SfInspectionPhotoArchive> archives = archiveMapper.selectDateList(tenantId);
        SfInspectionPhotoArchiveMiniappPageVo page = new SfInspectionPhotoArchiveMiniappPageVo();
        page.setDates(archives.stream().map(this::toDateVo).toList());
        page.setGroups(List.of());
        if (archives.isEmpty()) {
            return page;
        }
        SfInspectionPhotoArchive archive = requestedArchiveId == null ? archives.get(0)
            : archives.stream().filter(item -> Objects.equals(item.getArchiveId(), requestedArchiveId)).findFirst()
                .orElseThrow(() -> new ServiceException("归档日期不存在"));
        List<SfInspectionPhotoArchiveField> fields = archiveFieldMapper.selectByArchiveId(tenantId, archive.getArchiveId());
        Map<Long, Integer> photoCountByFieldId = countPhotos(tenantId, fields);
        List<SfInspectionPhotoArchiveMiniappFieldVo> fieldVos = fields.stream()
            .map(field -> toFieldVo(field, photoCountByFieldId.getOrDefault(field.getArchiveFieldId(), 0)))
            .toList();
        page.setSelectedArchiveId(archive.getArchiveId());
        page.setSummary(toSummary(fieldVos));
        page.setGroups(groupFieldsByCurrentLayout(fieldVos));
        return page;
    }

    private Map<Long, Integer> countPhotos(String tenantId, Collection<SfInspectionPhotoArchiveField> fields) {
        List<Long> archiveFieldIds = fields.stream().map(SfInspectionPhotoArchiveField::getArchiveFieldId).toList();
        if (archiveFieldIds.isEmpty()) {
            return Map.of();
        }
        return archiveFieldMapper.countPhotosByArchiveFieldIds(tenantId, archiveFieldIds).stream()
            .collect(Collectors.toMap(SfInspectionPhotoArchiveFieldPhotoCountVo::getArchiveFieldId,
                item -> item.getPhotoCount() == null ? 0 : Math.toIntExact(item.getPhotoCount()), (left, right) -> left));
    }

    private SfInspectionPhotoArchiveMiniappSummaryVo toSummary(List<SfInspectionPhotoArchiveMiniappFieldVo> fields) {
        int uploadedCount = (int) fields.stream()
            .filter(field -> UPLOAD_STATUS_UPLOADED.equals(field.getUploadStatus()))
            .count();
        SfInspectionPhotoArchiveMiniappSummaryVo summary = new SfInspectionPhotoArchiveMiniappSummaryVo();
        summary.setTotalFieldCount(fields.size());
        summary.setUploadedFieldCount(uploadedCount);
        summary.setPendingFieldCount(fields.size() - uploadedCount);
        return summary;
    }

    private List<SfInspectionPhotoArchiveMiniappGroupVo> groupFieldsByCurrentLayout(
        List<SfInspectionPhotoArchiveMiniappFieldVo> fields) {
        Map<Long, SfInspectionPhotoArchiveMiniappFieldVo> fieldById = fields.stream()
            .collect(Collectors.toMap(SfInspectionPhotoArchiveMiniappFieldVo::getFieldId, item -> item));
        Map<Long, Boolean> placedFieldIds = new HashMap<>();
        List<SfInspectionPhotoArchiveMiniappGroupVo> groups = new ArrayList<>();
        GreenhouseLayoutVo layout = greenhouseLayoutService.getLayout();
        List<GreenhouseLayoutVo.Column> columns = layout == null || layout.getColumns() == null ? List.of() : layout.getColumns();
        for (GreenhouseLayoutVo.Column column : columns) {
            List<SfInspectionPhotoArchiveMiniappFieldVo> groupFields = (column.getRows() == null ? List.<GreenhouseLayoutVo.Row>of()
                : column.getRows()).stream()
                .map(GreenhouseLayoutVo.Row::getFieldId)
                .map(fieldById::get)
                .filter(Objects::nonNull)
                .peek(field -> placedFieldIds.put(field.getFieldId(), Boolean.TRUE))
                .toList();
            if (groupFields.isEmpty()) {
                continue;
            }
            SfInspectionPhotoArchiveMiniappGroupVo group = new SfInspectionPhotoArchiveMiniappGroupVo();
            group.setGroupName(column.getColumnName());
            group.setGroupOrder(column.getColumnOrder());
            group.setFields(groupFields);
            groups.add(group);
        }
        List<SfInspectionPhotoArchiveMiniappFieldVo> ungroupedFields = fields.stream()
            .filter(field -> !placedFieldIds.containsKey(field.getFieldId()))
            .sorted(Comparator.comparing(SfInspectionPhotoArchiveMiniappFieldVo::getFieldCode,
                Comparator.nullsLast(String::compareTo)))
            .toList();
        if (!ungroupedFields.isEmpty()) {
            SfInspectionPhotoArchiveMiniappGroupVo group = new SfInspectionPhotoArchiveMiniappGroupVo();
            group.setGroupName(UNGROUPED_NAME);
            group.setGroupOrder(columns.stream().map(GreenhouseLayoutVo.Column::getColumnOrder)
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 1);
            group.setFields(ungroupedFields);
            groups.add(group);
        }
        return groups;
    }

    private List<SfInspectionPhotoArchiveMiniappDateVo> resolveSelectableDates(String tenantId,
                                                                                 SfInspectionPhotoArchive archive,
                                                                                 SfInspectionPhotoArchiveField field) {
        Map<Long, SfInspectionPhotoArchiveMiniappDateVo> dateByArchiveId = new LinkedHashMap<>();
        SfInspectionPhotoArchiveMiniappDateVo currentDate = toDateVo(archive);
        dateByArchiveId.put(currentDate.getArchiveId(), currentDate);
        archivePhotoMapper.selectHistoryDatesByFieldId(tenantId, field.getFieldId())
            .forEach(item -> dateByArchiveId.putIfAbsent(item.getArchiveId(), item));
        return dateByArchiveId.values().stream()
            .sorted(Comparator.comparing(SfInspectionPhotoArchiveMiniappDateVo::getArchiveDate).reversed()
                .thenComparing(SfInspectionPhotoArchiveMiniappDateVo::getArchiveId, Comparator.reverseOrder()))
            .toList();
    }

    private SfInspectionPhotoArchiveMiniappFieldVo toFieldVo(SfInspectionPhotoArchiveField field, int photoCount) {
        SfInspectionPhotoArchiveMiniappFieldVo vo = BeanUtil.copyProperties(field, SfInspectionPhotoArchiveMiniappFieldVo.class);
        vo.setArchiveId(field.getArchiveId());
        vo.setCropDisplayName(resolveCropDisplayName(field));
        vo.setPhotoCount(photoCount);
        vo.setUploadStatus(photoCount > 0 ? UPLOAD_STATUS_UPLOADED : UPLOAD_STATUS_PENDING);
        return vo;
    }

    private String resolveCropDisplayName(SfInspectionPhotoArchiveField field) {
        boolean hasSpecies = StringUtils.isNotBlank(field.getSpeciesName());
        boolean hasVariety = StringUtils.isNotBlank(field.getVarietyName());
        if (!hasSpecies && !hasVariety) {
            return "未种植";
        }
        if (!hasSpecies) {
            return field.getVarietyName();
        }
        return hasVariety ? field.getSpeciesName() + " / " + field.getVarietyName() : field.getSpeciesName();
    }

    private SfInspectionPhotoArchiveMiniappDateVo toDateVo(SfInspectionPhotoArchive archive) {
        return BeanUtil.copyProperties(archive, SfInspectionPhotoArchiveMiniappDateVo.class);
    }

    private SfInspectionPhotoArchiveMiniappPhotoVo toPhotoVo(SfInspectionPhotoArchivePhoto photo, String url) {
        SfInspectionPhotoArchiveMiniappPhotoVo vo = BeanUtil.copyProperties(photo, SfInspectionPhotoArchiveMiniappPhotoVo.class);
        vo.setUrl(url);
        return vo;
    }

    private Map<Long, String> loadPhotoUrlMap(String tenantId, Collection<SfInspectionPhotoArchivePhoto> photos) {
        List<Long> ossIds = photos.stream().map(SfInspectionPhotoArchivePhoto::getOssId)
            .filter(Objects::nonNull).distinct().toList();
        if (ossIds.isEmpty()) {
            return Map.of();
        }
        return masterOssAccessor.listByIds(tenantId, ossIds).stream()
            .collect(Collectors.toMap(RemoteFile::getOssId, RemoteFile::getUrl, (left, right) -> left));
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择一张图片");
        }
        String extension = FileUtil.extName(file.getOriginalFilename());
        String contentType = file.getContentType();
        boolean imageExtension = StringUtils.equalsAnyIgnoreCase(extension, MimeTypeUtils.IMAGE_EXTENSION);
        boolean imageContentType = StringUtils.isBlank(contentType) || contentType.toLowerCase().startsWith("image/");
        if (!imageExtension || !imageContentType) {
            throw new ServiceException("仅支持图片文件");
        }
    }

    private RemoteFileUploadVo uploadOne(String tenantId, MultipartFile file) {
        RemoteFileBatchUploadVo result = masterOssAccessor.uploadBatch(tenantId, new MultipartFile[] {file});
        if (result == null || CollUtil.isEmpty(result.getSuccessList())) {
            RemoteFileUploadFailVo failure = result == null || CollUtil.isEmpty(result.getFailList()) ? null
                : result.getFailList().get(0);
            throw new ServiceException("上传图片失败" + (failure == null || StringUtils.isBlank(failure.getMessage())
                ? "" : "：" + failure.getMessage()));
        }
        return result.getSuccessList().get(0);
    }

    private Long parseOssId(RemoteFileUploadVo upload) {
        try {
            return upload == null || StringUtils.isBlank(upload.getOssId()) ? null : Long.valueOf(upload.getOssId());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int nextPhotoSequence(String tenantId, Long archiveFieldId) {
        return archivePhotoMapper.selectByArchiveFieldIds(tenantId, List.of(archiveFieldId)).stream()
            .map(SfInspectionPhotoArchivePhoto::getSeq).filter(Objects::nonNull).max(Integer::compareTo)
            .map(value -> value + 1).orElse(0);
    }

    private SfInspectionPhotoArchive requireArchive(String tenantId, Long archiveId) {
        SfInspectionPhotoArchive archive = archiveMapper.selectTenantById(tenantId, archiveId);
        if (archive == null) {
            throw new ServiceException("归档日期不存在、已被删除或不属于当前租户", HttpStatus.NOT_FOUND);
        }
        return archive;
    }

    private SfInspectionPhotoArchive requireArchiveForUpdate(String tenantId, Long archiveId) {
        SfInspectionPhotoArchive archive = archiveMapper.selectTenantByIdForUpdate(tenantId, archiveId);
        if (archive == null) {
            throw new ServiceException("归档日期不存在、已被删除或不属于当前租户", HttpStatus.NOT_FOUND);
        }
        return archive;
    }

    private SfInspectionPhotoArchiveField requireArchiveField(String tenantId, Long archiveId, Long archiveFieldId) {
        SfInspectionPhotoArchiveField field = archiveFieldMapper.selectTenantArchiveField(tenantId, archiveId, archiveFieldId);
        if (field == null) {
            throw new ServiceException("归档大棚不存在、已被删除或不属于当前租户", HttpStatus.NOT_FOUND);
        }
        return field;
    }

    private String requireMiniappAccess() {
        if (!EmployeeConstants.USER_TYPE_WX_EMPLOYEE.equals(LoginHelper.getLoginUser().getUserType())) {
            throw new ServiceException("当前登录身份不是小程序人员");
        }
        return masterPermissionAccessor.requireInspectionPhotoAccess(LoginHelper.getUserId());
    }
}
