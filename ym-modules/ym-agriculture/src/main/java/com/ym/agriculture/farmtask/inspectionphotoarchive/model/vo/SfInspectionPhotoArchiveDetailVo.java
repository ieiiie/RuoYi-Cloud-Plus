package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 巡查归档详情。
 *
 * @author ym-cloud
 */
@Data
public class SfInspectionPhotoArchiveDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 归档主键。 */
    private Long archiveId;

    /** 巡查归档日期，格式 yyyy-MM-dd。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate archiveDate;

    /** 当日全部归档大棚快照，按归档时排序返回。 */
    private List<SfInspectionPhotoArchiveFieldVo> fields;
}
