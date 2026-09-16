package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

import java.util.List;

/**
 * stask 小程序工作台视图对象。
 */
@Data
public class SfStaskWorkbenchVo {

    /**
     * 待审核数量。
     */
    private Long pendingReviewCount;

    /**
     * 待验收数量。
     */
    private Long pendingAcceptanceCount;

    /**
     * 进行中数量。
     */
    private Long processingCount;

    /**
     * 待处理数量（生产管理员：草稿/技术退回任务包数）。
     */
    private Long pendingCount;

    /**
     * 今日已完成数量（生产管理员：当日已验收的拆分工单，含通过与不通过）。
     */
    private Long completedTodayCount;

    /**
     * 待处理任务包卡片列表（生产管理员主页：草稿、技术退回）。
     */
    private List<SfStaskHomeTaskCardVo> pendingTasks;

    /**
     * 待处理待验收拆分工单列表（生产管理员主页待处理区）。
     */
    private List<SfStaskWorkOrderVo> pendingAcceptanceTasks;

    /**
     * 进行中待技术员确认任务包列表（生产管理员主页）。
     */
    private List<SfStaskHomeTaskCardVo> processingPackages;

    /**
     * 进行中拆分工单列表（生产管理员主页，不含待验收）。
     */
    private List<SfStaskWorkOrderVo> processingTasks;

    /**
     * 今日已完成拆分工单列表（生产管理员主页，含验收通过与不通过）。
     */
    private List<SfStaskWorkOrderVo> completedTodayTasks;

    /**
     * 任务列表（查看全部 / 兼容字段，生产管理员可与三列表一致或合并）。
     */
    private List<SfStaskWorkOrderVo> rows;
}
