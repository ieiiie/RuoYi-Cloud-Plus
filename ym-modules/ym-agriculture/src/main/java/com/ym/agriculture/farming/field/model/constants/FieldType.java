package com.ym.agriculture.farming.field.model.constants;

import com.ym.common.core.utils.StringUtils;

import java.util.Locale;

/**
 * 地块类型。
 */
public final class FieldType {

    public static final String FIELD = "FIELD";
    public static final String GREENHOUSE = "GREENHOUSE";

    private FieldType() {
    }

    public static String normalize(String value) {
        return StringUtils.isBlank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    public static String normalizeOrDefault(String value) {
        String normalized = normalize(value);
        return normalized == null ? FIELD : normalized;
    }

    public static boolean isGreenhouse(String value) {
        return GREENHOUSE.equals(normalize(value));
    }

    public static boolean isSupported(String value) {
        String normalized = normalize(value);
        return FIELD.equals(normalized) || GREENHOUSE.equals(normalized);
    }
}
