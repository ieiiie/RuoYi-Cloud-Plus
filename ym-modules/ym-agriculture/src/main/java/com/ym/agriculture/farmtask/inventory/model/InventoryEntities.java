package com.ym.agriculture.farmtask.inventory.model;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 库存、领料和资产领域的 MyBatis-Plus 持久化模型。 */
public final class InventoryEntities {

    @Data
    @TableName("sf_inventory_material_category")
    public static class MaterialCategory {
        @TableId(value = "category_id", type = IdType.ASSIGN_ID)
        private Long categoryId;
        private String tenantId;
        private String categoryName;
        private Integer sortOrder;
        private Boolean enabled;
        @Version
        private Long version;
        private Long createDept;
        private Long createBy;
        private Date createTime;
        private Long updateBy;
        private Date updateTime;
        private String remark;
    }

    @Data
    @TableName("sf_inventory_material")
    public static class Material {
        @TableId(value = "material_id", type = IdType.ASSIGN_ID)
        private Long materialId;
        private String tenantId;
        private String materialCode;
        private String materialName;
        private Long categoryId;
        private String category;
        private String specification;
        private String unit;
        private Boolean enabled;
        @Version
        private Long version;
        private String delFlag;
        private Long createDept;
        private Long createBy;
        private Date createTime;
        private Long updateBy;
        private Date updateTime;
        private String remark;
    }

    @Data
    @TableName("sf_inventory_balance")
    public static class Balance {
        @TableId(value = "balance_id", type = IdType.ASSIGN_ID)
        private Long balanceId;
        private String tenantId;
        private Long materialId;
        private BigDecimal quantity;
        @TableField(updateStrategy = FieldStrategy.ALWAYS)
        private String abnormalReason;
        private Boolean outboundLocked;
        @Version
        private Long version;
        private Long createBy;
        private Date createTime;
        private Long updateBy;
        private Date updateTime;
    }

    @Data
    @TableName("sf_inventory_ledger")
    public static class Ledger {
        @TableId(value = "ledger_id", type = IdType.ASSIGN_ID)
        private Long ledgerId;
        private String tenantId;
        private Long materialId;
        private String ledgerType;
        private String businessSubtype;
        private BigDecimal quantityDelta;
        private BigDecimal balanceAfter;
        private String relatedOrderType;
        private Long relatedOrderId;
        private String relatedOrderNo;
        private Long sourceLineId;
        private Boolean effective;
        @Version
        private Long version;
        private String correctionReason;
        private Long operatorEmployeeId;
        private String operatorNameSnapshot;
        private Date occurredAt;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_inventory_inbound_order")
    public static class InboundOrder {
        @TableId(value = "inbound_order_id", type = IdType.ASSIGN_ID)
        private Long inboundOrderId;
        private String tenantId;
        private String orderNo;
        private String status;
        private String supplierName;
        private Date businessDate;
        @Version
        private Long version;
        private Long createDept;
        private Long createBy;
        private Date createTime;
        private Long updateBy;
        private Date updateTime;
        private String remark;
    }

    @Data
    @TableName("sf_inventory_inbound_line")
    public static class InboundLine {
        @TableId(value = "inbound_line_id", type = IdType.ASSIGN_ID)
        private Long inboundLineId;
        private String tenantId;
        private Long inboundOrderId;
        private Long materialId;
        private String materialCodeSnapshot;
        private String materialNameSnapshot;
        private String specificationSnapshot;
        private String unitSnapshot;
        private BigDecimal quantity;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_inventory_outbound_order")
    public static class OutboundOrder {
        @TableId(value = "outbound_order_id", type = IdType.ASSIGN_ID)
        private Long outboundOrderId;
        private String tenantId;
        private String orderNo;
        private String source;
        private String status;
        private Long materialReceiptId;
        private Long taskPackageId;
        private Long farmItemId;
        private Long leaderEmployeeId;
        private String receiverNameSnapshot;
        private String taskNameSnapshot;
        private String farmWorkNameSnapshot;
        private String greenhouseNamesSnapshot;
        private Date businessDate;
        private Long confirmedBy;
        private Date confirmedAt;
        private Long cancelledBy;
        private Date cancelledAt;
        @Version
        private Long version;
        private Long createDept;
        private Long createBy;
        private Date createTime;
        private Long updateBy;
        private Date updateTime;
        private String remark;
    }

