package com.ym.agriculture.shared.i18n;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 一次业务文本扫描结果，供全租户预处理汇总使用。
 *
 * @param resourceCount 扫描到的业务记录数
 * @param sourceTextCount 提取到的非空业务文本数，未去重
 * @param sourceTexts 本次扫描得到的精确中文原文集合
 */
public record StaskI18nScanResult(
    int resourceCount,
    long sourceTextCount,
    Set<String> sourceTexts
) {

    /** 返回空扫描结果。 */
    public static StaskI18nScanResult empty() {
        return new StaskI18nScanResult(0, 0, Set.of());
    }

    /** 根据一批已登记资源构造扫描结果。 */
    public static StaskI18nScanResult of(int resourceCount, Collection<I18nTextSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return new StaskI18nScanResult(resourceCount, 0, Set.of());
        }
        LinkedHashSet<String> texts = new LinkedHashSet<>();
        long count = 0;
        for (I18nTextSource source : sources) {
            if (source != null && StringUtils.isNotBlank(source.sourceText())) {
                count++;
                texts.add(source.sourceText());
            }
        }
        return new StaskI18nScanResult(resourceCount, count, Set.copyOf(texts));
    }

    /** 合并两个扫描结果。 */
    public StaskI18nScanResult merge(StaskI18nScanResult other) {
        if (other == null) {
            return this;
        }
        LinkedHashSet<String> texts = new LinkedHashSet<>(sourceTexts);
        texts.addAll(other.sourceTexts);
        return new StaskI18nScanResult(resourceCount + other.resourceCount,
            sourceTextCount + other.sourceTextCount, Set.copyOf(texts));
    }
}
