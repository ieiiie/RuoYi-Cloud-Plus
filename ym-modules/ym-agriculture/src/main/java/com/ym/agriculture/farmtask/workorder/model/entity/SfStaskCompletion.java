package com.ym.agriculture.farmtask.workorder.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * stask 完工记录，表 {@code sf_stask_completion}。
 */
@Data
@TableName("sf_stask_completion")
public class SfStaskCompletion {

    /**
     * 完工记录主键。
     */
    @TableId("completion_id")
    private Long completionId;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 工单ID。
     */
    private Long orderId;

    /**
     * 组长员工ID。
     */
    private Long leaderId;

    /**
     * 作业照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String workPhotos;

    /**
     * 完工备注。
     */
    private String completionRemark;

    /**
     * 完工提交时间。
     */
    private Date completedAt;

    /**
     * 创建时间。
     */
    private Date createTime;
}
