package com.ym.agriculture.farmtask.inventory;

import com.ym.common.core.domain.R;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** 库存 Web 与小程序共享稳定错误响应，沿用 code/msg/data 并追加 errorCode。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ym.agriculture.stask")
@RequiredArgsConstructor
public class InventoryExceptionHandler {

    private static final Map<String, String> MESSAGE_KEYS = Map.ofEntries(
        Map.entry(InventoryConstants.ERROR_BUSINESS_RULE_VIOLATION,
            StaskMessageKeys.INVENTORY_ERROR_BUSINESS_RULE_VIOLATION),
        Map.entry(InventoryConstants.ERROR_VALIDATION_FAILED,
            StaskMessageKeys.INVENTORY_ERROR_VALIDATION_FAILED),
        Map.entry(InventoryConstants.ERROR_VERSION_CONFLICT, StaskMessageKeys.INVENTORY_ERROR_VERSION_CONFLICT),
        Map.entry(InventoryConstants.ERROR_ORDER_NOT_FOUND, StaskMessageKeys.INVENTORY_ERROR_NOT_FOUND),
        Map.entry(InventoryConstants.ERROR_INVENTORY_INSUFFICIENT, StaskMessageKeys.INVENTORY_ERROR_INSUFFICIENT),
        Map.entry(InventoryConstants.ERROR_INVENTORY_LOCKED, StaskMessageKeys.INVENTORY_ERROR_LOCKED),
        Map.entry(InventoryConstants.ERROR_IDEMPOTENCY_CONFLICT,
            StaskMessageKeys.INVENTORY_ERROR_IDEMPOTENCY_CONFLICT),
        Map.entry(InventoryConstants.ERROR_IDEMPOTENCY_PROCESSING,
            StaskMessageKeys.INVENTORY_ERROR_IDEMPOTENCY_PROCESSING),
        Map.entry(InventoryConstants.ERROR_RETURN_PENDING_EXISTS,
            StaskMessageKeys.INVENTORY_ERROR_RETURN_PENDING_EXISTS),
        Map.entry(InventoryConstants.ERROR_RETURN_QUANTITY_EXCEEDED,
            StaskMessageKeys.INVENTORY_ERROR_RETURN_QUANTITY_EXCEEDED),
        Map.entry(InventoryConstants.ERROR_RETURN_SOURCE_INVALID,
            StaskMessageKeys.INVENTORY_ERROR_RETURN_SOURCE_INVALID),
        Map.entry(InventoryConstants.ERROR_RETURN_LINE_NOT_REQUESTED,
            StaskMessageKeys.INVENTORY_ERROR_RETURN_LINE_NOT_REQUESTED),
        Map.entry(InventoryConstants.ERROR_LEGACY_MATERIAL_ID,
            StaskMessageKeys.INVENTORY_ERROR_LEGACY_MATERIAL_ID),
        Map.entry(InventoryConstants.ERROR_LEGACY_INVENTORY_RETIRED,
            StaskMessageKeys.INVENTORY_ERROR_LEGACY_RETIRED),
        Map.entry(InventoryConstants.ERROR_ASSET_STATE_INVALID,
            StaskMessageKeys.INVENTORY_ERROR_ASSET_STATE_INVALID),
        Map.entry(InventoryConstants.ERROR_ACTION_FORBIDDEN,
            StaskMessageKeys.INVENTORY_ERROR_ACTION_FORBIDDEN),
        Map.entry(InventoryConstants.ERROR_TASK_MATERIAL_INVALID,
            StaskMessageKeys.INVENTORY_ERROR_TASK_MATERIAL_INVALID),
        Map.entry(InventoryConstants.ERROR_TASK_MATERIAL_NOT_ALLOWED,
            StaskMessageKeys.INVENTORY_ERROR_TASK_MATERIAL_NOT_ALLOWED),
        Map.entry(InventoryConstants.ERROR_TASK_VOID_OUTBOUND_EXISTS,
            StaskMessageKeys.INVENTORY_ERROR_TASK_OUTBOUND_EXISTS),
        Map.entry(InventoryConstants.ERROR_ORDER_ALREADY_PROCESSED,
            StaskMessageKeys.INVENTORY_ERROR_ALREADY_PROCESSED),
        Map.entry(InventoryConstants.ERROR_NEGATIVE_CORRECTION_CONFIRMATION_REQUIRED,
            StaskMessageKeys.INVENTORY_ERROR_NEGATIVE_CORRECTION_ADMIN_REQUIRED),
        Map.entry(InventoryConstants.ERROR_OUTBOUND_RETURN_EXISTS,
            StaskMessageKeys.INVENTORY_ERROR_OUTBOUND_RETURN_EXISTS)
    );

    private final StaskMessageResolver messages;

    @ExceptionHandler(InventoryBusinessException.class)
    public InventoryErrorResponse handle(InventoryBusinessException exception) {
        InventoryErrorResponse response = new InventoryErrorResponse();
        response.setCode(exception.getCode());
        response.setMsg(localize(exception.getErrorCode(), exception.getMessage()));
        response.setErrorCode(exception.getErrorCode());
        response.setLineErrors(exception.getLineErrors().stream().map(this::localizeLine)
            .collect(Collectors.toList()));
        return response;
    }

    private InventoryBusinessException.LineError localizeLine(InventoryBusinessException.LineError line) {
        return new InventoryBusinessException.LineError(line.lineId(), line.inventoryMaterialId(),
            line.errorCode(), localize(line.errorCode(), line.msg()), line.availableQuantity(),
            line.remainingQuantity());
    }

    private String localize(String errorCode, String fallback) {
        Locale locale = LocaleContextHolder.getLocale();
        if (InventoryConstants.ERROR_BUSINESS_RULE_VIOLATION.equals(errorCode)
            && "zh".equalsIgnoreCase(locale.getLanguage())
            && fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        String key = MESSAGE_KEYS.get(errorCode);
        return key == null ? fallback : messages.message(key);
    }

    @Getter
    @Setter
    public static class InventoryErrorResponse extends R<Void> {
        private String errorCode;
        private List<InventoryBusinessException.LineError> lineErrors = List.of();
    }
}
