package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

/**
 * 组长工作台统计视图（三栏计数）。
 */
@Data
public class SfStaskLeaderWorkbenchSummaryVo {

    /**
     * 待接单数量。
     */
    private Long pendingAcceptCount;

    /**
     * 已废弃待派工数量，兼容返回固定值 0。
     */
    private Long pendingAssignCount;

    /**
     * 进行中数量（派工完成、已到达、待验收）。
     */
    private Long inProgressCount;

    /**
     * 今日验收通过数量。
     */
    private Long completedTodayCount;

    /**
     * 兼容字段，与 pendingAcceptCount 一致。
     */
    private Long pendingReviewCount;

    /**
     * 兼容字段，与 inProgressCount 一致。
     */
    private Long processingCount;
}
