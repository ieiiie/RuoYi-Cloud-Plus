package com.ym.agriculture.shared.i18n.client;

/** 中文到维吾尔文翻译提供者。 */
public interface I18nTranslationProvider {

    /**
     * 翻译中文文本。
     *
     * @param sourceText 中文原文
     * @return 维吾尔文译文
     */
    String translateZhToUyghur(String sourceText);

    /**
     * 提供者标识。
     *
     * @return 提供者名称
     */
    default String providerName() {
        return "xfyun";
    }
}
