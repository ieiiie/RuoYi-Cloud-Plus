package com.ym.agriculture.farmtask.inventory.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetDevice;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetType;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetUsageLog;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Balance;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.InboundLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.InboundOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Ledger;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Material;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialCategory;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceipt;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceiptLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OutboundLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OutboundOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.StocktakeLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.StocktakeOrder;

import java.util.ArrayList;
import java.util.List;

/** 从库存和资产持久化实体提取允许进入中维翻译体系的中文业务文本。 */
public final class InventoryI18nSourceFactory {

    private InventoryI18nSourceFactory() {
    }

    public static List<I18nTextSource> sources(MaterialCategory row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.INVENTORY_MATERIAL_CATEGORY, row.getCategoryId(),
            "categoryName", row.getCategoryName());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(Material row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.INVENTORY_MATERIAL, row.getMaterialId(),
            "materialName", row.getMaterialName());
        add(sources, I18nResourceType.INVENTORY_MATERIAL, row.getMaterialId(),
            "specification", row.getSpecification());
        add(sources, I18nResourceType.INVENTORY_MATERIAL, row.getMaterialId(), "unit", row.getUnit());
        add(sources, I18nResourceType.INVENTORY_MATERIAL, row.getMaterialId(), "remark", row.getRemark());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(Balance row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.INVENTORY_MATERIAL, row.getMaterialId(),
            "abnormalReason", row.getAbnormalReason());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(Ledger row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.INVENTORY_LEDGER, row.getLedgerId(),
            "operatorNameSnapshot", row.getOperatorNameSnapshot());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(InboundOrder row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.INVENTORY_INBOUND, row.getInboundOrderId(),
            "supplierName", row.getSupplierName());
        add(sources, I18nResourceType.INVENTORY_INBOUND, row.getInboundOrderId(), "remark", row.getRemark());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(InboundLine row) {
        if (row == null) {
            return List.of();
        }
        return materialSnapshotSources(I18nResourceType.INVENTORY_INBOUND, row.getInboundLineId(),
            row.getMaterialNameSnapshot(), row.getSpecificationSnapshot(), row.getUnitSnapshot());
    }

    public static List<I18nTextSource> sources(OutboundOrder row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.INVENTORY_OUTBOUND, row.getOutboundOrderId(),
            "receiverNameSnapshot", row.getReceiverNameSnapshot());
        add(sources, I18nResourceType.INVENTORY_OUTBOUND, row.getOutboundOrderId(),
            "taskNameSnapshot", row.getTaskNameSnapshot());
        add(sources, I18nResourceType.INVENTORY_OUTBOUND, row.getOutboundOrderId(),
            "farmWorkNameSnapshot", row.getFarmWorkNameSnapshot());
        add(sources, I18nResourceType.INVENTORY_OUTBOUND, row.getOutboundOrderId(),
            "greenhouseNamesSnapshot", row.getGreenhouseNamesSnapshot());
        add(sources, I18nResourceType.INVENTORY_OUTBOUND, row.getOutboundOrderId(), "remark", row.getRemark());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(OutboundLine row) {
        if (row == null) {
            return List.of();
        }
        return materialSnapshotSources(I18nResourceType.INVENTORY_OUTBOUND, row.getOutboundLineId(),
            row.getMaterialNameSnapshot(), row.getSpecificationSnapshot(), row.getUnitSnapshot());
    }

