package com.ym.agriculture.shared.i18n;

/**
 * 需要在组成字段本地化后重建派生展示文本的视图对象。
 */
public interface StaskI18nComposite {

    /**
     * 使用已经本地化的组成字段重新生成派生展示文本。
     */
    void rebuildLocalizedText();
}
