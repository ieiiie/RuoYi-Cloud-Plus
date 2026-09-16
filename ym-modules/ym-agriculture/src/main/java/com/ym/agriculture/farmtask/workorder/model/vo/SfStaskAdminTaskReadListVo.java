package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 后台具体任务只读列表项。
 */
@Data
public class SfStaskAdminTaskReadListVo {

    private Long orderId;

    private String orderNo;

    private Long packageId;

    private String packageNo;

    private Long workItemId;

    private String workItemDisplay;

    private Long greenhouseId;

    private String greenhouseDisplay;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date planDate;

    private String status;

    private String statusLabel;

    private Long leaderId;

    private String leaderName;

    private Double workerCount;

    private Integer acceptedWorkerCount;

    private Long creatorEmployeeId;

    private String creatorEmployeeName;

    private String creatorRoleCode;

    private String creatorRoleName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
}
