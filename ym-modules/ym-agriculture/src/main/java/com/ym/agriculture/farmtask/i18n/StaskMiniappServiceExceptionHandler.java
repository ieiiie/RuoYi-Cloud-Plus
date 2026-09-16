package com.ym.agriculture.farmtask.i18n;

import com.ym.agriculture.shared.i18n.StaskMiniappErrorResponse;
import com.ym.agriculture.shared.i18n.StaskMiniappException;

import com.ym.common.core.domain.R;
import com.ym.common.core.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 仅覆盖小程序 stask 的业务异常，保留全局异常响应结构及业务错误码。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ym.agriculture.miniapp.controller")
public class StaskMiniappServiceExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public R<Void> handle(ServiceException exception, HttpServletRequest request) {
        log.warn("stask小程序业务请求失败 exceptionType={}, code={}",
            exception.getClass().getSimpleName(), exception.getCode());
        return exception.getCode() == null ? R.fail(exception.getMessage())
            : R.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(StaskMiniappException.class)
    public StaskMiniappErrorResponse handleStable(StaskMiniappException exception,
        HttpServletRequest request) {
        log.warn("stask小程序业务请求失败 errorCode={}, code={}",
            exception.getErrorCode(), exception.getCode());
        return new StaskMiniappErrorResponse(exception.getCode(), exception.getMessage(),
            exception.getErrorCode());
    }
}
