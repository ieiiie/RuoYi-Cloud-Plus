package com.ym.agriculture.farmtask.inventory.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 库存 Web 与小程序共享的请求、响应模型。 */
public final class InventoryModels {

    @Data
    public static class MaterialQuery {
        private String keyword;
        private Boolean enabled;
        /** NORMAL/ABNORMAL；为空查询全部。 */
        private String status;
        private Long categoryId;
        /** 兼容旧调用方；新调用方使用 categoryId。 */
        private String category;
    }

    @Data
    public static class MaterialCategorySaveBo {
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称不能超过64个字符")
        private String categoryName;
        @Min(value = 0, message = "分类排序不能小于0")
        private Integer sortOrder = 0;
        private Boolean enabled = true;
        private Long version;
        @Size(max = 500, message = "备注不能超过500个字符")
        private String remark;
    }

    @Data
    public static class MaterialCategoryVo {
        private Long id;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL_CATEGORY,
            idProperty = "id", fieldKey = "categoryName")
        private String categoryName;
        private Integer sortOrder;
        private Boolean enabled;
        private Long materialCount;
        private Long version;
        private String remark;
    }

    /** 分类物料数量的批量聚合查询结果，不作为外部接口模型。 */
    @Data
    public static class MaterialCategoryCount {
        private Long categoryId;
        private Long materialCount;
    }

    @Data
    public static class MaterialSaveBo {
        @NotBlank(message = "物资编码不能为空")
        @Size(max = 64, message = "物资编码不能超过64个字符")
        private String materialCode;
        @NotBlank(message = "物资名称不能为空")
        @Size(max = 128, message = "物资名称不能超过128个字符")
        private String materialName;
        /** 新调用方必须提交分类 ID；category 仅用于旧调用方兼容匹配。 */
        private Long categoryId;
        @Size(max = 64, message = "物资分类不能超过64个字符")
        private String category;
        @Size(max = 255, message = "规格型号不能超过255个字符")
        private String specification;
        @NotBlank(message = "计量单位不能为空")
        @Size(max = 32, message = "计量单位不能超过32个字符")
        private String unit;
        private Boolean enabled = true;
        private Long version;
        @Size(max = 500, message = "备注不能超过500个字符")
        private String remark;
    }

    @Data
    public static class MaterialVo {
        private Long id;
        private String materialCode;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL,
            idProperty = "id", fieldKey = "materialName")
        private String materialName;
        private Long categoryId;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL_CATEGORY,
            idProperty = "categoryId", fieldKey = "categoryName")
        private String categoryName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL_CATEGORY,
            idProperty = "categoryId", fieldKey = "categoryName")
        private String category;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL,
            idProperty = "id", fieldKey = "specification")
        private String specification;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL,
            idProperty = "id", fieldKey = "unit")
        private String unit;
        private Boolean enabled;
        private BigDecimal quantity;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL,
            idProperty = "id", fieldKey = "abnormalReason")
        private String abnormalReason;
        private Boolean outboundLocked;
        private Long version;
        private Date createTime;
        private Date updateTime;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL,
            idProperty = "id", fieldKey = "remark")
        private String remark;
    }

    /** 技术员创建任务时使用的启用物资选项。 */
    @Data
    public static class MaterialOptionVo {
        private Long id;
        private String materialCode;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL,
            idProperty = "id", fieldKey = "materialName")
        private String materialName;
        private Long categoryId;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL_CATEGORY,
            idProperty = "categoryId", fieldKey = "categoryName")
        private String categoryName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL_CATEGORY,
            idProperty = "categoryId", fieldKey = "categoryName")
        private String category;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL,
            idProperty = "id", fieldKey = "specification")
        private String specification;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_MATERIAL,
            idProperty = "id", fieldKey = "unit")
        private String unit;
        private BigDecimal availableQuantity;
        private Boolean selectable;
    }

    @Data
    public static class LedgerQuery {
        private String type;
    }

    @Data
    public static class LedgerVo {
        private Long id;
        private String type;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String typeLabel;
        private String businessSubtype;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String businessSubtypeLabel;
        private BigDecimal quantity;
        private BigDecimal balanceAfter;
        private Long relatedOrderId;
        private String relatedOrderNo;
        private String relatedOrderType;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String relatedOrderTypeLabel;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_LEDGER,
            idProperty = "id", fieldKey = "operatorNameSnapshot")
        private String operatorName;
        /** 库存业务发生日期，JSON 格式：yyyy-MM-dd。 */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date occurredAt;
    }

    @Data
    public static class DocumentLineBo {
        @NotNull(message = "物资ID不能为空")
        private Long inventoryMaterialId;
        @NotNull(message = "数量不能为空")
        private BigDecimal quantity;
    }

    @Data
    public static class InboundSaveBo {
        @Size(max = 64, message = "入库单号不能超过64个字符")
        private String orderNo;
        @Size(max = 128, message = "供应方名称不能超过128个字符")
        private String supplierName;
        /** 入库日期，格式：yyyy-MM-dd。 */
        @NotNull(message = "入库日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date businessDate;
        @Size(max = 500, message = "备注不能超过500个字符")
        private String remark;
        @Valid
        @NotEmpty(message = "入库明细不能为空")
        private List<DocumentLineBo> lines = new ArrayList<>();
    }

    @Data
    public static class DirectOutboundSaveBo {
        @Size(max = 64, message = "出库单号不能超过64个字符")
        private String orderNo;
        /** 可选关联领料单 ID；关联时直接完成已到岗待出库的领料单。 */
        private Long materialReceiptId;
        @NotBlank(message = "领用人不能为空")
        private String receiverName;
        /** 出库日期，格式：yyyy-MM-dd。 */
        @NotNull(message = "出库日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date businessDate;
        @Size(max = 500, message = "备注不能超过500个字符")
        private String remark;
        @Valid
        @NotEmpty(message = "出库明细不能为空")
        private List<DocumentLineBo> lines = new ArrayList<>();
    }

    @Data
    public static class DirectReturnSaveBo {
        @Size(max = 64, message = "退库单号不能超过64个字符")
        private String orderNo;
        /** 可选关联领料单 ID；关联时只能退原实发物资且受剩余可退量限制。 */
        private Long materialReceiptId;
        @NotBlank(message = "退库人不能为空")
        private String returnerName;
        /** 退库日期，格式：yyyy-MM-dd。 */
        @NotNull(message = "退库日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date businessDate;
        @Size(max = 500, message = "备注不能超过500个字符")
        private String remark;
        @Valid
        @NotEmpty(message = "退库明细不能为空")
        private List<DocumentLineBo> lines = new ArrayList<>();
    }

    @Data
    public static class StocktakeSaveBo {
        /** 盘点日期，格式：yyyy-MM-dd。 */
        @NotNull(message = "盘点日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date businessDate;
        @Size(max = 500, message = "备注不能超过500个字符")
        private String remark;
        @Valid
        @NotEmpty(message = "盘点明细不能为空")
        private List<StocktakeLineBo> lines = new ArrayList<>();
    }

    @Data
    public static class StocktakeLineBo {
        @NotNull(message = "物资ID不能为空")
        private Long inventoryMaterialId;
        @NotNull(message = "实盘数量不能为空")
        private BigDecimal actualQuantity;
    }

    @Data
    public static class OrderQuery {
        private String keyword;
        private String status;
        private String source;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date beginDate;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date endDate;
    }

    @Data
    public static class PendingLineBo {
        @NotNull(message = "明细ID不能为空")
        private Long id;
        private BigDecimal actualQuantity;
        private Boolean deleted = false;
    }

    @Data
    public static class PendingLinesSaveBo {
        @NotNull(message = "版本不能为空")
        private Long version;
        @Valid
        @NotEmpty(message = "出库明细不能为空")
        private List<PendingLineBo> lines = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class OutboundConfirmBo extends PendingLinesSaveBo {
        @NotBlank(message = "幂等键不能为空")
        @Size(max = 128, message = "幂等键不能超过128个字符")
        private String idempotencyKey;
    }

    @Data
    public static class VersionBo {
        @NotNull(message = "版本不能为空")
        private Long version;
    }

    /** 库管取消待出库单时可记录给组长和技术员的退回原因。 */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CancelOutboundBo extends VersionBo {
        @Size(max = 500, message = "库管取消理由不能超过500个字符")
        private String keeperCancelReason;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ReceiptApplyBo extends VersionBo {
        @NotBlank(message = "幂等键不能为空")
        @Size(max = 128, message = "幂等键不能超过128个字符")
        private String idempotencyKey;
    }

    @Data
    public static class ReturnUpdateBo {
        @NotNull(message = "领料单ID不能为空")
        private Long materialReceiptId;
        @NotNull(message = "版本不能为空")
        private Long version;
        @Size(max = 500, message = "备注不能超过500个字符")
        private String note;
        @Valid
        @NotEmpty(message = "退库明细不能为空")
        private List<ReturnRequestLineBo> lines = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ReturnCreateBo extends ReturnUpdateBo {
        @NotBlank(message = "幂等键不能为空")
        @Size(max = 128, message = "幂等键不能超过128个字符")
        private String idempotencyKey;
    }

    @Data
    public static class ReturnRequestLineBo {
        @NotNull(message = "领料单明细ID不能为空")
        private Long materialReceiptLineId;
        @NotNull(message = "申请退库数量不能为空")
        private BigDecimal requestedReturnQuantity;
    }

    @Data
    public static class ReturnConfirmBo {
        @NotNull(message = "版本不能为空")
        private Long version;
        @NotBlank(message = "幂等键不能为空")
        @Size(max = 128, message = "幂等键不能超过128个字符")
        private String idempotencyKey;
        @Valid
        @NotEmpty(message = "退库明细不能为空")
        private List<ReturnConfirmLineBo> lines = new ArrayList<>();
    }

    @Data
    public static class ReturnConfirmLineBo {
        @NotNull(message = "退库明细ID不能为空")
        private Long id;
        private BigDecimal actualReturnQuantity;
        private Boolean deletedByKeeper = false;
    }

    @Data
    public static class MutationVo {
        private Long id;
        private Long version;
        private String status;
        /** 当前请求语言的状态展示文案；业务判断继续使用 status。 */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String statusLabel;

        public MutationVo() {
        }

        public MutationVo(Long id, Long version, String status) {
            this.id = id;
            this.version = version;
            this.status = status;
        }
    }

    @Data
    public static class WorkbenchVo {
        private long pendingOutboundCount;
        private long pendingReturnCount;
        private long abnormalInventoryCount;
        private long enabledMaterialCount;
        private long assetCount;
    }

    /** Web 库存页面顶部统计；无权限的字段保持 null 并从响应中省略。 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InventorySummaryVo {
        /** 当前租户待确认出库单数量。 */
        private Long pendingOutboundCount;
        /** 当前租户待确认退库单数量。 */
        private Long pendingReturnCount;
        /** 当前租户已锁定出库的异常库存数量。 */
        private Long abnormalInventoryCount;
        /** 当前租户启用物资数量。 */
        private Long enabledMaterialCount;
        /** 当前租户未删除资产设备数量。 */
        private Long assetCount;
    }

    @Data
    public static class StocktakeVo {
        private Long id;
        private String orderNo;
        private String status;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String statusLabel;
        /** 盘点日期，JSON 格式：yyyy-MM-dd。 */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date businessDate;
        private Long version;
        private Date createTime;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_STOCKTAKE,
            idProperty = "id", fieldKey = "remark")
        private String remark;
    }

    /** 已完成盘点的只读单据头与明细快照。 */
    @Data
    public static class StocktakeDetailVo {
        /** 盘点单 ID。 */
        private Long id;
        /** 盘点单号。 */
        private String orderNo;
        /** 盘点单状态。 */
        private String status;
        /** 当前请求语言的盘点状态标签。 */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String statusLabel;
        /** 盘点日期，JSON 格式：yyyy-MM-dd。 */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date businessDate;
        /** 当前单据版本。 */
        private Long version;
        /** 创建时间。 */
        private Date createTime;
        /** 盘点备注。 */
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_STOCKTAKE,
            idProperty = "id", fieldKey = "remark")
        private String remark;
        /** 盘点时保存的物资明细快照。 */
        private List<StocktakeDetailLineVo> lines = new ArrayList<>();
    }

    /** 已完成盘点的只读物资明细快照。 */
    @Data
    public static class StocktakeDetailLineVo {
        /** 盘点明细 ID。 */
        private Long id;
        /** 盘点时持久化的库存物资 ID。 */
        private Long inventoryMaterialId;
        /** 盘点时保存的物资编码。 */
        private String materialCode;
        /** 盘点时保存的物资名称。 */
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_STOCKTAKE,
            idProperty = "id", fieldKey = "materialNameSnapshot")
        private String materialName;
        /** 盘点时保存的规格型号。 */
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_STOCKTAKE,
            idProperty = "id", fieldKey = "specificationSnapshot")
        private String specification;
        /** 盘点时保存的计量单位。 */
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_STOCKTAKE,
            idProperty = "id", fieldKey = "unitSnapshot")
        private String unit;
        /** 盘点提交前的系统数量。 */
        private BigDecimal systemQuantity;
        /** 用户录入的实际数量。 */
        private BigDecimal actualQuantity;
        /** 实际数量减系统数量的差异。 */
        private BigDecimal differenceQuantity;
    }

    @Data
    public static class OutboundVo {
        private Long id;
        private String orderNo;
        private String source;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String sourceLabel;
        private String status;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String statusLabel;
        private Long materialReceiptId;
        /** 当前租户关联领料单号；未关联或历史关联已不可读取时为空。 */
        private String receiptNo;
        private Long taskPackageId;
        private Long farmItemId;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_OUTBOUND,
            idProperty = "id", fieldKey = "receiverNameSnapshot")
        private String receiverName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_OUTBOUND,
            idProperty = "id", fieldKey = "taskNameSnapshot")
        private String taskName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_OUTBOUND,
            idProperty = "id", fieldKey = "farmWorkNameSnapshot")
        private String farmWorkName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_OUTBOUND,
            idProperty = "id", fieldKey = "greenhouseNamesSnapshot")
        private String greenhouseNames;
        /** 出库日期，JSON 格式：yyyy-MM-dd。 */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date businessDate;
        private Long version;
        private Date createTime;
        private Date confirmedAt;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_OUTBOUND,
            idProperty = "id", fieldKey = "remark")
        private String remark;
        private List<OutboundLineVo> lines = new ArrayList<>();
    }

    @Data
    public static class OutboundLineVo {
        private Long id;
        private Long materialReceiptLineId;
        private Long inventoryMaterialId;
        private String materialCode;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_OUTBOUND,
            idProperty = "id", fieldKey = "materialNameSnapshot")
        private String materialName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_OUTBOUND,
            idProperty = "id", fieldKey = "specificationSnapshot")
        private String specification;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_OUTBOUND,
            idProperty = "id", fieldKey = "unitSnapshot")
        private String unit;
        private BigDecimal requestedQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal availableQuantity;
        private Boolean deletedByKeeper;
    }

    @Data
    public static class ReceiptVo {
        private Long id;
        private String receiptNo;
        private Long taskPackageId;
        private Long farmItemId;
        private Long leaderEmployeeId;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RECEIPT,
            idProperty = "id", fieldKey = "leaderNameSnapshot")
        private String leaderName;
        /** 关联任务包最终经手技术员员工 ID，JavaScript 调用方按字符串处理。 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private Long handlerTechnicianEmployeeId;
        /** 关联任务包保存的最终经手技术员姓名快照。 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RECEIPT,
            idProperty = "id", fieldKey = "handlerTechnicianEmployeeName")
        private String handlerTechnicianEmployeeName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RECEIPT,
            idProperty = "id", fieldKey = "taskNameSnapshot")
        private String taskName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RECEIPT,
            idProperty = "id", fieldKey = "farmWorkNameSnapshot")
        private String farmWorkName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RECEIPT,
            idProperty = "id", fieldKey = "greenhouseNamesSnapshot")
        private String greenhouseNames;
        /** 最近一次库管取消关联待出库单的理由；未取消或重新申请后为 null。 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RECEIPT,
            idProperty = "id", fieldKey = "keeperCancelReason")
        private String keeperCancelReason;
        private String status;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String statusLabel;
        private Boolean arrived;
        private Long version;
        private Date createTime;
        private List<ReceiptLineVo> lines = new ArrayList<>();
    }

    @Data
    public static class ReceiptLineVo {
        private Long id;
        private Long inventoryMaterialId;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RECEIPT,
            idProperty = "id", fieldKey = "materialNameSnapshot")
        private String materialName;
        private String materialCode;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RECEIPT,
            idProperty = "id", fieldKey = "specificationSnapshot")
        private String specification;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RECEIPT,
            idProperty = "id", fieldKey = "unitSnapshot")
        private String unit;
        private BigDecimal requestedQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal returnedQuantity;
        private BigDecimal remainingReturnableQuantity;
        private Boolean enabled;
    }

    @Data
    public static class ReturnVo {
        private Long id;
        private String orderNo;
        private String source;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String sourceLabel;
        private String status;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String statusLabel;
        private Long materialReceiptId;
        private String receiptNo;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RETURN,
            idProperty = "id", fieldKey = "taskNameSnapshot")
        private String taskName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RETURN,
            idProperty = "id", fieldKey = "farmWorkNameSnapshot")
        private String farmWorkName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RETURN,
            idProperty = "id", fieldKey = "greenhouseNamesSnapshot")
        private String greenhouseNames;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RETURN,
            idProperty = "id", fieldKey = "returnerNameSnapshot")
        private String returnerName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RETURN,
            idProperty = "id", fieldKey = "invalidReason")
        private String invalidReason;
        /** 退库日期，JSON 格式：yyyy-MM-dd。 */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date businessDate;
        private Long version;
        private Date createTime;
        private Date confirmedAt;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RETURN,
            idProperty = "id", fieldKey = "remark")
        private String remark;
        private List<ReturnLineVo> lines = new ArrayList<>();
    }

    @Data
    public static class ReturnLineVo {
        private Long id;
        private Long materialReceiptLineId;
        private Long inventoryMaterialId;
        private String materialCode;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RETURN,
            idProperty = "id", fieldKey = "materialNameSnapshot")
        private String materialName;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RETURN,
            idProperty = "id", fieldKey = "specificationSnapshot")
        private String specification;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RETURN,
            idProperty = "id", fieldKey = "unitSnapshot")
        private String unit;
        private BigDecimal issuedQuantity;
        private BigDecimal returnedQuantity;
        private BigDecimal remainingReturnableQuantity;
        private BigDecimal requestedReturnQuantity;
        private BigDecimal actualReturnQuantity;
        private Boolean deletedByKeeper;
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_RETURN,
            idProperty = "id", fieldKey = "invalidReason")
        private String invalidReason;
    }

    @Data
    public static class MiniappPageResponse<T> {
        private List<T> items;
        private long total;

        public MiniappPageResponse() {
        }

        public MiniappPageResponse(List<T> items, long total) {
            this.items = items == null ? List.of() : items;
            this.total = total;
        }
    }

    @Data
    public static class AllocationPreviewBo {
        @Valid
        @NotEmpty(message = "物资不能为空")
        private List<AllocationMaterialBo> materials = new ArrayList<>();
        @Valid
        @NotEmpty(message = "组长不能为空")
        private List<AllocationLeaderBo> leaders = new ArrayList<>();
    }

    @Data
    public static class AllocationMaterialBo {
        @NotNull(message = "物资ID不能为空")
        private Long inventoryMaterialId;
        @NotNull(message = "物资数量不能为空")
        private BigDecimal quantity;
    }

    @Data
    public static class AllocationLeaderBo {
        @NotNull(message = "组长ID不能为空")
        private Long leaderEmployeeId;
        @NotBlank(message = "组长姓名不能为空")
        private String leaderName;
        @NotNull(message = "大棚数不能为空")
        private Integer greenhouseCount;
        @NotNull(message = "名单顺序不能为空")
        private Integer order;
        private List<String> greenhouseNames = new ArrayList<>();
    }

    @Data
    public static class AssetTypeSaveBo {
        @NotBlank(message = "资产种类编码不能为空")
        private String typeCode;
        @NotBlank(message = "资产种类名称不能为空")
        private String typeName;
        private Boolean enabled = true;
        private Long version;
        private String remark;
    }

    @Data
    public static class AssetDeviceBatchBo {
        @NotNull(message = "资产种类不能为空")
        private Long assetTypeId;
        @NotBlank(message = "设备编号前缀不能为空")
        private String deviceNoPrefix;
        @NotBlank(message = "设备名称不能为空")
        private String deviceName;
        @NotNull(message = "新增数量不能为空")
        private Integer quantity;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date acquiredDate;
        private String remark;
    }

    @Data
    public static class AssetQuery {
        private String keyword;
        private Long assetTypeId;
        private String status;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AssetActionBo extends VersionBo {
        private Long holderEmployeeId;
        private String holderName;
        private String targetStatus;
        @Size(max = 500, message = "说明不能超过500个字符")
        private String remark;
    }

    @Data
    public static class AssetTypeVo {
        private Long id;
        private String typeCode;
        @StaskI18nField(resourceType = I18nResourceType.ASSET_TYPE,
            idProperty = "id", fieldKey = "typeName")
        private String typeName;
        private Boolean enabled;
        private Long version;
        private long totalCount;
        private long idleCount;
        private long inUseCount;
        private long maintenanceCount;
        private long scrappedCount;
    }

    @Data
    public static class AssetDeviceVo {
        private Long id;
        private Long assetTypeId;
        @StaskI18nField(resourceType = I18nResourceType.ASSET_TYPE,
            idProperty = "assetTypeId", fieldKey = "typeName")
        private String assetTypeName;
        private String deviceNo;
        @StaskI18nField(resourceType = I18nResourceType.ASSET_DEVICE,
            idProperty = "id", fieldKey = "deviceName")
        private String deviceName;
        private String status;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String statusLabel;
        private Long currentHolderEmployeeId;
        @StaskI18nField(resourceType = I18nResourceType.ASSET_DEVICE,
            idProperty = "id", fieldKey = "currentHolderNameSnapshot")
        private String currentHolderName;
        /** 购置日期，JSON 格式：yyyy-MM-dd。 */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date acquiredDate;
        private Long version;
        @StaskI18nField(resourceType = I18nResourceType.ASSET_DEVICE,
            idProperty = "id", fieldKey = "remark")
        private String remark;
        private List<AssetTimelineVo> timeline = new ArrayList<>();
    }

    @Data
    public static class AssetTimelineVo {
        private Long id;
        private String action;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String actionLabel;
        private String fromStatus;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String fromStatusLabel;
        private String toStatus;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String toStatusLabel;
        private Long holderEmployeeId;
        @StaskI18nField(resourceType = I18nResourceType.ASSET_USAGE_LOG,
            idProperty = "id", fieldKey = "holderNameSnapshot")
        private String holderName;
        @StaskI18nField(resourceType = I18nResourceType.ASSET_USAGE_LOG,
            idProperty = "id", fieldKey = "operatorNameSnapshot")
        private String operatorName;
        private Date occurredAt;
        @StaskI18nField(resourceType = I18nResourceType.ASSET_USAGE_LOG,
            idProperty = "id", fieldKey = "remark")
        private String remark;
    }

    private InventoryModels() {
    }
}
