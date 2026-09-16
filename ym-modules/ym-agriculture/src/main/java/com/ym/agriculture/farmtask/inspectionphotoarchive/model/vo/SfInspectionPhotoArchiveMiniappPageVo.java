package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 小程序巡棚拍照列表页聚合数据。 */
@Data
public class SfInspectionPhotoArchiveMiniappPageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前租户全部已创建归档日期，按日期倒序。 */
    private List<SfInspectionPhotoArchiveMiniappDateVo> dates;

    /** 当前选中的归档主键；尚无日期时为空。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long selectedArchiveId;

    /** 当前日期汇总；尚无日期时为空。 */
    private SfInspectionPhotoArchiveMiniappSummaryVo summary;

    /** 按当前大屏布局分组的大棚；尚无日期时为空列表。 */
    private List<SfInspectionPhotoArchiveMiniappGroupVo> groups;
}
