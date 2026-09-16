package com.ym.agriculture.farming.solarterms.support;

import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 lunar-java 计算节气交节时间（固定按北京时间解释）。
 */
public final class SolarTermCalculator {

    public static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    private SolarTermCalculator() {
    }

    /**
     * 计算指定公历年的 24 节气（交节时刻落在该公历年的记录）。
     */
    public static List<TermInstant> calculateYear(int year) {
        Map<String, Solar> table = loadJieQiTableAround(year);
        List<TermInstant> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : SolarTermCodes.codeToName().entrySet()) {
            String termName = entry.getValue();
            Solar solar = table.get(termName);
            if (solar == null) {
                continue;
            }
            LocalDateTime occurredAt = LocalDateTime.of(
                solar.getYear(), solar.getMonth(), solar.getDay(),
                solar.getHour(), solar.getMinute(), solar.getSecond());
            if (occurredAt.getYear() != year) {
                continue;
            }
            Lunar termLunar = solar.getLunar();
            String lunarText = termLunar.getYearInChinese() + "年"
                + termLunar.getMonthInChinese() + "月"
                + termLunar.getDayInChinese();
            list.add(new TermInstant(
                entry.getKey(),
                termName,
                occurredAt,
                lunarText
            ));
        }
        list.sort(Comparator.comparing(TermInstant::getOccurredAt));
        return list;
    }

    /**
     * 合并相邻月份/年份的节气表，避免单日表缺跨年边界节气。
     * 同名节气优先保留交节年等于目标年的记录。
     */
    private static Map<String, Solar> loadJieQiTableAround(int year) {
        Map<String, Solar> merged = new LinkedHashMap<>();
        int[][] probes = {
            {year - 1, 12},
            {year, 1},
            {year, 3},
            {year, 6},
            {year, 9},
            {year, 12},
            {year + 1, 1}
        };
        for (int[] probe : probes) {
            Lunar lunar = Solar.fromYmd(probe[0], probe[1], 1).getLunar();
            Map<String, Solar> table = lunar.getJieQiTable();
            if (table == null) {
                continue;
            }
            for (Map.Entry<String, Solar> entry : table.entrySet()) {
                Solar candidate = entry.getValue();
                if (candidate == null) {
                    continue;
                }
                Solar existing = merged.get(entry.getKey());
                if (existing == null) {
                    merged.put(entry.getKey(), candidate);
                } else if (candidate.getYear() == year && existing.getYear() != year) {
                    merged.put(entry.getKey(), candidate);
                }
            }
        }
        return merged;
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(BEIJING).toInstant());
    }

    @Value
    public static class TermInstant {
        String termCode;
        String termName;
        LocalDateTime occurredAt;
        String lunarDateText;
    }
}
