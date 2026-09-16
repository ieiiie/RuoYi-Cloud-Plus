package com.ym.agriculture.farmtask.i18n;

import com.ym.agriculture.shared.i18n.StaskLocaleNegotiator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

/**
 * 约束 stask 静态消息的请求语言：小程序可使用维文，管理端始终中文。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
@Slf4j
public class StaskLocaleContextFilter extends OncePerRequestFilter {

    private final StaskLocaleNegotiator localeNegotiator;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !localeNegotiator.supports(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        LocaleContext previous = LocaleContextHolder.getLocaleContext();
        Locale locale = localeNegotiator.resolve(request);
        log.debug("stask locale negotiated: requestURI={}, locale={}",
            request.getRequestURI(), locale.toLanguageTag());
        try {
            LocaleContextHolder.setLocale(locale);
            filterChain.doFilter(new NegotiatedLocaleRequest(request, locale), response);
        } finally {
            if (previous == null) {
                LocaleContextHolder.resetLocaleContext();
            } else {
                LocaleContextHolder.setLocaleContext(previous);
            }
        }
    }

    /**
     * 让 DispatcherServlet 的平台语言解析器读取已协商语言，避免它覆盖过滤器上下文。
     */
    private static final class NegotiatedLocaleRequest extends HttpServletRequestWrapper {

        private static final String CONTENT_LANGUAGE = "content-language";

        private final Locale locale;

        private NegotiatedLocaleRequest(HttpServletRequest request, Locale locale) {
            super(request);
            this.locale = locale;
        }

        @Override
        public Locale getLocale() {
            return locale;
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return Collections.enumeration(Collections.singleton(locale));
        }

        /**
         * 兼容平台 {@code I18nLocaleResolver}：该解析器读取请求的
         * {@code Content-Language}，而不是 {@link #getLocale()}。这里将已经通过
         * {@code Accept-Language} 协商出的 stask 语言桥接给 MVC，避免
         * DispatcherServlet 再次解析时覆盖线程语言。
         */
        @Override
        public String getHeader(String name) {
            if (CONTENT_LANGUAGE.equalsIgnoreCase(name)) {
                return locale.getLanguage() + "_" + locale.getCountry();
            }
            return super.getHeader(name);
        }
    }

}
