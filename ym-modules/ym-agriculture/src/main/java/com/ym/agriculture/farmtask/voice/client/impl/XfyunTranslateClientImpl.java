package com.ym.agriculture.farmtask.voice.client.impl;

import com.ym.agriculture.shared.i18n.client.I18nTranslationProvider;
import com.ym.agriculture.farmtask.voice.client.XfyunTranslateClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 已废弃的 stask 语音翻译适配器。
 *
 * <p>保留接口以兼容语音服务和既有测试，实际翻译统一委托农业核心的通用翻译提供者。</p>
 */
@Deprecated
@Component
@RequiredArgsConstructor
public class XfyunTranslateClientImpl implements XfyunTranslateClient {

    private final I18nTranslationProvider translationProvider;

    @Override
    public String translateZhToUyghur(String sourceText) {
        return translationProvider.translateZhToUyghur(sourceText);
    }
}
