package com.ym.agriculture.shared.i18n.client;

/** 翻译服务调用异常。 */
public class I18nTranslationException extends RuntimeException {

    /** 稳定错误码。 */
    private final String errorCode;

    /** 是否允许 Worker 自动重试。 */
    private final boolean retryable;

    public I18nTranslationException(String message) {
        this(I18nTranslationErrorCode.UNKNOWN, true, message);
    }

    public I18nTranslationException(String message, Throwable cause) {
        this(I18nTranslationErrorCode.UNKNOWN, true, message, cause);
    }

    public I18nTranslationException(String errorCode, boolean retryable, String message) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public I18nTranslationException(String errorCode, boolean retryable, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    /**
     * 获取稳定错误码。
     *
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 判断是否允许自动重试。
     *
     * @return 可重试时返回 true
     */
    public boolean isRetryable() {
        return retryable;
    }
}
