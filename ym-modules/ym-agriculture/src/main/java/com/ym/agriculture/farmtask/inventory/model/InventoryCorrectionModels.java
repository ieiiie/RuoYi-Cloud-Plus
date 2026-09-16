package com.ym.agriculture.farmtask.inventory.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OutboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Web 后台历史库存单据纠错请求与入库单详情响应。 */
public final class InventoryCorrectionModels {

    /** 历史单据纠错物资行；未提交的原行视为删除。 */
    @Data
    public static class CorrectionLineBo {
        /** 新库存物资档案 ID。 */
        @NotNull(message = "物资ID不能为空")
        private Long inventoryMaterialId;

        /** 纠错后的有效数量，必须为正数且最多一位小数。 */
        @NotNull(message = "数量不能为空")
        private BigDecimal quantity;
    }

    /** 历史纠错的公共字段。 */
    @Data
    public abstract static class CorrectionDocumentBo {
        /** 当前单据版本。 */
        @NotNull(message = "版本不能为空")
        private Long version;

        /** 纠错后的租户内唯一业务单号。 */
        @NotBlank(message = "单据编号不能为空")
        @Size(max = 64, message = "单据编号不能超过64个字符")
        private String orderNo;

        /** 纠错后的业务发生日期。 */
        @NotNull(message = "业务日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date businessDate;

        /** 纠错后的有效明细；同一物资只能一行。 */
        @Valid
        @NotEmpty(message = "单据明细不能为空")
        private List<CorrectionLineBo> lines = new ArrayList<>();

        /** 是否明确确认允许本次纠错产生负库存。 */
        private Boolean allowNegativeCorrection = false;

        /** 负库存纠错原因，可选。 */
        @Size(max = 500, message = "纠错原因不能超过500个字符")
        private String correctionReason;

        /** 单据备注。 */
        @Size(max = 500, message = "备注不能超过500个字符")
        private String remark;
    }

    /** 入库单历史纠错。 */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class InboundCorrectionBo extends CorrectionDocumentBo {
        /** 供应方名称快照，可空。 */
        @Size(max = 128, message = "供应方名称不能超过128个字符")
        private String supplierName;
    }

    /** 已出库单历史纠错。 */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class OutboundCorrectionBo extends CorrectionDocumentBo {
        /** 领用人姓名快照。 */
        @NotBlank(message = "领用人不能为空")
        @Size(max = 100, message = "领用人不能超过100个字符")
        private String receiverName;
    }

    /** 已退库单历史纠错。 */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ReturnCorrectionBo extends CorrectionDocumentBo {
        /** 退库人姓名快照。 */
        @NotBlank(message = "退库人不能为空")
        @Size(max = 100, message = "退库人不能超过100个字符")
        private String returnerName;
    }

    /** 历史单据删除/作废参数。 */
    @Data
    public static class CorrectionDeleteBo {
        /** 当前单据版本。 */
        @NotNull(message = "版本不能为空")
        private Long version;

        /** 是否明确确认允许本次作废回滚产生负库存。 */
        private Boolean allowNegativeCorrection = false;

        /** 负库存作废/纠错原因，可选。 */
        @Size(max = 500, message = "纠错原因不能超过500个字符")
        private String correctionReason;
    }

