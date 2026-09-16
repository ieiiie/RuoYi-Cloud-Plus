package com.ym.agriculture.farmtask.workorder.model.bo;

import lombok.Data;

import java.util.Date;

/**
 * 后台只读任务包查询条件。
 */
@Data
public class SfStaskAdminPackageQueryBo {

    private String keyword;

    private Date planStartDate;

    private Date planEndDate;

    private String status;

    private Long creatorEmployeeId;

    private String creatorRoleCode;
}
