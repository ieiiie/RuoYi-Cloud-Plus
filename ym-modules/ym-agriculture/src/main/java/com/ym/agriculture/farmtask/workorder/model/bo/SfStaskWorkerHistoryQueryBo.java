package com.ym.agriculture.farmtask.workorder.model.bo;

import lombok.Data;

import java.util.Date;

/**
 * 工人历史任务分页查询入参。
 */
@Data
public class SfStaskWorkerHistoryQueryBo {

    /**
     * 计划开始日期，格式：yyyy-MM-dd。
     */
    private Date planStartDate;

    /**
     * 计划结束日期，格式：yyyy-MM-dd。
     */
    private Date planEndDate;

    /**
     * 农事项目 ID，可选筛选条件。
     */
    private Long workItemId;
}
