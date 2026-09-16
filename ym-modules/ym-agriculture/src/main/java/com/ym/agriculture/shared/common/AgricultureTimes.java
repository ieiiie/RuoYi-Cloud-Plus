package com.ym.agriculture.shared.common;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/** 迁移期间兼容旧业务 {@link Date} 与新基础实体 {@link LocalDateTime} 的时间边界。 */
public final class AgricultureTimes {
    private AgricultureTimes() {
    }

    public static LocalDateTime toLocalDateTime(Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    public static Date toDate(LocalDateTime value) {
        return value == null ? null : Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }
}
