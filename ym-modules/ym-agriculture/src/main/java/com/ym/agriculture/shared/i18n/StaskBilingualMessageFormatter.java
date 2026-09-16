package com.ym.agriculture.shared.i18n;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 异步通知的中维双语正文格式器。
 *
 * <p>该组件仅读取已成功译文；读取失败或译文缺失时维文段回退中文，不登记文本也不调用远程翻译。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaskBilingualMessageFormatter {

    private final StaskMessageResolver messages;
    private final ISfI18nTextService i18nTextService;

    /**
     * 生成只有静态参数的中维双语正文。
     *
     * @param key 静态模板键
     * @param args 模板参数
     * @return 中文段和维文段
     */
    public BilingualContent format(String key, Object... args) {
        return new BilingualContent(messages.chinese(key, args), messages.uyghur(key, args));
    }

    /**
     * 生成包含一个可翻译业务文本的中维双语正文。
     *
     * @param tenantId 当前租户
     * @param key 静态模板键，使用第一个参数承载业务文本
     * @param source 业务原文资源
     * @return 中文段和维文段
     */
    public BilingualContent formatWithResource(String tenantId, String key, I18nTextSource source) {
        String chinese = source == null ? null : source.sourceText();
        String uyghur = resolveUyghur(tenantId, source);
        return new BilingualContent(messages.chinese(key, chinese), messages.uyghur(key,
            StringUtils.blankToDefault(uyghur, chinese)));
    }

    private String resolveUyghur(String tenantId, I18nTextSource source) {
        if (source == null || StringUtils.isBlank(tenantId) || StringUtils.isBlank(source.sourceText())) {
            return null;
        }
        try {
            Map<I18nTextSource, String> translations = i18nTextService.resolveUyghurTextsByResources(tenantId,
                List.of(source));
            return translations.get(source);
        } catch (RuntimeException e) {
            log.warn("stask双语通知读取业务译文失败 tenantId={}, resourceType={}, errorType={}",
                tenantId, source.resourceType(), e.getClass().getSimpleName());
            return null;
        }
    }
}
