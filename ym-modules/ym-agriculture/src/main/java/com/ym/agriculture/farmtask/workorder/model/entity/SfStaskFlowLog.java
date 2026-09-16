package com.ym.agriculture.farmtask.workorder.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * stask 工单流转日志，表 {@code sf_stask_flow_log}。
 */
@Data
@TableName("sf_stask_flow_log")
public class SfStaskFlowLog {

    /**
     * 流转日志主键。
     */
    @TableId("log_id")
    private Long logId;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 工单ID。
     */
    private Long orderId;

    /**
     * 任务包ID。
     */
    private Long packageId;

    /**
     * 流转前状态。
     */
    private String fromStatus;

    /**
     * 流转后状态。
     */
    private String toStatus;

    /**
     * 流转事件。
     */
    private String event;

    /**
     * 操作人员工ID。
     */
    private Long operatorEmployeeId;

    /**
     * 操作人角色编码。
     */
    private String operatorRoleCode;

    /**
     * 流转说明。
     */
    private String remark;

    /**
     * 创建时间。
     */
    private Date createTime;
}
