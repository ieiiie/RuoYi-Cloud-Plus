package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 后台任务包分页数据库投影。
 */
@Data
public class SfStaskAdminPackageListRow {

    private Long packageId;

    private String packageNo;

    private Long creatorEmployeeId;

    private String creatorRoleCode;

    private Date planDate;

    private String status;

    private Long taskCount;

    private Date createTime;
}
