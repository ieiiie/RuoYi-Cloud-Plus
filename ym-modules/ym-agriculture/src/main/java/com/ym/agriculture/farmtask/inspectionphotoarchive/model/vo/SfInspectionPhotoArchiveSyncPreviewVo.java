package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 巡查归档与当前启用大棚的同步预览。
 *
 * @author ym-cloud
 */
@Data
public class SfInspectionPhotoArchiveSyncPreviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前启用但归档缺失、可新增的大棚数量。 */
    private Integer addCount;

    /** 归档存在但当前已不启用且没有照片、可删除的大棚数量。 */
    private Integer removeCount;

    /** 同步状态：NONE、ADD、REMOVE 或 BOTH。 */
    private String mode;
}
