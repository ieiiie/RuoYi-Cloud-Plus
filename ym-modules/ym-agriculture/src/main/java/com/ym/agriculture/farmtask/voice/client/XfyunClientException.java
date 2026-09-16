package com.ym.agriculture.farmtask.voice.client;

/**
 * 讯飞语音播报客户端异常。
 */
public class XfyunClientException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;

    public XfyunClientException(String message) {
        this("VOICE_PROVIDER_ERROR", false, message, null);
    }

    public XfyunClientException(String message, Throwable cause) {
        this("VOICE_PROVIDER_ERROR", false, message, cause);
    }

    public XfyunClientException(String errorCode, boolean retryable, String message) {
        this(errorCode, retryable, message, null);
    }

    public XfyunClientException(String errorCode, boolean retryable, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
