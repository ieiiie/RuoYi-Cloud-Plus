package com.ym.agriculture.farmtask.leaderlabor.model.vo;

import lombok.Data;

import java.time.LocalDate;

/** 可批量操作的计划日期选项。 */
@Data
public class SfStaskLaborDateOptionVo {
    /** 任务计划日期。 */
    private LocalDate planDate;
    /** 当日可操作任务数。 */
    private Long taskCount;
}
