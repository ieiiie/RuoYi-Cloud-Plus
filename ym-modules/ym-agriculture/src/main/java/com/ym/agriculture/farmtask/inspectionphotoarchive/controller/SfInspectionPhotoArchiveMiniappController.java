package com.ym.agriculture.farmtask.inspectionphotoarchive.controller;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo.SfInspectionPhotoArchiveMiniappCreateBo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappFieldDetailVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappPageVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappUploadVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.service.ISfInspectionPhotoArchiveMiniappService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 巡棚拍照小程序专用接口，不影响管理端巡查照片归档接口。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/miniapp/smart-farming/inspection-photo-archives")
public class SfInspectionPhotoArchiveMiniappController extends BaseController {

    private final ISfInspectionPhotoArchiveMiniappService miniappService;

    /**
     * 查询列表页数据；不传归档主键时选择最近创建日期。
     *
     * @param archiveId 指定归档主键，可为空
     * @return 日期选项、汇总与分组大棚轻量列表
     */
    @GetMapping("/archives")
    public R<SfInspectionPhotoArchiveMiniappPageVo> page(@RequestParam(required = false) Long archiveId) {
        return R.ok(miniappService.getPage(archiveId));
    }

    /**
     * 创建一条小程序使用的巡查归档日期。
     *
     * @param bo 创建日期入参
     * @return 新建日期的列表页数据
     */
    @PostMapping("/archives")
    public R<SfInspectionPhotoArchiveMiniappPageVo> create(@Valid @RequestBody SfInspectionPhotoArchiveMiniappCreateBo bo) {
        return R.ok(miniappService.create(bo));
    }

    /**
     * 查询指定归档日期下的大棚拍照详情。
     *
     * @param archiveId 归档主键
     * @param archiveFieldId 归档大棚快照主键
     * @return 大棚信息、成功照片及可切换日期
     */
    @GetMapping("/archives/{archiveId}/fields/{archiveFieldId}")
    public R<SfInspectionPhotoArchiveMiniappFieldDetailVo> fieldDetail(@NotNull @PathVariable Long archiveId,
                                                                         @NotNull @PathVariable Long archiveFieldId) {
        return R.ok(miniappService.getFieldDetail(archiveId, archiveFieldId));
    }

    /**
     * 立即上传一张现场照片；客户端标识用于网络重试幂等。
     *
     * @param archiveId 归档主键
     * @param archiveFieldId 归档大棚快照主键
     * @param clientUploadId 小程序持久化生成的单张上传标识
     * @param file 图片文件
     * @return 成功关联的照片与幂等命中标识
     */
    @PostMapping(value = "/archives/{archiveId}/fields/{archiveFieldId}/photos",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SfInspectionPhotoArchiveMiniappUploadVo> uploadPhoto(@NotNull @PathVariable Long archiveId,
                                                                    @NotNull @PathVariable Long archiveFieldId,
                                                                    @NotBlank @Size(max = 64) @RequestParam String clientUploadId,
                                                                    @RequestPart("file") MultipartFile file) {
        return R.ok(miniappService.uploadPhoto(archiveId, archiveFieldId, clientUploadId, file));
    }

    /**
     * 删除当前小程序人员本人上传的一张归档照片。
     *
     * @param archiveId 归档主键
     * @param archiveFieldId 归档大棚快照主键
     * @param photoId 照片关联主键
     * @return 删除结果
     */
    @DeleteMapping("/archives/{archiveId}/fields/{archiveFieldId}/photos/{photoId}")
    public R<Void> removeOwnPhoto(@NotNull @PathVariable Long archiveId,
                                  @NotNull @PathVariable Long archiveFieldId,
                                  @NotNull @PathVariable Long photoId) {
        return toAjax(miniappService.removeOwnPhoto(archiveId, archiveFieldId, photoId));
    }
}
