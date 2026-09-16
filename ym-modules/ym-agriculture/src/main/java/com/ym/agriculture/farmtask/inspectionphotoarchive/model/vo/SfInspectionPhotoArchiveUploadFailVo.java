package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 巡查归档图片上传失败项。
 *
 * @author ym-cloud
 */
@Data
public class SfInspectionPhotoArchiveUploadFailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 失败文件的原始文件名。 */
    private String fileName;

    /** 面向操作人员的失败原因。 */
    private String message;
}
