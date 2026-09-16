package com.ym.agriculture.shared.i18n.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 资源级维文解析结果。
 *
 * @param translations 已成功且与当前中文原文匹配的维文
 * @param registeredCount 本次尝试新增、恢复或优选译文晋级的唯一词条数量
 */
public record I18nResourceResolution(
    Map<I18nTextSource, String> translations,
    int registeredCount
) {

    public I18nResourceResolution {
        translations = Collections.unmodifiableMap(new LinkedHashMap<>(
            Objects.requireNonNull(translations, "translations")));
        if (registeredCount < 0) {
            throw new IllegalArgumentException("registeredCount must not be negative");
        }
    }

    /** 返回无译文且无新登记的结果。 */
    public static I18nResourceResolution empty() {
        return new I18nResourceResolution(Map.of(), 0);
    }
}
