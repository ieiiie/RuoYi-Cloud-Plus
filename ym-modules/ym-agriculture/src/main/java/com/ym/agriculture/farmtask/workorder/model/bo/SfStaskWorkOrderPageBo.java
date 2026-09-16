package com.ym.agriculture.farmtask.workorder.model.bo;

import lombok.Data;

import java.util.Date;

/**
 * stask 工单分页查询入参。
 */
@Data
public class SfStaskWorkOrderPageBo {

    /**
     * 工单状态。
     */
    private String status;

    /**
     * 大棚ID。
     */
    private Long greenhouseId;

    /**
     * 农事项目ID。
     */
    private Long workItemId;

    /**
     * 组长员工ID。
     */
    private Long leaderId;

    /**
     * 计划开始日期。
     */
    private Date planStartDate;

    /**
     * 计划结束日期。
     */
    private Date planEndDate;
}
