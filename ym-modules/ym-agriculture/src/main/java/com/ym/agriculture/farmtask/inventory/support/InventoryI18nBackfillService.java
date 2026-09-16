package com.ym.agriculture.farmtask.inventory.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.StaskI18nScanResult;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AssetDeviceMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AssetTypeMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AssetUsageLogMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.BalanceMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.InboundOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.LedgerMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialCategoryMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.OutboundOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.ReturnOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.StocktakeOrderMapper;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetDevice;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetType;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetUsageLog;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Balance;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.InboundOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Ledger;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Material;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialCategory;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceipt;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceiptLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OutboundOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.StocktakeOrder;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 按主键游标批量扫描库存和资产历史业务文本。 */
@Service
@RequiredArgsConstructor
public class InventoryI18nBackfillService {

    static final int BATCH_SIZE = 200;

    public static final Set<String> RESOURCE_TYPES = Set.of(
        I18nResourceType.INVENTORY_MATERIAL_CATEGORY,
        I18nResourceType.INVENTORY_MATERIAL,
        I18nResourceType.INVENTORY_LEDGER,
        I18nResourceType.INVENTORY_INBOUND,
        I18nResourceType.INVENTORY_OUTBOUND,
        I18nResourceType.INVENTORY_RETURN,
        I18nResourceType.INVENTORY_STOCKTAKE,
        I18nResourceType.INVENTORY_RECEIPT,
        I18nResourceType.ASSET_TYPE,
        I18nResourceType.ASSET_DEVICE,
        I18nResourceType.ASSET_USAGE_LOG
    );

    private final InventoryI18nResourceRegistrar registrar;
    private final MaterialCategoryMapper materialCategoryMapper;
    private final MaterialMapper materialMapper;
    private final BalanceMapper balanceMapper;
    private final LedgerMapper ledgerMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final StocktakeOrderMapper stocktakeOrderMapper;
    private final MaterialReceiptMapper receiptMapper;
    private final MaterialReceiptLineMapper receiptLineMapper;
    private final AssetTypeMapper assetTypeMapper;
    private final AssetDeviceMapper assetDeviceMapper;
    private final AssetUsageLogMapper usageLogMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final PlatformTransactionManager transactionManager;

    /**
     * 扫描选定库存资源；空集合表示全部库存与资产资源。
     *
     * @param tenantId 当前租户
     * @param resourceTypes 资源类型
     * @return 扫描资源和中文文本统计
     */
    public StaskI18nScanResult backfill(String tenantId, Collection<String> resourceTypes) {
        Set<String> selected = resourceTypes == null || resourceTypes.isEmpty()
            ? RESOURCE_TYPES : resourceTypes.stream().filter(RESOURCE_TYPES::contains).collect(Collectors.toSet());
        if (selected.isEmpty()) {
            return StaskI18nScanResult.empty();
        }
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        if (selected.contains(I18nResourceType.INVENTORY_MATERIAL_CATEGORY)) {
            summary = summary.merge(backfillMaterialCategories(tenantId,
                Set.of(I18nResourceType.INVENTORY_MATERIAL_CATEGORY)));
        }
        if (selected.contains(I18nResourceType.INVENTORY_MATERIAL)) {
            summary = summary.merge(backfillMaterials(tenantId,
                Set.of(I18nResourceType.INVENTORY_MATERIAL)));
        }
        if (selected.contains(I18nResourceType.INVENTORY_LEDGER)) {
            summary = summary.merge(backfillLedgers(tenantId,
                Set.of(I18nResourceType.INVENTORY_LEDGER)));
        }
        if (selected.contains(I18nResourceType.INVENTORY_INBOUND)) {
            summary = summary.merge(backfillInbound(tenantId,
                Set.of(I18nResourceType.INVENTORY_INBOUND)));
        }
        if (selected.contains(I18nResourceType.INVENTORY_OUTBOUND)) {
            summary = summary.merge(backfillOutbound(tenantId,
                Set.of(I18nResourceType.INVENTORY_OUTBOUND)));
        }
        if (selected.contains(I18nResourceType.INVENTORY_RETURN)) {
            summary = summary.merge(backfillReturns(tenantId,
                Set.of(I18nResourceType.INVENTORY_RETURN)));
        }
        if (selected.contains(I18nResourceType.INVENTORY_STOCKTAKE)) {
            summary = summary.merge(backfillStocktakes(tenantId,
                Set.of(I18nResourceType.INVENTORY_STOCKTAKE)));
        }
        if (selected.contains(I18nResourceType.INVENTORY_RECEIPT)) {
            summary = summary.merge(backfillReceipts(tenantId,
                Set.of(I18nResourceType.INVENTORY_RECEIPT)));
        }
        if (selected.contains(I18nResourceType.ASSET_TYPE)) {
            summary = summary.merge(backfillAssetTypes(tenantId,
                Set.of(I18nResourceType.ASSET_TYPE)));
        }
        if (selected.contains(I18nResourceType.ASSET_DEVICE)
            || selected.contains(I18nResourceType.ASSET_USAGE_LOG)) {
            summary = summary.merge(backfillAssetDevices(tenantId,
                selected.stream().filter(type -> I18nResourceType.ASSET_DEVICE.equals(type)
                        || I18nResourceType.ASSET_USAGE_LOG.equals(type))
                    .collect(Collectors.toSet())));
        }
        return summary;
    }

