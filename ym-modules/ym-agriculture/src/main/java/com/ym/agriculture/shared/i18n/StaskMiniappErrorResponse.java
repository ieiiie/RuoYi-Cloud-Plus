package com.ym.agriculture.shared.i18n;

import com.ym.common.core.domain.R;
import com.ym.common.core.constant.HttpStatus;

/**
 * stask 小程序稳定错误响应，保留既有 code/msg/data 字段并追加 errorCode。
 */
public class StaskMiniappErrorResponse extends R<Void> {

    private String errorCode;

    public StaskMiniappErrorResponse() {
        super();
    }

    public StaskMiniappErrorResponse(Integer code, String msg, String errorCode) {
        super();
        setCode(code == null ? HttpStatus.ERROR : code);
        setMsg(msg);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
