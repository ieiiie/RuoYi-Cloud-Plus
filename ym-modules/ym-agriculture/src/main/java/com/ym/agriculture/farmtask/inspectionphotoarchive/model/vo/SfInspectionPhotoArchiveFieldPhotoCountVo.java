package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import lombok.Data;

/**
 * 巡查归档照片按大棚聚合计数。
 */
@Data
public class SfInspectionPhotoArchiveFieldPhotoCountVo {

    /** 归档大棚快照主键。 */
    private Long archiveFieldId;

    /** 来源大棚主键。 */
    private Long fieldId;

    /** 成功关联的照片数。 */
    private Long photoCount;
}
