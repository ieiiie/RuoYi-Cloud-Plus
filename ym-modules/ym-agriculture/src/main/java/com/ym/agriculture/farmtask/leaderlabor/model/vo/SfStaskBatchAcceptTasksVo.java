package com.ym.agriculture.farmtask.leaderlabor.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 指定计划日的批量接单任务与日用工回显。 */
@Data
public class SfStaskBatchAcceptTasksVo {
    /** 计划日期。 */
    private LocalDate planDate;
    /** 日用工记录ID，无记录时为空。 */
    private Long laborRecordId;
    /** 已有日用工人数，无记录时为空。 */
    private BigDecimal dailyLaborCount;
    /** 已有记录版本，无记录时为空。 */
    private Long laborRecordVersion;
    /** 待接单任务。 */
    private List<SfStaskBatchTaskVo> rows;
}
