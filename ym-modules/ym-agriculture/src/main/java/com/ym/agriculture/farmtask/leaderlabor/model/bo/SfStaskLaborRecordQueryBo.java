package com.ym.agriculture.farmtask.leaderlabor.model.bo;

import lombok.Data;

import java.time.LocalDate;

/** 日用工记录分页筛选条件。 */
@Data
public class SfStaskLaborRecordQueryBo {
    /** 计划日期开始。 */
    private LocalDate planDateStart;
    /** 计划日期结束。 */
    private LocalDate planDateEnd;
    /** 组长员工ID，仅只读角色可筛选。 */
    private Long leaderEmployeeId;
}
