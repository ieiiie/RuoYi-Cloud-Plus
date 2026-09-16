package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/** 小程序巡查归档日期选项。 */
@Data
public class SfInspectionPhotoArchiveMiniappDateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 归档主键。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long archiveId;

    /** 归档日期，格式 yyyy-MM-dd。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate archiveDate;
}
