package com.ym.agriculture.farming.satellite.model.constants;

import java.time.LocalDate;

/**
 * 遥感周期计划 {@code sf_satellite_schedule.schedule_status} 状态常量。
 * <p>
 * 非停用状态下由后端根据 detectStart / detectEnd 与当前日期动态计算。
 *
 * @author ym-cloud
 */
public final class SatelliteScheduleStatus {

    /** 未开始（当前日期 < detectStart） */
    public static final int NOT_STARTED = 0;

    /** 周期中（detectStart <= 当前日期 <= detectEnd） */
    public static final int IN_PROGRESS = 1;

    /** 已完成（当前日期 > detectEnd 或 nextRunDate > detectEnd） */
    public static final int COMPLETED = 2;

    /** 已停用（手动暂停） */
    public static final int PAUSED = 3;

    private SatelliteScheduleStatus() {
    }

    /**
     * 根据当前日期和检测窗口计算运行时状态（排除手动停用场景）。
     *
     * @param detectStart 检测开始日期
     * @param detectEnd   检测结束日期
     * @param today       当前日期
     * @return 计算出的状态值
     */
    public static int computeStatus(LocalDate detectStart, LocalDate detectEnd, LocalDate today) {
        if (today.isBefore(detectStart)) {
            return NOT_STARTED;
        }
        if (today.isAfter(detectEnd)) {
            return COMPLETED;
        }
        return IN_PROGRESS;
    }

    public static String describe(int status) {
        return switch (status) {
            case NOT_STARTED -> "未开始";
            case IN_PROGRESS -> "周期中";
            case COMPLETED -> "已完成";
            case PAUSED -> "已停用";
            default -> "未知";
        };
    }
}
