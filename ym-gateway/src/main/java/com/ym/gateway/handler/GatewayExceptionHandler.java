package com.ym.gateway.handler;

import cn.dev33.satoken.exception.NotLoginException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.ym.common.core.constant.HttpStatus;
import com.ym.common.core.domain.R;
import com.ym.common.json.utils.JsonUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 网关统一异常处理
 *
 * @author Lion Li
 */
@Slf4j
@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public void handleNotLogin(HttpServletRequest request, HttpServletResponse response, NotLoginException ex) throws IOException {
        log.warn("[网关认证失败]请求路径:{},异常信息:{}", request.getRequestURI(), ex.getMessage());
        writeJson(response, HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    public void handle(HttpServletRequest request, HttpServletResponse response, Throwable ex) throws IOException {
        int code;
        String msg;
        String exceptionMessage = ex.getMessage();
        if ("NotFoundException".equals(ex.getClass().getSimpleName())
            || (exceptionMessage != null && exceptionMessage.contains("Unable to find instance"))) {
            code = HttpStatus.NOT_FOUND;
            msg = "服务未找到";
        } else if (ex instanceof ResponseStatusException responseStatusException) {
            code = responseStatusException.getStatusCode().value();
            msg = responseStatusException.getMessage();
        } else {
            code = HttpStatus.ERROR;
            msg = "内部服务器错误";
        }

        log.error("[网关异常处理]请求路径:{},异常信息:{}", request.getRequestURI(), exceptionMessage, ex);
        writeJson(response, code, msg);
    }

    private void writeJson(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JsonUtils.toJsonString(R.fail(code, msg)));
    }
}
