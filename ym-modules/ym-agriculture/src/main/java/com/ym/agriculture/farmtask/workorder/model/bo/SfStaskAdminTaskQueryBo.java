package com.ym.agriculture.farmtask.workorder.model.bo;

import lombok.Data;

import java.util.Date;

/**
 * 后台只读具体任务查询条件。
 */
@Data
public class SfStaskAdminTaskQueryBo {

    private String keyword;

    private Long packageId;

    private Date planStartDate;

    private Date planEndDate;

    private String status;

    private Long greenhouseId;

    private Long workItemId;

    private Long leaderId;
}
