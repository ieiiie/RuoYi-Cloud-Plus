package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 小程序单张照片上传结果。 */
@Data
public class SfInspectionPhotoArchiveMiniappUploadVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 已成功关联的照片。 */
    private SfInspectionPhotoArchiveMiniappPhotoVo photo;

    /** 是否命中同一 clientUploadId 的幂等重试。 */
    private Boolean idempotentHit;
}
