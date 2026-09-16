package com.ym.agriculture.farmtask.inspectionphotoarchive.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo.SfInspectionPhotoArchiveCreateBo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo.SfInspectionPhotoArchiveSyncBo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveDateVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveDetailVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveSyncPreviewVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveSyncResultVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveUploadVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.service.ISfInspectionPhotoArchiveService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理端巡查照片归档接口。
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/inspection-photo-archives")
public class SfInspectionPhotoArchiveController extends BaseController {

    /** 巡查照片归档领域服务。 */
    private final ISfInspectionPhotoArchiveService archiveService;

    /**
     * 查询当前租户的巡查归档日期。
     *
     * @return 按日期倒序排列的归档日期列表
     */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:list")
    @GetMapping
    public R<List<SfInspectionPhotoArchiveDateVo>> listDates() {
        return R.ok(archiveService.listDates());
    }

    /**
     * 创建一个归档日期，并自动生成所有当前启用大棚的快照。
     *
     * @param bo 日期入参
     * @return 新建后的完整归档详情
     */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:add")
    @Log(title = "巡查照片归档", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<SfInspectionPhotoArchiveDetailVo> create(@Valid @RequestBody SfInspectionPhotoArchiveCreateBo bo) {
        return R.ok(archiveService.create(bo));
    }

    /**
     * 查询一个归档日期及全部大棚照片。
     *
     * @param archiveId 归档主键
     * @return 归档详情
     */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:list")
    @GetMapping("/{archiveId}")
    public R<SfInspectionPhotoArchiveDetailVo> getDetail(@NotNull @PathVariable Long archiveId) {
        return R.ok(archiveService.getDetail(archiveId));
    }

    /**
     * 删除一个归档日期及其内部关联；OSS 原图仍保留在系统文件库。
     *
     * @param archiveId 归档主键
     * @return 删除结果
     */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:remove")
    @Log(title = "巡查照片归档", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/{archiveId}")
    public R<Void> removeArchive(@NotNull @PathVariable Long archiveId) {
        return toAjax(archiveService.removeArchive(archiveId));
    }

    /**
     * 为指定归档大棚上传图片并在上传成功后立即关联。
     *
     * @param archiveId      归档主键
     * @param archiveFieldId 归档大棚快照主键
     * @param files          图片文件数组，表单字段名固定为 files
     * @return 文件级成功与失败结果
     */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:upload")
    @Log(title = "巡查照片归档图片", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping(value = "/{archiveId}/archive-fields/{archiveFieldId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SfInspectionPhotoArchiveUploadVo> uploadPhotos(@NotNull @PathVariable Long archiveId,
                                                            @NotNull @PathVariable Long archiveFieldId,
                                                            @RequestPart("files") MultipartFile[] files) {
        return R.ok(archiveService.uploadPhotos(archiveId, archiveFieldId, files));
    }

    /**
     * 删除一张归档照片关联，不删除 OSS 原图。
     *
     * @param archiveId      归档主键
     * @param archiveFieldId 归档大棚快照主键
     * @param photoId        照片关联主键
     * @return 删除结果
     */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:remove")
    @Log(title = "巡查照片归档图片", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/{archiveId}/archive-fields/{archiveFieldId}/photos/{photoId}")
    public R<Void> removePhoto(@NotNull @PathVariable Long archiveId,
                               @NotNull @PathVariable Long archiveFieldId,
                               @NotNull @PathVariable Long photoId) {
        return toAjax(archiveService.removePhoto(archiveId, archiveFieldId, photoId));
    }

    /**
     * 预览当前启用大棚相对归档快照的增删差异。
     *
     * @param archiveId 归档主键
     * @return 同步预览
     */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:sync")
    @GetMapping("/{archiveId}/greenhouse-sync-preview")
    public R<SfInspectionPhotoArchiveSyncPreviewVo> previewSync(@NotNull @PathVariable Long archiveId) {
        return R.ok(archiveService.previewSync(archiveId));
    }

    /**
     * 按当前数据重新计算差异并同步大棚快照。
     *
     * @param archiveId 归档主键
     * @param bo        同步动作
     * @return 实际同步数量
     */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:sync")
    @Log(title = "巡查照片归档大棚同步", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/{archiveId}/greenhouse-sync")
    public R<SfInspectionPhotoArchiveSyncResultVo> syncGreenhouses(@NotNull @PathVariable Long archiveId,
                                                                    @Valid @RequestBody SfInspectionPhotoArchiveSyncBo bo) {
        return R.ok(archiveService.syncGreenhouses(archiveId, bo));
    }
}
