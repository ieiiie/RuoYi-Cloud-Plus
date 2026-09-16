package com.ym.agriculture.farmtask.inventory;

import com.ym.agriculture.farmtask.inventory.controller.AssetInventoryController;
import com.ym.agriculture.farmtask.inventory.controller.InventoryController;
import com.ym.agriculture.farmtask.inventory.support.InventoryDisplayLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** 在库存 Web 与小程序响应序列化前补充当前语言的展示标签。 */
@ControllerAdvice(assignableTypes = {
    InventoryController.class,
    AssetInventoryController.class
})
@RequiredArgsConstructor
public class InventoryDisplayResponseAdvice implements ResponseBodyAdvice<Object> {

    private final InventoryDisplayLabelResolver labelResolver;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
        ServerHttpResponse response) {
        labelResolver.enrich(body);
        return body;
    }
}
