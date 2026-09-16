package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 巡查归档大棚同步执行结果。
 *
 * @author ym-cloud
 */
@Data
public class SfInspectionPhotoArchiveSyncResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 本次新建的归档大棚快照数量。 */
    private Integer addedCount;

    /** 本次删除的无照片归档大棚快照数量。 */
    private Integer removedCount;
}
