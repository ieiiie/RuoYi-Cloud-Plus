package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 小程序单棚详情中的已上传照片。 */
@Data
public class SfInspectionPhotoArchiveMiniappPhotoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 归档照片关联主键。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long photoId;

    /** OSS 对象主键。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ossId;

    /** 上传原始文件名。 */
    private String originalName;

    /** 当前可访问的图片地址。 */
    private String url;

    /** 归档关联创建时间。 */
    private Date createTime;
}
