package com.ym.agriculture.farmtask.inspectionphotoarchive.service;

import com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo.SfInspectionPhotoArchiveCreateBo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo.SfInspectionPhotoArchiveSyncBo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveDateVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveDetailVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveSyncPreviewVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveSyncResultVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveUploadVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 巡查照片归档服务。
 *
 * @author ym-cloud
 */
public interface ISfInspectionPhotoArchiveService {

    /**
     * 查询当前租户全部巡查归档日期。
     *
     * @return 日期列表，按日期倒序
     */
    List<SfInspectionPhotoArchiveDateVo> listDates();

    /**
     * 查询一日归档详情及全部大棚照片。
     *
     * @param archiveId 归档主键
     * @return 归档详情
     */
    SfInspectionPhotoArchiveDetailVo getDetail(Long archiveId);

    /**
     * 创建归档日期，并将当前启用大棚和当前作物写入历史快照。
     *
     * @param bo 创建入参
     * @return 新建归档详情
     */
    SfInspectionPhotoArchiveDetailVo create(SfInspectionPhotoArchiveCreateBo bo);

    /**
     * 删除一日归档及其大棚、照片关联；不删除 OSS 原文件。
     *
     * @param archiveId 归档主键
     * @return 是否删除成功
     */
    boolean removeArchive(Long archiveId);

    /**
     * 上传图片并立即关联到指定归档大棚。
     *
     * @param archiveId      归档主键
     * @param archiveFieldId 归档大棚快照主键
     * @param files          图片文件，单次最多 30 张
     * @return 每个文件的成功或失败结果
     */
    SfInspectionPhotoArchiveUploadVo uploadPhotos(Long archiveId, Long archiveFieldId, MultipartFile[] files);

    /**
     * 删除一张归档照片关联；不删除 OSS 原文件。
     *
     * @param archiveId      归档主键
     * @param archiveFieldId 归档大棚快照主键
     * @param photoId        照片关联主键
     * @return 是否删除成功
     */
    boolean removePhoto(Long archiveId, Long archiveFieldId, Long photoId);

    /**
     * 预览当前启用大棚与归档快照之间的同步差异。
     *
     * @param archiveId 归档主键
     * @return 可新增和可删除数量
     */
    SfInspectionPhotoArchiveSyncPreviewVo previewSync(Long archiveId);

    /**
     * 按当前数据重新计算差异并执行指定同步动作。
     *
     * @param archiveId 归档主键
     * @param bo        同步动作入参
     * @return 实际新增和删除数量
     */
    SfInspectionPhotoArchiveSyncResultVo syncGreenhouses(Long archiveId, SfInspectionPhotoArchiveSyncBo bo);
}
