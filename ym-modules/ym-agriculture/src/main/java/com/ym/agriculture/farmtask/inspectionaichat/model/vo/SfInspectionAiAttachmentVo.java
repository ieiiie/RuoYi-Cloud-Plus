package com.ym.agriculture.farmtask.inspectionaichat.model.vo;

import lombok.Data;

import java.time.LocalDate;

/** AI 历史消息附件。 */
@Data
public class SfInspectionAiAttachmentVo {
    /** 归档照片主键。 */
    private Long photoId;
    /** 原始文件名。 */
    private String originalName;
    /** 归档日期快照。 */
    private LocalDate archiveDate;
    /** 大棚编号快照。 */
    private String greenhouseCode;
    /** 大棚名称快照。 */
    private String greenhouseName;
    /** 当前原图是否可用。 */
    private boolean available;
    /** 当前预览地址；失效时为空。 */
    private String url;
}
