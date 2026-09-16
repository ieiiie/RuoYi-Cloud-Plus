package com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 巡查归档照片与 OSS 对象的关联，表 {@code sf_inspection_photo_archive_photo}。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_inspection_photo_archive_photo")
public class SfInspectionPhotoArchivePhoto extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 归档照片关联主键。 */
    @TableId(value = "photo_id", type = IdType.ASSIGN_ID)
    private Long photoId;

    /** 所属归档大棚快照主键。 */
    private Long archiveFieldId;

    /** 系统 OSS 对象主键 {@code sys_oss.oss_id}。 */
    private Long ossId;

    /** 上传时的原始文件名，用于页面展示。 */
    private String originalName;

    /** 小程序单张上传幂等标识；管理端上传为空。 */
    private String clientUploadId;

    /** 同一归档大棚中的展示顺序，从 0 开始递增。 */
    private Integer seq;
}