    public static List<I18nTextSource> sources(ReturnOrder row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.INVENTORY_RETURN, row.getReturnOrderId(),
            "returnerNameSnapshot", row.getReturnerNameSnapshot());
        add(sources, I18nResourceType.INVENTORY_RETURN, row.getReturnOrderId(),
            "taskNameSnapshot", row.getTaskNameSnapshot());
        add(sources, I18nResourceType.INVENTORY_RETURN, row.getReturnOrderId(),
            "farmWorkNameSnapshot", row.getFarmWorkNameSnapshot());
        add(sources, I18nResourceType.INVENTORY_RETURN, row.getReturnOrderId(),
            "greenhouseNamesSnapshot", row.getGreenhouseNamesSnapshot());
        add(sources, I18nResourceType.INVENTORY_RETURN, row.getReturnOrderId(),
            "invalidReason", row.getInvalidReason());
        add(sources, I18nResourceType.INVENTORY_RETURN, row.getReturnOrderId(), "remark", row.getRemark());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(ReturnLine row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>(materialSnapshotSources(
            I18nResourceType.INVENTORY_RETURN, row.getReturnLineId(), row.getMaterialNameSnapshot(),
            row.getSpecificationSnapshot(), row.getUnitSnapshot()));
        add(sources, I18nResourceType.INVENTORY_RETURN, row.getReturnLineId(),
            "invalidReason", row.getInvalidReason());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(StocktakeOrder row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.INVENTORY_STOCKTAKE, row.getStocktakeOrderId(),
            "remark", row.getRemark());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(StocktakeLine row) {
        if (row == null) {
            return List.of();
        }
        return materialSnapshotSources(I18nResourceType.INVENTORY_STOCKTAKE, row.getStocktakeLineId(),
            row.getMaterialNameSnapshot(), row.getSpecificationSnapshot(), row.getUnitSnapshot());
    }

    public static List<I18nTextSource> sources(MaterialReceipt row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.INVENTORY_RECEIPT, row.getMaterialReceiptId(),
            "leaderNameSnapshot", row.getLeaderNameSnapshot());
        add(sources, I18nResourceType.INVENTORY_RECEIPT, row.getMaterialReceiptId(),
            "taskNameSnapshot", row.getTaskNameSnapshot());
        add(sources, I18nResourceType.INVENTORY_RECEIPT, row.getMaterialReceiptId(),
            "farmWorkNameSnapshot", row.getFarmWorkNameSnapshot());
        add(sources, I18nResourceType.INVENTORY_RECEIPT, row.getMaterialReceiptId(),
            "greenhouseNamesSnapshot", row.getGreenhouseNamesSnapshot());
        add(sources, I18nResourceType.INVENTORY_RECEIPT, row.getMaterialReceiptId(),
            "keeperCancelReason", row.getKeeperCancelReason());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(MaterialReceiptLine row) {
        if (row == null) {
            return List.of();
        }
        return materialSnapshotSources(I18nResourceType.INVENTORY_RECEIPT,
            row.getMaterialReceiptLineId(), row.getMaterialNameSnapshot(),
            row.getSpecificationSnapshot(), row.getUnitSnapshot());
    }

    public static List<I18nTextSource> sources(AssetType row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.ASSET_TYPE, row.getAssetTypeId(), "typeName", row.getTypeName());
        add(sources, I18nResourceType.ASSET_TYPE, row.getAssetTypeId(), "remark", row.getRemark());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(AssetDevice row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.ASSET_DEVICE, row.getAssetDeviceId(),
            "deviceName", row.getDeviceName());
        add(sources, I18nResourceType.ASSET_DEVICE, row.getAssetDeviceId(),
            "currentHolderNameSnapshot", row.getCurrentHolderNameSnapshot());
        add(sources, I18nResourceType.ASSET_DEVICE, row.getAssetDeviceId(), "remark", row.getRemark());
        return List.copyOf(sources);
    }

    public static List<I18nTextSource> sources(AssetUsageLog row) {
        if (row == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.ASSET_USAGE_LOG, row.getUsageLogId(),
            "holderNameSnapshot", row.getHolderNameSnapshot());
        add(sources, I18nResourceType.ASSET_USAGE_LOG, row.getUsageLogId(),
            "operatorNameSnapshot", row.getOperatorNameSnapshot());
        add(sources, I18nResourceType.ASSET_USAGE_LOG, row.getUsageLogId(), "remark", row.getRemark());
        return List.copyOf(sources);
    }

    private static List<I18nTextSource> materialSnapshotSources(String resourceType, Long resourceId,
        String materialName, String specification, String unit) {
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, resourceType, resourceId, "materialNameSnapshot", materialName);
        add(sources, resourceType, resourceId, "specificationSnapshot", specification);
        add(sources, resourceType, resourceId, "unitSnapshot", unit);
        return List.copyOf(sources);
    }

    private static void add(List<I18nTextSource> sources, String resourceType, Long resourceId,
        String fieldKey, String sourceText) {
        if (resourceId != null && StringUtils.isNotBlank(sourceText)) {
            sources.add(new I18nTextSource(resourceType, resourceId, fieldKey, sourceText));
        }
    }
}
