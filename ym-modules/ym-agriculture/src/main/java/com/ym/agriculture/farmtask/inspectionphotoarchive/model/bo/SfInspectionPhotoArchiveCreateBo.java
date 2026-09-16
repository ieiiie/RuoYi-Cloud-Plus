package com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 创建巡查照片归档日期入参。
 *
 * @author ym-cloud
 */
@Data
public class SfInspectionPhotoArchiveCreateBo {

    /** 巡查归档日期，格式 yyyy-MM-dd，不允许晚于当前日期。 */
    @NotNull(message = "归档日期不能为空")
    @PastOrPresent(message = "归档日期不能晚于当前日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate archiveDate;
}
