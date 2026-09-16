package com.ym.agriculture.farming.batch.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 种植批次状态聚合（仪表盘「地块状态概览」），与列表/日历相同的数据权限范围。
 *
 * @author ym-cloud
 */
@Data
public class SfPlantingBatchDashboardStatsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 计划中（PLANNING）
     */
    private long planningCount;

    /**
     * 种植中（PLANTING）
     */
    private long plantingCount;

    /**
     * 生长期（GROWING）
     */
    private long growingCount;

    /**
     * 采收中（HARVESTING）
     */
    private long harvestingCount;

    /**
     * 已结束（FINISHED）
     */
    private long finishedCount;

    /**
     * 失败/异常（FAILED）
     */
    private long failedCount;
}
