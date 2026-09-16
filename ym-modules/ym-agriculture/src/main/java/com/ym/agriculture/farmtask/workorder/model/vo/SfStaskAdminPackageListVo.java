package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 后台任务包只读列表项。
 */
@Data
public class SfStaskAdminPackageListVo {

    private Long packageId;

    private String packageNo;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date planDate;

    private String status;

    private String statusLabel;

    private Long taskCount;

    private String workItemDisplay;

    private String greenhouseDisplay;

    private Long creatorEmployeeId;

    private String creatorEmployeeName;

    private String creatorRoleCode;

    private String creatorRoleName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
}
