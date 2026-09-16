package com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 小程序创建巡查归档日期入参。 */
@Data
public class SfInspectionPhotoArchiveMiniappCreateBo {

    /** 巡查归档日期，格式 yyyy-MM-dd，不能晚于当天。 */
    @NotNull(message = "归档日期不能为空")
    @PastOrPresent(message = "归档日期不能晚于当前日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate archiveDate;
}