    @Data
    @TableName("sf_inventory_outbound_line")
    public static class OutboundLine {
        @TableId(value = "outbound_line_id", type = IdType.ASSIGN_ID)
        private Long outboundLineId;
        private String tenantId;
        private Long outboundOrderId;
        private Long materialReceiptLineId;
        private Long materialId;
        private String materialCodeSnapshot;
        private String materialNameSnapshot;
        private String specificationSnapshot;
        private String unitSnapshot;
        private BigDecimal requestedQuantity;
        private BigDecimal actualQuantity;
        private Boolean deletedByKeeper;
        @Version
        private Long version;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_inventory_return_order")
    public static class ReturnOrder {
        @TableId(value = "return_order_id", type = IdType.ASSIGN_ID)
        private Long returnOrderId;
        private String tenantId;
        private String orderNo;
        private String source;
        private String status;
        private Long materialReceiptId;
        private String receiptNoSnapshot;
        private Long taskPackageId;
        private Long farmItemId;
        private Long leaderEmployeeId;
        private String returnerNameSnapshot;
        private String taskNameSnapshot;
        private String farmWorkNameSnapshot;
        private String greenhouseNamesSnapshot;
        private String invalidReason;
        private Date businessDate;
        private Long confirmedBy;
        private Date confirmedAt;
        @Version
        private Long version;
        private Long createDept;
        private Long createBy;
        private Date createTime;
        private Long updateBy;
        private Date updateTime;
        private String remark;
    }

    @Data
    @TableName("sf_inventory_return_line")
    public static class ReturnLine {
        @TableId(value = "return_line_id", type = IdType.ASSIGN_ID)
        private Long returnLineId;
        private String tenantId;
        private Long returnOrderId;
        private Long materialReceiptLineId;
        private Long materialId;
        private String materialCodeSnapshot;
        private String materialNameSnapshot;
        private String specificationSnapshot;
        private String unitSnapshot;
        private BigDecimal issuedQuantitySnapshot;
        private BigDecimal returnedBeforeSnapshot;
        private BigDecimal remainingBeforeSnapshot;
        private BigDecimal requestedReturnQuantity;
        private BigDecimal actualReturnQuantity;
        private Boolean deletedByKeeper;
        private String invalidReason;
        @Version
        private Long version;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_inventory_stocktake_order")
    public static class StocktakeOrder {
        @TableId(value = "stocktake_order_id", type = IdType.ASSIGN_ID)
        private Long stocktakeOrderId;
        private String tenantId;
        private String orderNo;
        private String status;
        private Date businessDate;
        @Version
        private Long version;
        private Long createDept;
        private Long createBy;
        private Date createTime;
        private Long updateBy;
        private Date updateTime;
        private String remark;
    }

    @Data
    @TableName("sf_inventory_stocktake_line")
    public static class StocktakeLine {
        @TableId(value = "stocktake_line_id", type = IdType.ASSIGN_ID)
        private Long stocktakeLineId;
        private String tenantId;
        private Long stocktakeOrderId;
        private Long materialId;
        private String materialCodeSnapshot;
        private String materialNameSnapshot;
        private String specificationSnapshot;
        private String unitSnapshot;
        private BigDecimal systemQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal differenceQuantity;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_stask_task_material")
    public static class TaskMaterial {
        @TableId(value = "task_material_id", type = IdType.ASSIGN_ID)
        private Long taskMaterialId;
        private String tenantId;
        private Long taskPackageId;
        private Long farmItemId;
        private Long materialId;
        private String materialCodeSnapshot;
        private String materialNameSnapshot;
        private String specificationSnapshot;
        private String unitSnapshot;
        private BigDecimal totalQuantity;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_stask_material_allocation")
    public static class Allocation {
        @TableId(value = "allocation_id", type = IdType.ASSIGN_ID)
        private Long allocationId;
        private String tenantId;
        private Long taskPackageId;
        private Long farmItemId;
        private Long leaderEmployeeId;
        private String leaderNameSnapshot;
        private Integer greenhouseCount;
        private Integer leaderOrder;
        private String greenhouseNamesSnapshot;
        private String algorithmVersion;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_stask_material_allocation_line")
    public static class AllocationLine {
        @TableId(value = "allocation_line_id", type = IdType.ASSIGN_ID)
        private Long allocationLineId;
        private String tenantId;
        private Long allocationId;
        private Long taskMaterialId;
        private Long materialId;
        private BigDecimal allocatedQuantity;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_stask_material_receipt")
    public static class MaterialReceipt {
        @TableId(value = "material_receipt_id", type = IdType.ASSIGN_ID)
        private Long materialReceiptId;
        private String tenantId;
        private String receiptNo;
        private Long taskPackageId;
        private Long farmItemId;
        private Long leaderEmployeeId;
        private String leaderNameSnapshot;
        private String taskNameSnapshot;
        private String farmWorkNameSnapshot;
        private String greenhouseNamesSnapshot;
        private String status;
        private Boolean arrived;
        /** 最近一次库管取消关联待出库单的理由；重新申请出库时清空。 */
        @TableField(updateStrategy = FieldStrategy.ALWAYS)
        private String keeperCancelReason;
        @Version
        private Long version;
        private Long createBy;
        private Date createTime;
        private Long updateBy;
        private Date updateTime;
    }

