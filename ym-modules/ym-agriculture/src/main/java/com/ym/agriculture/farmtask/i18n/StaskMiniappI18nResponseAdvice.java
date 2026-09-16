package com.ym.agriculture.farmtask.i18n;

import com.ym.agriculture.shared.i18n.StaskI18nResponseLocalizer;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;

import com.ym.common.core.domain.R;
import com.ym.common.core.constant.HttpStatus;
import com.ym.common.tenant.helper.TenantHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Locale;

/**
 * 小程序 stask 返回结果维文化切面。
 */
@ControllerAdvice(basePackages = "com.ym.agriculture.miniapp.controller")
@RequiredArgsConstructor
public class StaskMiniappI18nResponseAdvice implements ResponseBodyAdvice<Object> {

    private final StaskI18nResponseLocalizer responseLocalizer;
    private final StaskMessageResolver messages;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
        ServerHttpResponse response) {
        if ("ug".equalsIgnoreCase(LocaleContextHolder.getLocale().getLanguage())) {
            responseLocalizer.localizeUyghur(TenantHelper.getTenantId(), body);
            localizeEnvelope(body);
        }
        return body;
    }

    private void localizeEnvelope(Object body) {
        if (body instanceof R<?> result) {
            if (result.getCode() == HttpStatus.SUCCESS && R.ok().getMsg().equals(result.getMsg())) {
                result.setMsg(messages.message(StaskMessageKeys.MESSAGE_COMMON_OPERATION_SUCCESS));
            } else if (result.getCode() != HttpStatus.SUCCESS && R.fail().getMsg().equals(result.getMsg())) {
                result.setMsg(messages.message(StaskMessageKeys.MESSAGE_COMMON_OPERATION_FAILED));
            }
            localizeEnvelope(result.getData());
            return;
        }
    }
}
