package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 工人首页任务聚合视图对象。
 */
@Data
public class SfStaskWorkerHomeVo {

    /**
     * 待接受邀请数量。
     */
    private Long pendingInviteCount;

    /**
     * 已接受且未完成验收的任务数量。
     */
    private Long acceptedCount;

    /**
     * 兼容字段，同 pendingInviteCount。
     */
    private Long pendingReviewCount;

    /**
     * 兼容字段，同 acceptedCount。
     */
    private Long processingCount;

    /**
     * 待接受任务卡片列表。
     */
    private List<SfStaskWorkerTaskItemVo> pendingInviteTasks;

    /**
     * 已接受任务卡片列表。
     */
    private List<SfStaskWorkerTaskItemVo> acceptedTasks;
}
