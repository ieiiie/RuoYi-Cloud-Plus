package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 平台后台农事任务统一列表项。
 */
@Data
public class SfStaskAdminTaskListVo {

    /**
     * 当前列表行的稳定标识；任务包为任务包 ID，拆分任务为工单 ID。
     */
    private Long recordId;

    /**
     * 任务类型：PACKAGE-任务包，SPLIT-拆分工单。
     */
    private String taskType;

    /**
     * 任务类型中文文案。
     */
    private String taskTypeLabel;

    /**
     * 任务包 ID，两类任务均有值。
     */
    private Long packageId;

    /**
     * 任务包编号。
     */
    private String packageNo;

    /**
     * 拆分工单 ID，仅拆分任务有值。
     */
    private Long orderId;

    /**
     * 拆分工单编号，仅拆分任务有值。
     */
    private String orderNo;

    /**
     * 农事项 ID，仅拆分任务有值。
     */
    private Long workItemId;

    /**
     * 农事项展示文本；任务包为首项/总项数摘要。
     */
    private String workItemDisplay;

    /**
     * 大棚 ID，仅拆分任务有值。
     */
    private Long greenhouseId;

    /**
     * 大棚展示文本；任务包为去重大棚数摘要。
     */
    private String greenhouseDisplay;

    /**
     * 计划作业日期。
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date planDate;

    /**
     * 任务状态编码。
     */
    private String status;

    /**
     * 任务状态中文文案。
     */
    private String statusLabel;

    /**
     * 组长员工 ID，仅拆分任务有值。
     */
    private Long leaderId;

    /**
     * 组长姓名，仅拆分任务有值。
     */
    private String leaderName;

    /**
     * 组长接单填写的工人数量，不含组长；未接单时为空。
     */
    private Double workerCount;

    /**
     * 创建人员工 ID。
     */
    private Long creatorEmployeeId;

    /**
     * 创建人姓名。
     */
    private String creatorEmployeeName;

    /**
     * 创建时 stask 应用角色编码。
     */
    private String creatorRoleCode;

    /**
     * 创建时 stask 应用角色名称。
     */
    private String creatorRoleName;

    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
}