    private StaskI18nScanResult backfillMaterialCategories(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<MaterialCategory> rows = materialCategoryMapper.selectList(
                Wrappers.<MaterialCategory>lambdaQuery()
                    .eq(MaterialCategory::getTenantId, tenantId)
                    .gt(MaterialCategory::getCategoryId, cursor)
                    .orderByAsc(MaterialCategory::getCategoryId)
                    .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            summary = summary.merge(register(tenantId, rows.size(), rows, selected));
            cursor = rows.get(rows.size() - 1).getCategoryId();
        }
    }

    private StaskI18nScanResult backfillMaterials(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<Material> rows = materialMapper.selectList(Wrappers.<Material>lambdaQuery()
                .eq(Material::getTenantId, tenantId).eq(Material::getDelFlag, "0")
                .gt(Material::getMaterialId, cursor).orderByAsc(Material::getMaterialId)
                .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            List<Long> ids = rows.stream().map(Material::getMaterialId).toList();
            List<Object> entities = new ArrayList<>(rows);
            entities.addAll(balanceMapper.selectList(Wrappers.<Balance>lambdaQuery()
                .eq(Balance::getTenantId, tenantId).in(Balance::getMaterialId, ids)));
            summary = summary.merge(register(tenantId, rows.size(), entities, selected));
            cursor = ids.get(ids.size() - 1);
        }
    }

    private StaskI18nScanResult backfillLedgers(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<Ledger> rows = ledgerMapper.selectList(Wrappers.<Ledger>lambdaQuery()
                .eq(Ledger::getTenantId, tenantId).eq(Ledger::getEffective, true)
                .gt(Ledger::getLedgerId, cursor).orderByAsc(Ledger::getLedgerId)
                .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            summary = summary.merge(register(tenantId, rows.size(), rows, selected));
            cursor = rows.get(rows.size() - 1).getLedgerId();
        }
    }

    private StaskI18nScanResult backfillInbound(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<InboundOrder> rows = inboundOrderMapper.selectList(Wrappers.<InboundOrder>lambdaQuery()
                .eq(InboundOrder::getTenantId, tenantId).gt(InboundOrder::getInboundOrderId, cursor)
                .orderByAsc(InboundOrder::getInboundOrderId).last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            List<Long> ids = rows.stream().map(InboundOrder::getInboundOrderId).toList();
            List<I18nTextSource> sources = inBatchTransaction(
                () -> registrar.registerInboundOrders(tenantId, ids, selected));
            summary = summary.merge(StaskI18nScanResult.of(rows.size(), sources));
            cursor = ids.get(ids.size() - 1);
        }
    }

    private StaskI18nScanResult backfillOutbound(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<OutboundOrder> rows = outboundOrderMapper.selectList(Wrappers.<OutboundOrder>lambdaQuery()
                .eq(OutboundOrder::getTenantId, tenantId).gt(OutboundOrder::getOutboundOrderId, cursor)
                .orderByAsc(OutboundOrder::getOutboundOrderId).last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            List<Long> ids = rows.stream().map(OutboundOrder::getOutboundOrderId).toList();
            List<I18nTextSource> sources = inBatchTransaction(
                () -> registrar.registerOutboundOrders(tenantId, ids, selected));
            summary = summary.merge(StaskI18nScanResult.of(rows.size(), sources));
            cursor = ids.get(ids.size() - 1);
        }
    }

    private StaskI18nScanResult backfillReturns(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<ReturnOrder> rows = returnOrderMapper.selectList(Wrappers.<ReturnOrder>lambdaQuery()
                .eq(ReturnOrder::getTenantId, tenantId).gt(ReturnOrder::getReturnOrderId, cursor)
                .orderByAsc(ReturnOrder::getReturnOrderId).last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            List<Long> ids = rows.stream().map(ReturnOrder::getReturnOrderId).toList();
            List<I18nTextSource> sources = inBatchTransaction(
                () -> registrar.registerReturnOrders(tenantId, ids, selected));
            summary = summary.merge(StaskI18nScanResult.of(rows.size(), sources));
            cursor = ids.get(ids.size() - 1);
        }
    }

