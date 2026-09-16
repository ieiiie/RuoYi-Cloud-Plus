package com.ym.agriculture.farmtask.leaderlabor.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/** 日用工记录列表行。 */
@Data
public class SfStaskLaborRecordVo {
    /** 日用工记录ID。 */
    private Long laborRecordId;
    /** 组长员工ID。 */
    private Long leaderEmployeeId;
    /** 组长姓名。 */
    private String leaderEmployeeName;
    /** 计划日期。 */
    private LocalDate planDate;
    /** 日用工人数。 */
    private BigDecimal dailyLaborCount;
    /** 已接单后的有效任务数。 */
    private Long taskCount;
    /** 当前用户是否可编辑。 */
    private Boolean editable;
    /** 只读原因。 */
    private String readonlyReason;
    /** 乐观锁版本。 */
    private Long version;
    /** 最后更新时间。 */
    private Date updateTime;
}
