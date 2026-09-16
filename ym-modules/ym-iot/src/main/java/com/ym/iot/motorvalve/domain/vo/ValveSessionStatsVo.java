package com.ym.iot.motorvalve.domain.vo;

import lombok.Data;

/**
 * 电动阀开阀会话汇总统计。
 */
@Data
public class ValveSessionStatsVo {

    /** 会话总数（已关闭 + 异常终止；不含仍开启中） */
    private Long totalSessions;

    /** 累计持续秒数（已关闭/异常终止会话） */
    private Long totalDurationSeconds;

    /** 累计持续分钟数（展示用，向下取整） */
    private Long totalDurationMinutes;

    /** 平均持续秒数（已关闭会话） */
    private Long avgDurationSeconds;

    /** 当前仍开启中的会话数 */
    private Long openSessionCount;
}
