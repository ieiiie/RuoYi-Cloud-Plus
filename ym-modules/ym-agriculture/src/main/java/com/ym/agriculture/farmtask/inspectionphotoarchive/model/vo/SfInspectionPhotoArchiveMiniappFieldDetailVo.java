package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/** 小程序单棚拍照页聚合数据。 */
@Data
public class SfInspectionPhotoArchiveMiniappFieldDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前归档主键。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long archiveId;

    /** 当前归档日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate archiveDate;

    /** 当前归档大棚快照主键。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long archiveFieldId;

    /** 来源大棚主键。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fieldId;

    /** 大棚编号。 */
    private String fieldCode;

    /** 大棚名称。 */
    private String fieldName;

    /** 当前日期下的成功照片。 */
    private List<SfInspectionPhotoArchiveMiniappPhotoVo> photos;

    /** 可切换日期：当前日期与该棚存在成功照片的历史日期。 */
    private List<SfInspectionPhotoArchiveMiniappDateVo> selectableDates;
}
