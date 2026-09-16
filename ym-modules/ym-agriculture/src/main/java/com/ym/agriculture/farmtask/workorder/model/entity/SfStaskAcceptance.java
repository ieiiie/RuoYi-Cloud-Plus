package com.ym.agriculture.farmtask.workorder.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * stask 验收记录，表 {@code sf_stask_acceptance}。
 */
@Data
@TableName("sf_stask_acceptance")
public class SfStaskAcceptance {

    /**
     * 验收记录主键。
     */
    @TableId("acceptance_id")
    private Long acceptanceId;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 工单ID。
     */
    private Long orderId;

    /**
     * 验收人员工ID。
     */
    private Long acceptorEmployeeId;

    /**
     * 验收人角色编码。
     */
    private String acceptorRoleCode;

    /**
     * 验收结果：PASS/REJECT。
     */
    private String result;

    /**
     * 不合格原因。
     */
    private String rejectReason;

    /**
     * 验收照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String acceptancePhotos;

    /**
     * 验收时间。
     */
    private Date acceptedAt;

    /**
     * 创建时间。
     */
    private Date createTime;
}
