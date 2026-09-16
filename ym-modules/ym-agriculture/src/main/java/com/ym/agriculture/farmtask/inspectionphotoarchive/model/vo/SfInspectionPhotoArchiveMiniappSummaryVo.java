package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 小程序归档日期下的大棚上传汇总。 */
@Data
public class SfInspectionPhotoArchiveMiniappSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前日期全部归档大棚数。 */
    private Integer totalFieldCount;

    /** 尚无成功照片的大棚数。 */
    private Integer pendingFieldCount;

    /** 至少已有一张成功照片的大棚数。 */
    private Integer uploadedFieldCount;
}