    private StaskI18nScanResult backfillStocktakes(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<StocktakeOrder> rows = stocktakeOrderMapper.selectList(Wrappers.<StocktakeOrder>lambdaQuery()
                .eq(StocktakeOrder::getTenantId, tenantId).gt(StocktakeOrder::getStocktakeOrderId, cursor)
                .orderByAsc(StocktakeOrder::getStocktakeOrderId).last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            List<Long> ids = rows.stream().map(StocktakeOrder::getStocktakeOrderId).toList();
            List<I18nTextSource> sources = inBatchTransaction(
                () -> registrar.registerStocktakeOrders(tenantId, ids, selected));
            summary = summary.merge(StaskI18nScanResult.of(rows.size(), sources));
            cursor = ids.get(ids.size() - 1);
        }
    }

    private StaskI18nScanResult backfillReceipts(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<MaterialReceipt> rows = receiptMapper.selectList(Wrappers.<MaterialReceipt>lambdaQuery()
                .eq(MaterialReceipt::getTenantId, tenantId).gt(MaterialReceipt::getMaterialReceiptId, cursor)
                .orderByAsc(MaterialReceipt::getMaterialReceiptId).last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            List<Long> ids = rows.stream().map(MaterialReceipt::getMaterialReceiptId).toList();
            List<MaterialReceiptLine> lines = receiptLineMapper.selectList(
                Wrappers.<MaterialReceiptLine>lambdaQuery()
                    .eq(MaterialReceiptLine::getTenantId, tenantId)
                    .in(MaterialReceiptLine::getMaterialReceiptId, ids));
            Map<Long, SfStaskTaskPackage> packages = loadPackages(tenantId, rows);
            List<I18nTextSource> sources = inBatchTransaction(() -> registrar.registerReceiptEntities(
                tenantId, rows, lines, packages, selected));
            summary = summary.merge(StaskI18nScanResult.of(rows.size(), sources));
            cursor = ids.get(ids.size() - 1);
        }
    }

    private StaskI18nScanResult backfillAssetTypes(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<AssetType> rows = assetTypeMapper.selectList(Wrappers.<AssetType>lambdaQuery()
                .eq(AssetType::getTenantId, tenantId).eq(AssetType::getDelFlag, "0")
                .gt(AssetType::getAssetTypeId, cursor).orderByAsc(AssetType::getAssetTypeId)
                .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            summary = summary.merge(register(tenantId, rows.size(), rows, selected));
            cursor = rows.get(rows.size() - 1).getAssetTypeId();
        }
    }

    private StaskI18nScanResult backfillAssetDevices(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<AssetDevice> rows = assetDeviceMapper.selectList(Wrappers.<AssetDevice>lambdaQuery()
                .eq(AssetDevice::getTenantId, tenantId).eq(AssetDevice::getDelFlag, "0")
                .gt(AssetDevice::getAssetDeviceId, cursor).orderByAsc(AssetDevice::getAssetDeviceId)
                .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            List<Long> ids = rows.stream().map(AssetDevice::getAssetDeviceId).toList();
            List<Object> entities = new ArrayList<>(rows);
            entities.addAll(usageLogMapper.selectList(Wrappers.<AssetUsageLog>lambdaQuery()
                .eq(AssetUsageLog::getTenantId, tenantId)
                .in(AssetUsageLog::getAssetDeviceId, ids)));
            summary = summary.merge(register(tenantId, rows.size(), entities, selected));
            cursor = ids.get(ids.size() - 1);
        }
    }

    private Map<Long, SfStaskTaskPackage> loadPackages(String tenantId,
        Collection<MaterialReceipt> receipts) {
        List<Long> packageIds = receipts.stream().map(MaterialReceipt::getTaskPackageId)
            .filter(Objects::nonNull).distinct().toList();
        if (packageIds.isEmpty()) {
            return Map.of();
        }
        return taskPackageMapper.selectList(Wrappers.<SfStaskTaskPackage>lambdaQuery()
                .eq(SfStaskTaskPackage::getTenantId, tenantId)
                .in(SfStaskTaskPackage::getPackageId, packageIds))
            .stream().collect(Collectors.toMap(SfStaskTaskPackage::getPackageId,
                Function.identity(), (left, right) -> left));
    }

    private StaskI18nScanResult register(String tenantId, int resourceCount,
        Collection<?> entities, Collection<String> selected) {
        List<I18nTextSource> sources = inBatchTransaction(
            () -> registrar.registerEntities(tenantId, entities, selected));
        return StaskI18nScanResult.of(resourceCount, sources);
    }

    private <T> T inBatchTransaction(java.util.function.Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> action.get());
    }
}
