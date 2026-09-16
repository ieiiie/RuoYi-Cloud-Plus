package com.ym.agriculture.shared.i18n;

/**
 * stask 小程序业务异常；稳定错误码与本地化展示消息分离。
 */
public final class StaskMiniappException extends RuntimeException {

    private final String errorCode;
    private final Integer code;

    public StaskMiniappException(String errorCode, String message) {
        this(errorCode, 500, message);
    }

    public StaskMiniappException(String errorCode, Integer code, String message) {
        super(message);
        this.errorCode = errorCode;
        this.code = code;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getCode() {
        return code;
    }
}