    @Data
    @TableName("sf_stask_material_receipt_line")
    public static class MaterialReceiptLine {
        @TableId(value = "material_receipt_line_id", type = IdType.ASSIGN_ID)
        private Long materialReceiptLineId;
        private String tenantId;
        private Long materialReceiptId;
        private Long materialId;
        private String materialCodeSnapshot;
        private String materialNameSnapshot;
        private String specificationSnapshot;
        private String unitSnapshot;
        private BigDecimal requestedQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal returnedQuantity;
        @Version
        private Long version;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_asset_type")
    public static class AssetType {
        @TableId(value = "asset_type_id", type = IdType.ASSIGN_ID)
        private Long assetTypeId;
        private String tenantId;
        private String typeCode;
        private String typeName;
        private Boolean enabled;
        @Version
        private Long version;
        private String delFlag;
        private Long createDept;
        private Long createBy;
        private Date createTime;
        private Long updateBy;
        private Date updateTime;
        private String remark;
    }

    @Data
    @TableName("sf_asset_device")
    public static class AssetDevice {
        @TableId(value = "asset_device_id", type = IdType.ASSIGN_ID)
        private Long assetDeviceId;
        private String tenantId;
        private Long assetTypeId;
        private String deviceNo;
        private String deviceName;
        private String status;
        @TableField(updateStrategy = FieldStrategy.ALWAYS)
        private Long currentHolderEmployeeId;
        @TableField(updateStrategy = FieldStrategy.ALWAYS)
        private String currentHolderNameSnapshot;
        private Date acquiredDate;
        @Version
        private Long version;
        private String delFlag;
        private Long createDept;
        private Long createBy;
        private Date createTime;
        private Long updateBy;
        private Date updateTime;
        private String remark;
    }

    @Data
    @TableName("sf_asset_usage_log")
    public static class AssetUsageLog {
        @TableId(value = "usage_log_id", type = IdType.ASSIGN_ID)
        private Long usageLogId;
        private String tenantId;
        private Long assetDeviceId;
        private String action;
        private String fromStatus;
        private String toStatus;
        private Long holderEmployeeId;
        private String holderNameSnapshot;
        private Long operatorEmployeeId;
        private String operatorNameSnapshot;
        private Date occurredAt;
        private String remark;
        private Date createTime;
    }

    @Data
    @TableName("sf_inventory_operation_log")
    public static class OperationLog {
        @TableId(value = "operation_log_id", type = IdType.ASSIGN_ID)
        private Long operationLogId;
        private String tenantId;
        private String aggregateType;
        private Long aggregateId;
        private String action;
        private String reason;
        private String beforeSnapshot;
        private String afterSnapshot;
        private Long operatorEmployeeId;
        private String operatorNameSnapshot;
        private Date createTime;
    }

    @Data
    @TableName("sf_inventory_business_sequence")
    public static class BusinessSequence {
        @TableId(value = "sequence_id", type = IdType.ASSIGN_ID)
        private Long sequenceId;
        private String tenantId;
        private String businessType;
        private Date sequenceDate;
        private Long currentValue;
        @Version
        private Long version;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_inventory_idempotency")
    public static class Idempotency {
        @TableId(value = "idempotency_id", type = IdType.ASSIGN_ID)
        private Long idempotencyId;
        private String tenantId;
        private Long operatorEmployeeId;
        private String operationType;
        private String idempotencyKey;
        private String requestHash;
        private String operationStatus;
        private String responseJson;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    @TableName("sf_inventory_legacy_material_map")
    public static class LegacyMaterialMap {
        @TableId(value = "map_id", type = IdType.INPUT)
        private Long mapId;
        private String tenantId;
        private Long legacyMaterialId;
        private Long inventoryMaterialId;
        private BigDecimal originalQuantity;
        private BigDecimal roundedQuantity;
        private BigDecimal roundingDelta;
        private String migrationBatch;
        private Date migratedAt;
    }

    private InventoryEntities() {
    }
}
