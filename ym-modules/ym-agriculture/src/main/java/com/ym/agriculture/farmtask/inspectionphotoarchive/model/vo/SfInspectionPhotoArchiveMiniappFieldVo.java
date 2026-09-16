package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 小程序列表页的大棚轻量数据。 */
@Data
public class SfInspectionPhotoArchiveMiniappFieldVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前大棚快照所属的归档主键。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long archiveId;

    /** 归档大棚快照主键。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long archiveFieldId;

    /** 来源大棚主键。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fieldId;

    /** 归档时的大棚编号。 */
    private String fieldCode;

    /** 归档时的大棚名称。 */
    private String fieldName;

    /** 归档时的作物展示名。 */
    private String cropDisplayName;

    /** 当前成功照片数。 */
    private Integer photoCount;

    /** 拍照状态：UPLOADED-已有成功照片，PENDING-尚未上传成功。 */
    private String uploadStatus;
}
