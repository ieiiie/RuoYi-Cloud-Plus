package com.ym.agriculture.farmtask.workorder.support;

import cn.hutool.core.collection.CollUtil;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchItemVo;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * stask 工作台条目排序器。
 */
public final class SfStaskWorkbenchItemSorter {

    private SfStaskWorkbenchItemSorter() {
    }

    /**
     * 按最新业务时间排序管理员工作台条目。
     *
     * @param items 管理员工作台条目
     */
    public static void sortManagerItems(List<SfStaskManagerWorkbenchItemVo> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.sort(Comparator
            .comparing((SfStaskManagerWorkbenchItemVo item) -> sortTime(
                    item.getEventTime(), item.getAcceptedAt(), item.getCompletedAt(), item.getPlanDate()),
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(SfStaskManagerWorkbenchItemVo::getPlanDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }

    /**
     * 按最新业务时间排序技术员工作台条目。
     *
     * @param items 技术员工作台条目
     */
    public static void sortTechnicianItems(List<SfStaskTechnicianWorkbenchItemVo> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.sort(Comparator
            .comparing((SfStaskTechnicianWorkbenchItemVo item) -> sortTime(
                    item.getEventTime(), item.getAcceptedAt(), item.getCompletedAt(), item.getPlanDate()),
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(SfStaskTechnicianWorkbenchItemVo::getPlanDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }

    /**
     * 按最新业务时间排序组长工作台条目。
     *
     * @param items 组长工作台条目
     */
    public static void sortLeaderItems(List<SfStaskLeaderWorkbenchItemVo> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.sort(Comparator
            .comparing((SfStaskLeaderWorkbenchItemVo item) -> sortTime(
                    item.getEventTime(), item.getAcceptedAt(), item.getCompletedAt(), item.getPlanDate()),
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(SfStaskLeaderWorkbenchItemVo::getPlanDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }

    private static Date sortTime(Date eventTime, Date acceptedAt, Date completedAt, Date fallbackTime) {
        if (eventTime != null) {
            return eventTime;
        }
        if (acceptedAt != null) {
            return acceptedAt;
        }
        return completedAt != null ? completedAt : fallbackTime;
    }
}
