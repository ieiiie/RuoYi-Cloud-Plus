package com.ym.agriculture.farmtask.workorder.model.bo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * stask 工人全部任务列表查询入参。
 */
@Data
public class SfStaskWorkerAllTaskQueryBo {

    /**
     * 工人任务状态集合：PENDING_CONFIRM-待确认，ACCEPTED-已接收，
     * REJECTED-已拒绝，COMPLETED-已完成（验收通过），NOT_PASSED-验收不通过，VOIDED-任务作废。
     */
    private List<String> statuses;

    /**
     * 大棚 ID 集合；为空表示全部大棚。
     */
    private List<Long> greenhouseIds;

    /**
     * 农事项目 ID 集合；为空表示全部农事项目。
     */
    private List<Long> workItemIds;

    /**
     * 计划作业开始日期，格式：yyyy-MM-dd。
     */
    private Date planStartDate;

    /**
     * 计划作业结束日期，格式：yyyy-MM-dd。
     */
    private Date planEndDate;
}
