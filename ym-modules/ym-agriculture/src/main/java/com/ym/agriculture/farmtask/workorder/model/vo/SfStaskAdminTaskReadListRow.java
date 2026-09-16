package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

import java.util.Date;

/** 后台具体任务分页数据库投影。 */
@Data
public class SfStaskAdminTaskReadListRow {
    private Long orderId;
    private String orderNo;
    private Long packageId;
    private String packageNo;
    private Long creatorEmployeeId;
    private String creatorRoleCode;
    private Date planDate;
    private String status;
    private Long greenhouseId;
    private String greenhouseNameSnapshot;
    private Long workItemId;
    private String workItemNameSnapshot;
    private Long leaderId;
    private Double requiredWorkerCount;
    private Integer acceptedWorkerCount;
    private Date createTime;
}
