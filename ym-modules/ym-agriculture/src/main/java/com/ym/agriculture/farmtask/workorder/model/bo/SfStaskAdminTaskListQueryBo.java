package com.ym.agriculture.farmtask.workorder.model.bo;

import lombok.Data;

import java.util.Date;

/**
 * 平台后台农事任务列表查询入参。
 */
@Data
public class SfStaskAdminTaskListQueryBo {

    /**
     * 农事项目 ID。
     */
    private Long workItemId;

    /**
     * 计划日期开始，包含边界。
     */
    private Date planStartDate;

    /**
     * 计划日期结束，包含边界。
     */
    private Date planEndDate;

    /**
     * 任务包或拆分工单状态。
     */
    private String status;

    /**
     * 大棚 ID。
     */
    private Long greenhouseId;

    /**
     * 创建人员工 ID。
     */
    private Long creatorEmployeeId;
}
