package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 巡查归档图片上传结果；单批允许部分成功。
 *
 * @author ym-cloud
 */
@Data
public class SfInspectionPhotoArchiveUploadVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 已成功上传并立即关联到归档大棚的照片。 */
    private List<SfInspectionPhotoArchivePhotoVo> successList;

    /** 未上传或未关联成功的文件及原因。 */
    private List<SfInspectionPhotoArchiveUploadFailVo> failList;
}
