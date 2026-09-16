package com.ym.agriculture.farming.weather.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 实况天气缓存时段边界：默认每日 0/6/12/18 点划分，{@code pull_time >= boundary} 视为当前时段有效。
 */
public final class LiveCacheBoundarySupport {

    private LiveCacheBoundarySupport() {
    }

    /**
     * 计算 {@code now} 所属时段的起点（不大于 {@code now} 的最近刷新时刻）。
     *
     * @param now          当前时间
     * @param refreshHours 日刷新小时列表，0 表示 00:00/24:00，取值 0~23
     */
    public static Date resolveBoundary(Date now, List<Integer> refreshHours) {
        List<Integer> hours = normalizeHours(refreshHours);
        Date beginOfDay = DateUtil.beginOfDay(now);
        Date boundary = null;
        for (int hour : hours) {
            Date candidate = DateUtil.offsetHour(beginOfDay, hour);
            if (!candidate.after(now) && (boundary == null || candidate.after(boundary))) {
                boundary = candidate;
            }
        }
        if (boundary != null) {
            return boundary;
        }
        int lastHour = hours.get(hours.size() - 1);
        return DateUtil.offsetHour(DateUtil.offsetDay(beginOfDay, -1), lastHour);
    }

    private static List<Integer> normalizeHours(List<Integer> refreshHours) {
        List<Integer> source = CollUtil.isEmpty(refreshHours)
            ? List.of(0, 6, 12, 18)
            : refreshHours;
        List<Integer> normalized = new ArrayList<>(source.size());
        for (Integer hour : source) {
            if (hour == null) {
                continue;
            }
            int h = hour == 24 ? 0 : hour;
            if (h >= 0 && h <= 23 && !normalized.contains(h)) {
                normalized.add(h);
            }
        }
        if (normalized.isEmpty()) {
            normalized.addAll(List.of(0, 6, 12, 18));
        }
        normalized.sort(Comparator.naturalOrder());
        return normalized;
    }
}
