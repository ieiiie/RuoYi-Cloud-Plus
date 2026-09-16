package com.ym.agriculture.farmtask.inventory;

import lombok.Getter;

import java.util.List;

/** 带稳定错误码及可选行错误的库存领域异常。 */
@Getter
public class InventoryBusinessException extends RuntimeException {

    private final int code;
    private final String errorCode;
    private final List<LineError> lineErrors;

    public InventoryBusinessException(String errorCode, String message) {
        this(errorCode, 409, message, List.of());
    }

    public InventoryBusinessException(String errorCode, int code, String message) {
        this(errorCode, code, message, List.of());
    }

    public InventoryBusinessException(String errorCode, int code, String message, List<LineError> lineErrors) {
        super(message);
        this.errorCode = errorCode;
        this.code = code;
        this.lineErrors = lineErrors == null ? List.of() : List.copyOf(lineErrors);
    }

    /** 将普通库存业务校验统一转换为带稳定错误码的冲突响应。 */
    public static InventoryBusinessException rule(String message) {
        return new InventoryBusinessException(InventoryConstants.ERROR_BUSINESS_RULE_VIOLATION, 409, message);
    }

    /** 行级错误，ID 保持 Long，由全局序列化规则安全输出。 */
    public record LineError(Long lineId, Long inventoryMaterialId, String errorCode,
                            String msg, String availableQuantity, String remainingQuantity) {
    }
}
