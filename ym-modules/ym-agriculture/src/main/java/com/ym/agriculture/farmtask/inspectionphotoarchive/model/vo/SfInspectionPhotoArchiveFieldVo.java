package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 巡查归档大棚卡片数据。
 *
 * @author ym-cloud
 */
@Data
public class SfInspectionPhotoArchiveFieldVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 归档大棚快照主键。 */
    private Long archiveFieldId;

    /** 来源大棚主键。 */
    private Long fieldId;

    /** 归档时的大棚编号。 */
    private String fieldCode;

    /** 归档时的大棚名称。 */
    private String fieldName;

    /** 归档时的大棚排序值。 */
    private Integer sortOrder;

    /** 归档时的种植批次主键；未种植时为空。 */
    private Long plantingBatchId;

    /** 归档时的作物物种主键；未种植时为空。 */
    private Long speciesId;

    /** 归档时的作物物种名称；未种植时为空。 */
    private String speciesName;

    /** 归档时的作物品种主键；未种植时为空。 */
    private Long varietyId;

    /** 归档时的作物品种名称；未种植时为空。 */
    private String varietyName;

    /** 页面展示用作物名称，格式为物种 / 品种；未种植时为“未种植”。 */
    private String cropDisplayName;

    /** 当前已关联的照片数量。 */
    private Integer photoCount;

    /** 当前已关联的照片列表，按上传顺序排列。 */
    private List<SfInspectionPhotoArchivePhotoVo> photos;
}
