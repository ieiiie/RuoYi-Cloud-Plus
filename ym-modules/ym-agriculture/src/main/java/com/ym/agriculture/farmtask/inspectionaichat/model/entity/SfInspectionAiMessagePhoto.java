package com.ym.agriculture.farmtask.inspectionaichat.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

/** 用户消息的巡查照片快照。 */
@Data
@TableName("sf_inspection_ai_message_photo")
public class SfInspectionAiMessagePhoto {
    /** 附件主键。 */
    @TableId(value = "message_photo_id", type = IdType.ASSIGN_ID)
    private Long messagePhotoId;
    /** 租户编号。 */
    private String tenantId;
    /** 用户消息主键。 */
    private Long messageId;
    /** 归档照片主键。 */
    private Long photoId;
    /** OSS 主键。 */
    private Long ossId;
    /** 原始文件名。 */
    private String originalName;
    /** 归档主键。 */
    private Long archiveId;
    /** 大棚快照主键。 */
    private Long archiveFieldId;
    /** 归档日期快照。 */
    private LocalDate archiveDateSnapshot;
    /** 大棚编号快照。 */
    private String greenhouseCodeSnapshot;
    /** 大棚名称快照。 */
    private String greenhouseNameSnapshot;
    /** 发送顺序。 */
    private Integer sortOrder;
    /** 创建时间。 */
    private Date createTime;
}
