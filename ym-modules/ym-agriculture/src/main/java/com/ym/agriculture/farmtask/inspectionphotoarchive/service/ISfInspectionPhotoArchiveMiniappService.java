package com.ym.agriculture.farmtask.inspectionphotoarchive.service;

import com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo.SfInspectionPhotoArchiveMiniappCreateBo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappFieldDetailVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappPageVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappUploadVo;
import org.springframework.web.multipart.MultipartFile;

/** 巡棚拍照小程序专用服务。 */
public interface ISfInspectionPhotoArchiveMiniappService {

    /** 查询列表页，未传归档主键时默认最近创建日期。 */
    SfInspectionPhotoArchiveMiniappPageVo getPage(Long archiveId);

    /** 创建归档日期并返回其列表页数据。 */
    SfInspectionPhotoArchiveMiniappPageVo create(SfInspectionPhotoArchiveMiniappCreateBo bo);

    /** 查询一个归档日期下的大棚拍照页。 */
    SfInspectionPhotoArchiveMiniappFieldDetailVo getFieldDetail(Long archiveId, Long archiveFieldId);

    /** 上传一张照片，按客户端标识保证幂等。 */
    SfInspectionPhotoArchiveMiniappUploadVo uploadPhoto(Long archiveId, Long archiveFieldId,
                                                          String clientUploadId, MultipartFile file);

    /** 删除当前登录人员本人上传的一张照片关联。 */
    boolean removeOwnPhoto(Long archiveId, Long archiveFieldId, Long photoId);
}