    /** 入库单详情。 */
    @Data
    public static class InboundVo {
        /** 入库单 ID。 */
        private Long id;
        /** 入库单号。 */
        private String orderNo;
        /** COMPLETED/VOIDED。 */
        private String status;
        /** 当前请求语言的入库状态标签。 */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String statusLabel;
        /** 供应方名称快照。 */
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_INBOUND,
            idProperty = "id", fieldKey = "supplierName")
        private String supplierName;
        /** 入库业务日期，JSON 格式：yyyy-MM-dd。 */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date businessDate;
        /** 乐观锁版本。 */
        private Long version;
        /** 创建时间。 */
        private Date createTime;
        /** 备注。 */
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_INBOUND,
            idProperty = "id", fieldKey = "remark")
        private String remark;
        /** 入库明细。 */
        private List<InboundLineVo> lines = new ArrayList<>();
    }

    /** 入库单详情行。 */
    @Data
    public static class InboundLineVo {
        /** 入库行 ID。 */
        private Long id;
        /** 库存物资 ID。 */
        private Long inventoryMaterialId;
        /** 物资编码快照。 */
        private String materialCode;
        /** 物资名称快照。 */
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_INBOUND,
            idProperty = "id", fieldKey = "materialNameSnapshot")
        private String materialName;
        /** 规格快照。 */
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_INBOUND,
            idProperty = "id", fieldKey = "specificationSnapshot")
        private String specification;
        /** 单位快照。 */
        @StaskI18nField(resourceType = I18nResourceType.INVENTORY_INBOUND,
            idProperty = "id", fieldKey = "unitSnapshot")
        private String unit;
        /** 入库数量。 */
        private BigDecimal quantity;
    }

    /** 入库单历史纠错的库存审计快照记录，只读展示使用。 */
    @Data
    public static class InboundCorrectionLogVo {
        /** 库存操作审计日志 ID。 */
        private Long id;
        /** 固定为 HISTORICAL_CORRECTION，供客户端兼容判断。 */
        private String action;
        /** 本次纠错原因；未填写时可能为纠错后的备注。 */
        private String reason;
        /** 操作人姓名快照。 */
        private String operatorName;
        /** 纠错记录创建时间。 */
        private Date createTime;
        /** 前后快照是否都能被当前版本解析。 */
        private Boolean snapshotAvailable;
        /** 纠错前的入库单历史快照；不可用时为空。 */
        private InboundVo before;
        /** 纠错后的入库单历史快照；不可用时为空。 */
        private InboundVo after;
    }

    /** 已出库单历史纠错的库存审计快照记录，只读展示使用。 */
    @Data
    public static class OutboundCorrectionLogVo {
        /** 库存操作审计日志 ID。 */
        private Long id;
        /** 固定为 HISTORICAL_CORRECTION，供客户端兼容判断。 */
        private String action;
        /** 本次纠错原因；未填写时可能为纠错后的备注。 */
        private String reason;
        /** 操作人姓名快照。 */
        private String operatorName;
        /** 纠错记录创建时间。 */
        private Date createTime;
        /** 前后快照是否都能被当前版本解析。 */
        private Boolean snapshotAvailable;
        /** 纠错前的已出库单历史快照；不可用时为空。 */
        private OutboundVo before;
        /** 纠错后的已出库单历史快照；不可用时为空。 */
        private OutboundVo after;
    }

    /** 已退库单历史纠错的库存审计快照记录，只读展示使用。 */
    @Data
    public static class ReturnCorrectionLogVo {
        /** 库存操作审计日志 ID。 */
        private Long id;
        /** 固定为 HISTORICAL_CORRECTION，供客户端兼容判断。 */
        private String action;
        /** 本次纠错原因；未填写时可能为纠错后的备注。 */
        private String reason;
        /** 操作人姓名快照。 */
        private String operatorName;
        /** 纠错记录创建时间。 */
        private Date createTime;
        /** 前后快照是否都能被当前版本解析。 */
        private Boolean snapshotAvailable;
        /** 纠错前的已退库单历史快照；不可用时为空。 */
        private ReturnVo before;
        /** 纠错后的已退库单历史快照；不可用时为空。 */
        private ReturnVo after;
    }

    /** 退库单操作审计记录，只读展示使用。 */
    @Data
    public static class ReturnOperationLogVo {
        /** 库存操作审计日志 ID。 */
        private Long id;
        /** CREATE/UPDATE/CONFIRM/CREATE_AND_CONFIRM/HISTORICAL_CORRECTION/VOID 等稳定动作编码。 */
        private String action;
        /** 当前请求语言的动作展示标签。 */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String actionLabel;
        /** 操作原因或备注。 */
        private String reason;
        /** 操作人姓名快照。 */
        private String operatorName;
        /** 操作记录创建时间。 */
        private Date createTime;
        /** 至少存在一个可解析的前后快照。 */
        private Boolean snapshotAvailable;
        /** 操作前的退库单历史快照；创建类动作可为空。 */
        private ReturnVo before;
        /** 操作后的退库单历史快照；作废、创建等动作可为空。 */
        private ReturnVo after;
    }

    private InventoryCorrectionModels() {
    }
}
