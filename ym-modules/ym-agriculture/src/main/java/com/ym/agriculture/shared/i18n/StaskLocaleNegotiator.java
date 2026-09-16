package com.ym.agriculture.shared.i18n;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * stask 请求语言协商器。
 *
 * <p>小程序接口支持中文和维文，管理端 stask 接口固定使用中文。</p>
 */
@Component
public class StaskLocaleNegotiator {

    static final String MINIAPP_PREFIX = "/miniapp/smart-farming/stask/";
    static final String ADMIN_PREFIX = "/smart-farming/stask/";

    /**
     * 判断请求是否属于 stask HTTP 接口。
     *
     * @param request HTTP 请求
     * @return 是否属于 stask 接口
     */
    public boolean supports(HttpServletRequest request) {
        String path = applicationPath(request);
        return path.startsWith(MINIAPP_PREFIX) || path.startsWith(ADMIN_PREFIX);
    }

    /**
     * 解析请求使用的语言。
     *
     * @param request HTTP 请求
     * @return 中文或维文区域
     */
    public Locale resolve(HttpServletRequest request) {
        String path = applicationPath(request);
        if (!path.startsWith(MINIAPP_PREFIX)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return resolveMiniappLanguage(request.getHeader("Accept-Language"));
    }

    Locale resolveMiniappLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        try {
            for (Locale.LanguageRange range : Locale.LanguageRange.parse(acceptLanguage.replace('_', '-'))) {
                if (range.getWeight() <= 0) {
                    continue;
                }
                String language = range.getRange().toLowerCase(Locale.ROOT);
                if ("ug".equals(language) || "ug-cn".equals(language)) {
                    return StaskMessageResolver.UYGHUR_LOCALE;
                }
                if ("zh".equals(language) || "zh-cn".equals(language)
                    || "zh-hans".equals(language) || "zh-hans-cn".equals(language)) {
                    return Locale.SIMPLIFIED_CHINESE;
                }
            }
            return Locale.SIMPLIFIED_CHINESE;
        } catch (IllegalArgumentException ignored) {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }

    private static String applicationPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
