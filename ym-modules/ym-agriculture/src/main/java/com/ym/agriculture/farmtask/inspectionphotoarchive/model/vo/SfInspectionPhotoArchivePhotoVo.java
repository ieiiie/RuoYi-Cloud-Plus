package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 巡查归档照片展示数据。
 *
 * @author ym-cloud
 */
@Data
public class SfInspectionPhotoArchivePhotoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 归档照片关联主键。 */
    private Long photoId;

    /** 系统 OSS 对象主键。 */
    private Long ossId;

    /** 上传原始文件名。 */
    private String originalName;

    /** 同一大棚内的展示顺序。 */
    private Integer seq;

    /** 当前 OSS 访问地址；原对象失效或被运维删除时为空。 */
    private String url;

    /** 归档关联创建时间。 */
    private Date createTime;
}
