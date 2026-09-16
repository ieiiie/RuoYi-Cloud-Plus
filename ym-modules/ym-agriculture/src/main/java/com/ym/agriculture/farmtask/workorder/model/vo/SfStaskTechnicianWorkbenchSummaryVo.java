package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

/**
 * 技术员工作台统计视图（三栏计数）。
 */
@Data
public class SfStaskTechnicianWorkbenchSummaryVo {

    /**
     * 待处理总数（本人草稿/待技术确认任务包 + 生产管理员待审任务包）。
     */
    private Long pendingCount;

    /**
     * 进行中总数（个人相关拆分工单）。
     */
    private Long processingCount;

    /**
     * 今日已完成总数（个人相关、当日已验收拆单）。
     */
    private Long completedTodayCount;

    /**
     * 兼容字段，与 pendingCount 一致。
     */
    private Long pendingReviewCount;

    /**
     * 生产管理员待审任务包数量（兼容字段）。
     */
    private Long pendingProductionReviewCount;

    /**
     * 本人经手的待验收拆分工单数量。
     */
    private Long pendingAcceptanceCount;
}
