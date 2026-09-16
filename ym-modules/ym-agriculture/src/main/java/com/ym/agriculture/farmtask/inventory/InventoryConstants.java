package com.ym.agriculture.farmtask.inventory;

/** V1.5 库存、领料和资产领域常量。 */
public final class InventoryConstants {

    public static final String LEDGER_INBOUND = "INBOUND";
    public static final String LEDGER_OUTBOUND = "OUTBOUND";
    public static final String LEDGER_RETURN = "RETURN";
    public static final String LEDGER_ADJUST = "ADJUST";

    /** 已退库单作废时写入的负数库存冲销流水。 */
    public static final String SUBTYPE_RETURN_VOID = "RETURN_VOID";

    public static final String SOURCE_MATERIAL_RECEIPT = "MATERIAL_RECEIPT";
    public static final String SOURCE_WEB_DIRECT = "WEB_DIRECT";
    public static final String SOURCE_LEADER_APPLY = "LEADER_APPLY";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_VOIDED = "VOIDED";
    public static final String STATUS_PENDING_RETURN = "PENDING_RETURN";
    public static final String STATUS_RETURNED = "RETURNED";

    public static final String RECEIPT_PENDING_ACCEPTANCE = "PENDING_ACCEPTANCE";
    public static final String RECEIPT_UNCLAIMED = "UNCLAIMED";
    public static final String RECEIPT_PENDING_OUTBOUND = "PENDING_OUTBOUND";
    public static final String RECEIPT_RECEIVED = "RECEIVED";
    public static final String RECEIPT_VOIDED = "VOIDED";

    public static final String ASSET_IDLE = "IDLE";
    public static final String ASSET_IN_USE = "IN_USE";
    public static final String ASSET_MAINTENANCE = "MAINTENANCE";
    public static final String ASSET_SCRAPPED = "SCRAPPED";

    public static final String ERROR_BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    public static final String ERROR_VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String ERROR_VERSION_CONFLICT = "VERSION_CONFLICT";
    public static final String ERROR_ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String ERROR_INVENTORY_INSUFFICIENT = "INVENTORY_INSUFFICIENT";
    public static final String ERROR_INVENTORY_LOCKED = "INVENTORY_LOCKED";
    public static final String ERROR_IDEMPOTENCY_CONFLICT = "IDEMPOTENCY_CONFLICT";
    public static final String ERROR_IDEMPOTENCY_PROCESSING = "IDEMPOTENCY_PROCESSING";
    public static final String ERROR_RETURN_PENDING_EXISTS = "RETURN_PENDING_EXISTS";
    public static final String ERROR_RETURN_QUANTITY_EXCEEDED = "RETURN_QUANTITY_EXCEEDED";
    public static final String ERROR_RETURN_SOURCE_INVALID = "RETURN_SOURCE_INVALID";
    public static final String ERROR_RETURN_LINE_NOT_REQUESTED = "RETURN_LINE_NOT_REQUESTED";
    public static final String ERROR_LEGACY_MATERIAL_ID = "LEGACY_MATERIAL_ID";
    public static final String ERROR_LEGACY_INVENTORY_RETIRED = "LEGACY_INVENTORY_RETIRED";
    public static final String ERROR_ASSET_STATE_INVALID = "ASSET_STATE_INVALID";
    public static final String ERROR_ACTION_FORBIDDEN = "ACTION_FORBIDDEN";
    public static final String ERROR_TASK_MATERIAL_INVALID = "TASK_MATERIAL_INVALID";
    public static final String ERROR_TASK_MATERIAL_NOT_ALLOWED = "TASK_MATERIAL_NOT_ALLOWED";
    public static final String ERROR_TASK_VOID_OUTBOUND_EXISTS = "TASK_VOID_OUTBOUND_EXISTS";
    public static final String ERROR_ORDER_ALREADY_PROCESSED = "ORDER_ALREADY_PROCESSED";
    /**
     * 历史纠错或作废回滚会导致负库存，但调用方未明确确认。
     *
     * <p>响应值沿用旧错误码，避免已发布客户端因错误码变更失去兼容。</p>
     */
    public static final String ERROR_NEGATIVE_CORRECTION_CONFIRMATION_REQUIRED =
        "NEGATIVE_CORRECTION_ADMIN_REQUIRED";

    /**
     * @deprecated 请使用 {@link #ERROR_NEGATIVE_CORRECTION_CONFIRMATION_REQUIRED}。
     */
    @Deprecated
    public static final String ERROR_NEGATIVE_CORRECTION_ADMIN_REQUIRED =
        ERROR_NEGATIVE_CORRECTION_CONFIRMATION_REQUIRED;
    public static final String ERROR_OUTBOUND_RETURN_EXISTS = "OUTBOUND_RETURN_EXISTS";

    public static final int QUANTITY_SCALE = 1;

    private InventoryConstants() {
    }
}
