package com.ym.agriculture.farmtask.workorder.support.workbench;

import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

/**
 * 工作台统一时间边界与事件时间解析。
 */
public final class SfStaskWorkbenchTimeSupport {

    /** 中国业务日期使用的固定时区。 */
    public static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private SfStaskWorkbenchTimeSupport() {
    }

    /**
     * 返回中国时区今天的左闭右开时间范围。
     *
     * @return 起止时间数组，索引 0 为开始、索引 1 为次日开始
     */
    public static Date[] todayRange() {
        LocalDate today = LocalDate.now(CHINA_ZONE);
        return new Date[] {
            Date.from(today.atStartOfDay(CHINA_ZONE).toInstant()),
            Date.from(today.plusDays(1).atStartOfDay(CHINA_ZONE).toInstant())
        };
    }

    /**
     * 从预加载索引读取指定工单的最新事件。
     */
    public static SfStaskFlowLog latestFlowLog(Map<String, SfStaskFlowLog> index, Long orderId, String event) {
        return orderId == null || event == null ? null : index.get(orderId + ":" + event);
    }

    /**
     * 解析任务包最近提交时间，保持管理员提交优先的兼容规则。
     */
    public static Date submitTime(Map<String, SfStaskFlowLog> flowLogIndex, Long orderId) {
        SfStaskFlowLog managerSubmit = latestFlowLog(flowLogIndex, orderId, StaskOrderEvent.SUBMIT_BY_MANAGER);
        if (managerSubmit != null) {
            return managerSubmit.getCreateTime();
        }
        SfStaskFlowLog technicianSubmit = latestFlowLog(
            flowLogIndex, orderId, StaskOrderEvent.SUBMIT_BY_TECHNICIAN);
        return technicianSubmit == null ? null : technicianSubmit.getCreateTime();
    }

    /**
     * 计算自发出时间起已经过的完整小时数。
     */
    public static Long elapsedHours(Date issuedAt) {
        if (issuedAt == null) {
            return null;
        }
        return Math.max(0L, Duration.between(issuedAt.toInstant(), Instant.now()).toHours());
    }

    /**
     * 判断计划日期是否为中国时区的今天。
     */
    public static boolean isToday(Date planDate) {
        return planDate != null
            && planDate.toInstant().atZone(CHINA_ZONE).toLocalDate().equals(LocalDate.now(CHINA_ZONE));
    }
}
