package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

/**
 * 生产管理员工作台统计视图（仅三栏计数）。
 */
@Data
public class SfStaskManagerWorkbenchSummaryVo {

    /**
     * 待处理总数（任务包 + 待验收拆单）。
     */
    private Long pendingCount;

    /**
     * 进行中总数（待技术确认包 + 进行中拆单）。
     */
    private Long processingCount;

    /**
     * 今日已完成总数（当日验收通过/不通过拆单）。
     */
    private Long completedTodayCount;

    /**
     * 兼容字段，与 pendingCount 一致。
     */
    private Long pendingReviewCount;

    /**
     * 待验收拆单数量（兼容字段）。
     */
    private Long pendingAcceptanceCount;
}
