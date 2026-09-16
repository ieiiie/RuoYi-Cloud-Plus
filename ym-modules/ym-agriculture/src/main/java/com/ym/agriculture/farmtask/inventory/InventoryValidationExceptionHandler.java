package com.ym.agriculture.farmtask.inventory;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import com.ym.agriculture.farmtask.inventory.InventoryExceptionHandler.InventoryErrorResponse;
import com.ym.agriculture.farmtask.inventory.controller.AssetInventoryController;
import com.ym.agriculture.farmtask.inventory.controller.InventoryController;
import com.ym.agriculture.farmtask.workorder.controller.SfStaskWorkOrderController;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Locale;

/** 为本次库存及任务物料接口的参数校验失败补充稳定错误码。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
    InventoryController.class,
    AssetInventoryController.class,
    SfStaskWorkOrderController.class
})
@RequiredArgsConstructor
public class InventoryValidationExceptionHandler {

    private final StaskMessageResolver messages;

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        HandlerMethodValidationException.class
    })
    public InventoryErrorResponse handleValidation(Exception exception) {
        InventoryErrorResponse response = new InventoryErrorResponse();
        response.setCode(400);
        response.setMsg(validationMessage(exception));
        response.setErrorCode(InventoryConstants.ERROR_VALIDATION_FAILED);
        return response;
    }

    private String validationMessage(Exception exception) {
        String detail = null;
        if (exception instanceof MethodArgumentNotValidException invalid) {
            detail = firstFieldMessage(invalid);
        } else if (exception instanceof BindException bind) {
            detail = firstFieldMessage(bind);
        } else if (exception instanceof ConstraintViolationException constraint
            && !constraint.getConstraintViolations().isEmpty()) {
            detail = constraint.getConstraintViolations().iterator().next().getMessage();
        }
        Locale locale = LocaleContextHolder.getLocale();
        if ("zh".equalsIgnoreCase(locale.getLanguage()) && detail != null && !detail.isBlank()) {
            return detail;
        }
        return messages.message(StaskMessageKeys.INVENTORY_ERROR_VALIDATION_FAILED);
    }

    private String firstFieldMessage(BindException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        return fieldError == null ? null : fieldError.getDefaultMessage();
    }
}
