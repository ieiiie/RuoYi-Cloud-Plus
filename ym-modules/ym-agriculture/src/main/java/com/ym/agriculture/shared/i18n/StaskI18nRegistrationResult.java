package com.ym.agriculture.shared.i18n;

import com.ym.agriculture.shared.i18n.model.I18nTextSource;

import java.util.List;

/**
 * 一批业务资源登记结果。
 *
 * @param resourceCount 按本次所选资源类型实际读取的业务行数
 * @param sources 从这些业务行提取的翻译文本源
 */
public record StaskI18nRegistrationResult(
    int resourceCount,
    List<I18nTextSource> sources
) {

    public StaskI18nRegistrationResult {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    /** 返回没有读取任何业务行的结果。 */
    public static StaskI18nRegistrationResult empty() {
        return new StaskI18nRegistrationResult(0, List.of());
    }
}
