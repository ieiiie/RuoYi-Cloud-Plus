package com.ym.iot.fertilizer.enums;

/**
 * 设备风险等级。
 *
 * <p>由 status1~2 的报警位推导。关键报警（如泵故障、液位低报）→ CRITICAL；
 * 非关键报警（如流量高报、液位断线）→ WARNING；无报警 → NORMAL。
 *
 * <p>CRITICAL 时禁止启动。WARNING 时允许操作但前端展示黄色提示。
 *
 * @author ym-cloud
 */
public enum FertilizerRiskLevel {

    /** 无任何报警，正常作业 */
    NORMAL,

    /** 存在非关键报警，需关注但不阻断操作 */
    WARNING,

    /** 存在关键报警或故障停机事件，禁止启动，需人工排除 */
    CRITICAL
}
