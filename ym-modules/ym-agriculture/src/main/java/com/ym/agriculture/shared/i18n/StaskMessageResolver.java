package com.ym.agriculture.shared.i18n;

import com.ym.common.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * stask 固定消息解析器。
 *
 * <p>消息资源由 stask 模块独立维护。请求消息使用当前请求语言；异步通知和写入中文业务原文时
 * 显式指定语言，不修改线程级 {@link LocaleContextHolder}。</p>
 */
@Component
@Slf4j
public class StaskMessageResolver {

    /** stask 维吾尔语区域。 */
    public static final Locale UYGHUR_LOCALE = Locale.forLanguageTag("ug-CN");

    private final MessageSource messageSource;

    public StaskMessageResolver(MessageSource platformMessageSource) {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/stask/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        // 独立加载领域资源，保留平台消息回退，不覆盖全局 MessageSource。
        source.setParentMessageSource(platformMessageSource);
        this.messageSource = source;
    }

    /**
     * 按当前请求语言解析消息，缺少译文时回退中文基线。
     *
     * @param key 消息资源键
     * @param args 格式化参数
     * @return 已解析消息
     */
    public String message(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        String resolved = messageSource.getMessage(key, args, null, locale);
        if (resolved == null) {
            log.warn("stask消息资源缺失 key={}, locale={}", key, locale.toLanguageTag());
            return chinese(key, args);
        }
        return resolved;
    }

    /**
     * 解析中文基线消息。
     *
     * @param key 消息资源键
     * @param args 格式化参数
     * @return 中文消息
     */
    public String chinese(String key, Object... args) {
        return messageSource.getMessage(key, args, key, Locale.SIMPLIFIED_CHINESE);
    }

    /**
     * 解析维文消息，资源缺失时回退中文基线。
     *
     * @param key 消息资源键
     * @param args 格式化参数
     * @return 维文或中文回退消息
     */
    public String uyghur(String key, Object... args) {
        String resolved = messageSource.getMessage(key, args, null, UYGHUR_LOCALE);
        if (resolved == null) {
            log.warn("stask维文消息资源缺失 key={}", key);
            return chinese(key, args);
        }
        return resolved;
    }

    /**
     * 创建使用当前请求语言的业务异常。
     *
     * @param key 消息资源键
     * @param args 格式化参数
     * @return 业务异常
     */
    public ServiceException exception(String key, Object... args) {
        return new ServiceException(message(key, args));
    }

    /**
     * 创建带稳定错误码的 stask 小程序异常。
     *
     * @param errorCode 稳定错误码
     * @param key       消息资源键
     * @param args      消息参数
     * @return stask 小程序异常
     */
    public StaskMiniappException stableException(String errorCode, String key, Object... args) {
        return new StaskMiniappException(errorCode, message(key, args));
    }

    /**
     * 创建带稳定错误码和 HTTP 语义状态码的 stask 小程序异常。
     *
     * @param errorCode 稳定错误码
     * @param code      HTTP/业务响应状态码
     * @param key       消息资源键
     * @param args      消息参数
     * @return stask 小程序异常
     */
    public StaskMiniappException stableException(String errorCode, int code, String key, Object... args) {
        return new StaskMiniappException(errorCode, code, message(key, args));
    }
}
