package com.ym.agriculture.farmtask.inventory.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AssetDeviceMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AssetTypeMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AssetUsageLogMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.BalanceMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.InboundLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.InboundOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.LedgerMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialCategoryMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.OutboundLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.OutboundOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.ReturnLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.ReturnOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.StocktakeLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.StocktakeOrderMapper;
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
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存和资产中文业务文本的批量翻译资源登记器。
 *
 * <p>业务写入在同一事务内调用聚合登记方法；历史回填可直接传入已批量读取的实体，
 * 两种路径都只调用一次翻译服务。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryI18nResourceRegistrar {

    private final ISfI18nTextService i18nTextService;
    private final MaterialCategoryMapper materialCategoryMapper;
    private final MaterialMapper materialMapper;
    private final BalanceMapper balanceMapper;
    private final LedgerMapper ledgerMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundLineMapper inboundLineMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundLineMapper outboundLineMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final ReturnLineMapper returnLineMapper;
    private final StocktakeOrderMapper stocktakeOrderMapper;
    private final StocktakeLineMapper stocktakeLineMapper;
    private final MaterialReceiptMapper receiptMapper;
    private final MaterialReceiptLineMapper receiptLineMapper;
    private final AssetTypeMapper assetTypeMapper;
    private final AssetDeviceMapper assetDeviceMapper;
    private final AssetUsageLogMapper assetUsageLogMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;

    /** 按分类 ID 批量登记分类名称。 */
    public List<I18nTextSource> registerMaterialCategories(String tenantId, Collection<Long> categoryIds) {
        List<Long> ids = ids(categoryIds);
        if (StringUtils.isBlank(tenantId) || ids.isEmpty()) {
            return List.of();
        }
        return registerEntities(tenantId, materialCategoryMapper.selectList(
            Wrappers.<MaterialCategory>lambdaQuery()
                .eq(MaterialCategory::getTenantId, tenantId)
                .in(MaterialCategory::getCategoryId, ids)));
    }

    /** 按物资 ID 批量登记物资档案和余额异常说明。 */
    public List<I18nTextSource> registerMaterials(String tenantId, Collection<Long> materialIds) {
        List<Long> ids = ids(materialIds);
        if (StringUtils.isBlank(tenantId) || ids.isEmpty()) {
            return List.of();
        }
        List<Object> resources = new ArrayList<>();
        resources.addAll(materialMapper.selectList(Wrappers.<Material>lambdaQuery()
            .eq(Material::getTenantId, tenantId)
            .eq(Material::getDelFlag, "0")
            .in(Material::getMaterialId, ids)));
        resources.addAll(balanceMapper.selectList(Wrappers.<Balance>lambdaQuery()
            .eq(Balance::getTenantId, tenantId)
            .in(Balance::getMaterialId, ids)));
        return registerEntities(tenantId, resources);
    }

    /** 按入库单 ID 批量登记单据、明细和关联流水。 */
    public List<I18nTextSource> registerInboundOrders(String tenantId, Collection<Long> orderIds) {
        return registerInboundOrders(tenantId, orderIds, List.of());
    }

    /** 按指定资源类型登记入库单、明细和关联流水。 */
    public List<I18nTextSource> registerInboundOrders(String tenantId, Collection<Long> orderIds,
        Collection<String> resourceTypes) {
        List<Long> ids = ids(orderIds);
        if (StringUtils.isBlank(tenantId) || ids.isEmpty()) {
            return List.of();
        }
        List<Object> resources = new ArrayList<>();
        resources.addAll(inboundOrderMapper.selectList(Wrappers.<InboundOrder>lambdaQuery()
            .eq(InboundOrder::getTenantId, tenantId).in(InboundOrder::getInboundOrderId, ids)));
        List<InboundLine> lines = inboundLineMapper.selectList(Wrappers.<InboundLine>lambdaQuery()
            .eq(InboundLine::getTenantId, tenantId).in(InboundLine::getInboundOrderId, ids));
        resources.addAll(lines);
        appendMaterialResources(tenantId, resources,
            lines.stream().map(InboundLine::getMaterialId).toList());
        resources.addAll(documentLedgers(tenantId, "INBOUND", ids));
        return registerEntities(tenantId, resources, resourceTypes);
    }

    /** 按出库单 ID 批量登记单据、明细和关联流水。 */
    public List<I18nTextSource> registerOutboundOrders(String tenantId, Collection<Long> orderIds) {
        return registerOutboundOrders(tenantId, orderIds, List.of());
    }

    /** 按指定资源类型登记出库单、明细和关联流水。 */
    public List<I18nTextSource> registerOutboundOrders(String tenantId, Collection<Long> orderIds,
        Collection<String> resourceTypes) {
        List<Long> ids = ids(orderIds);
        if (StringUtils.isBlank(tenantId) || ids.isEmpty()) {
            return List.of();
        }
        List<Object> resources = new ArrayList<>();
        resources.addAll(outboundOrderMapper.selectList(Wrappers.<OutboundOrder>lambdaQuery()
            .eq(OutboundOrder::getTenantId, tenantId).in(OutboundOrder::getOutboundOrderId, ids)));
        List<OutboundLine> lines = outboundLineMapper.selectList(Wrappers.<OutboundLine>lambdaQuery()
            .eq(OutboundLine::getTenantId, tenantId).in(OutboundLine::getOutboundOrderId, ids));
        resources.addAll(lines);
        appendMaterialResources(tenantId, resources,
            lines.stream().map(OutboundLine::getMaterialId).toList());
        resources.addAll(documentLedgers(tenantId, "OUTBOUND", ids));
        return registerEntities(tenantId, resources, resourceTypes);
    }

    /** 按退库单 ID 批量登记单据、明细和关联流水。 */
    public List<I18nTextSource> registerReturnOrders(String tenantId, Collection<Long> orderIds) {
        return registerReturnOrders(tenantId, orderIds, List.of());
    }

    /** 按指定资源类型登记退库单、明细和关联流水。 */
    public List<I18nTextSource> registerReturnOrders(String tenantId, Collection<Long> orderIds,
        Collection<String> resourceTypes) {
        List<Long> ids = ids(orderIds);
        if (StringUtils.isBlank(tenantId) || ids.isEmpty()) {
            return List.of();
        }
        List<Object> resources = new ArrayList<>();
        resources.addAll(returnOrderMapper.selectList(Wrappers.<ReturnOrder>lambdaQuery()
            .eq(ReturnOrder::getTenantId, tenantId).in(ReturnOrder::getReturnOrderId, ids)));
        List<ReturnLine> lines = returnLineMapper.selectList(Wrappers.<ReturnLine>lambdaQuery()
            .eq(ReturnLine::getTenantId, tenantId).in(ReturnLine::getReturnOrderId, ids));
        resources.addAll(lines);
        appendMaterialResources(tenantId, resources,
            lines.stream().map(ReturnLine::getMaterialId).toList());
        resources.addAll(documentLedgers(tenantId, "RETURN", ids));
        return registerEntities(tenantId, resources, resourceTypes);
    }

    /** 按盘点单 ID 批量登记单据、明细和调整流水。 */
    public List<I18nTextSource> registerStocktakeOrders(String tenantId, Collection<Long> orderIds) {
        return registerStocktakeOrders(tenantId, orderIds, List.of());
    }

    /** 按指定资源类型登记盘点单、明细和调整流水。 */
    public List<I18nTextSource> registerStocktakeOrders(String tenantId, Collection<Long> orderIds,
        Collection<String> resourceTypes) {
        List<Long> ids = ids(orderIds);
        if (StringUtils.isBlank(tenantId) || ids.isEmpty()) {
            return List.of();
        }
        List<Object> resources = new ArrayList<>();
        resources.addAll(stocktakeOrderMapper.selectList(Wrappers.<StocktakeOrder>lambdaQuery()
            .eq(StocktakeOrder::getTenantId, tenantId).in(StocktakeOrder::getStocktakeOrderId, ids)));
        List<StocktakeLine> lines = stocktakeLineMapper.selectList(Wrappers.<StocktakeLine>lambdaQuery()
            .eq(StocktakeLine::getTenantId, tenantId).in(StocktakeLine::getStocktakeOrderId, ids));
        resources.addAll(lines);
        appendMaterialResources(tenantId, resources,
            lines.stream().map(StocktakeLine::getMaterialId).toList());
        resources.addAll(documentLedgers(tenantId, "STOCKTAKE", ids));
        return registerEntities(tenantId, resources, resourceTypes);
    }

    /** 按领料单 ID 批量登记领料快照、明细和任务包最终经手技术员。 */
    public List<I18nTextSource> registerReceipts(String tenantId, Collection<Long> receiptIds) {
        List<Long> ids = ids(receiptIds);
        if (StringUtils.isBlank(tenantId) || ids.isEmpty()) {
            return List.of();
        }
        List<MaterialReceipt> receipts = receiptMapper.selectList(Wrappers.<MaterialReceipt>lambdaQuery()
            .eq(MaterialReceipt::getTenantId, tenantId).in(MaterialReceipt::getMaterialReceiptId, ids));
        List<MaterialReceiptLine> lines = receiptLineMapper.selectList(
            Wrappers.<MaterialReceiptLine>lambdaQuery()
                .eq(MaterialReceiptLine::getTenantId, tenantId)
                .in(MaterialReceiptLine::getMaterialReceiptId, ids));
        return registerReceiptEntities(tenantId, receipts, lines, loadTaskPackages(tenantId, receipts));
    }

    /** 按资产种类 ID 批量登记种类名称和备注。 */
    public List<I18nTextSource> registerAssetTypes(String tenantId, Collection<Long> typeIds) {
        List<Long> ids = ids(typeIds);
        if (StringUtils.isBlank(tenantId) || ids.isEmpty()) {
            return List.of();
        }
        return registerEntities(tenantId, assetTypeMapper.selectList(Wrappers.<AssetType>lambdaQuery()
            .eq(AssetType::getTenantId, tenantId).eq(AssetType::getDelFlag, "0")
            .in(AssetType::getAssetTypeId, ids)));
    }

    /** 按设备 ID 批量登记设备、资产种类和时间线文本。 */
    public List<I18nTextSource> registerAssetDevices(String tenantId, Collection<Long> deviceIds) {
        return registerAssetDevices(tenantId, deviceIds, List.of());
    }

    /** 按指定资源类型登记设备、资产种类和时间线文本。 */
    public List<I18nTextSource> registerAssetDevices(String tenantId, Collection<Long> deviceIds,
        Collection<String> resourceTypes) {
        List<Long> ids = ids(deviceIds);
        if (StringUtils.isBlank(tenantId) || ids.isEmpty()) {
            return List.of();
        }
        List<AssetDevice> devices = assetDeviceMapper.selectList(Wrappers.<AssetDevice>lambdaQuery()
            .eq(AssetDevice::getTenantId, tenantId).eq(AssetDevice::getDelFlag, "0")
            .in(AssetDevice::getAssetDeviceId, ids));
        List<Object> resources = new ArrayList<>(devices);
        List<Long> typeIds = devices.stream().map(AssetDevice::getAssetTypeId)
            .filter(Objects::nonNull).distinct().toList();
        if (!typeIds.isEmpty()) {
            resources.addAll(assetTypeMapper.selectList(Wrappers.<AssetType>lambdaQuery()
                .eq(AssetType::getTenantId, tenantId).eq(AssetType::getDelFlag, "0")
                .in(AssetType::getAssetTypeId, typeIds)));
        }
        resources.addAll(assetUsageLogMapper.selectList(Wrappers.<AssetUsageLog>lambdaQuery()
            .eq(AssetUsageLog::getTenantId, tenantId)
            .in(AssetUsageLog::getAssetDeviceId, ids)));
        return registerEntities(tenantId, resources, resourceTypes);
    }

    /**
     * 登记已由调用方按租户批量读取的库存实体，供历史回填复用。
     *
     * @param tenantId 当前租户
     * @param entities 实体集合
     * @return 非空且去重后的翻译来源
     */
    public List<I18nTextSource> registerEntities(String tenantId, Collection<?> entities) {
        return registerEntities(tenantId, entities, List.of());
    }

    /** 按指定资源类型登记已批量读取的实体；空类型集合表示全部。 */
    public List<I18nTextSource> registerEntities(String tenantId, Collection<?> entities,
        Collection<String> resourceTypes) {
        if (StringUtils.isBlank(tenantId) || entities == null || entities.isEmpty()) {
            return List.of();
        }
        return registerSources(tenantId, filterSources(extractSources(tenantId, entities), resourceTypes));
    }

    /** 供回填在已批量读取任务包时登记领料资源，避免逐单查询。 */
    public List<I18nTextSource> registerReceiptEntities(String tenantId,
        Collection<MaterialReceipt> receipts, Collection<MaterialReceiptLine> lines,
        Map<Long, SfStaskTaskPackage> packages) {
        return registerReceiptEntities(tenantId, receipts, lines, packages, List.of());
    }

    /** 按指定资源类型登记已批量读取的领料资源；空类型集合表示全部。 */
    public List<I18nTextSource> registerReceiptEntities(String tenantId,
        Collection<MaterialReceipt> receipts, Collection<MaterialReceiptLine> lines,
        Map<Long, SfStaskTaskPackage> packages, Collection<String> resourceTypes) {
        List<Object> resources = new ArrayList<>();
        if (receipts != null) {
            resources.addAll(receipts);
        }
        if (lines != null) {
            resources.addAll(lines);
        }
        LinkedHashSet<I18nTextSource> sources = new LinkedHashSet<>(extractSources(tenantId, resources));
        if (receipts != null && packages != null) {
            receipts.stream()
                .filter(Objects::nonNull)
                .filter(row -> Objects.equals(tenantId, row.getTenantId()))
                .forEach(row -> {
                    SfStaskTaskPackage taskPackage = packages.get(row.getTaskPackageId());
                    if (taskPackage != null && StringUtils.isNotBlank(
                        taskPackage.getHandlerTechnicianEmployeeNameSnapshot())) {
                        sources.add(new I18nTextSource(I18nResourceType.INVENTORY_RECEIPT,
                            row.getMaterialReceiptId(), "handlerTechnicianEmployeeName",
                            taskPackage.getHandlerTechnicianEmployeeNameSnapshot()));
                    }
                });
        }
        return registerSources(tenantId, filterSources(sources, resourceTypes));
    }

    private List<I18nTextSource> extractSources(String tenantId, Collection<?> entities) {
        LinkedHashSet<I18nTextSource> sources = new LinkedHashSet<>();
        entities.stream().filter(Objects::nonNull)
            .filter(entity -> Objects.equals(tenantId, tenantId(entity)))
            .forEach(entity -> sources.addAll(sources(entity)));
        return List.copyOf(sources);
    }

    private List<Ledger> documentLedgers(String tenantId, String orderType, List<Long> orderIds) {
        return ledgerMapper.selectList(Wrappers.<Ledger>lambdaQuery()
            .eq(Ledger::getTenantId, tenantId)
            .eq(Ledger::getRelatedOrderType, orderType)
            .in(Ledger::getRelatedOrderId, orderIds));
    }

    private void appendMaterialResources(String tenantId, List<Object> resources,
        Collection<Long> materialIds) {
        List<Long> ids = ids(materialIds);
        if (ids.isEmpty()) {
            return;
        }
        resources.addAll(materialMapper.selectList(Wrappers.<Material>lambdaQuery()
            .eq(Material::getTenantId, tenantId).eq(Material::getDelFlag, "0")
            .in(Material::getMaterialId, ids)));
        resources.addAll(balanceMapper.selectList(Wrappers.<Balance>lambdaQuery()
            .eq(Balance::getTenantId, tenantId).in(Balance::getMaterialId, ids)));
    }

    private Map<Long, SfStaskTaskPackage> loadTaskPackages(String tenantId,
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

    private List<I18nTextSource> registerSources(String tenantId,
        Collection<I18nTextSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        List<I18nTextSource> result = List.copyOf(new LinkedHashSet<>(sources));
        i18nTextService.registerTexts(tenantId, result);
        Map<String, Long> counts = result.stream().collect(Collectors.groupingBy(
            I18nTextSource::resourceType, Collectors.counting()));
        log.debug("inventory i18n resources registered: tenantId={}, resourceCounts={}", tenantId, counts);
        return result;
    }

    private Collection<I18nTextSource> filterSources(Collection<I18nTextSource> sources,
        Collection<String> resourceTypes) {
        if (resourceTypes == null || resourceTypes.isEmpty()) {
            return sources;
        }
        LinkedHashSet<String> selected = new LinkedHashSet<>(resourceTypes);
        return sources.stream().filter(source -> selected.contains(source.resourceType())).toList();
    }

    private List<I18nTextSource> sources(Object entity) {
        if (entity instanceof MaterialCategory row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof Material row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof Balance row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof Ledger row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof InboundOrder row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof InboundLine row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof OutboundOrder row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof OutboundLine row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof ReturnOrder row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof ReturnLine row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof StocktakeOrder row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof StocktakeLine row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof MaterialReceipt row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof MaterialReceiptLine row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof AssetType row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof AssetDevice row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        if (entity instanceof AssetUsageLog row) {
            return InventoryI18nSourceFactory.sources(row);
        }
        return List.of();
    }

    private String tenantId(Object entity) {
        if (entity instanceof MaterialCategory row) {
            return row.getTenantId();
        }
        if (entity instanceof Material row) {
            return row.getTenantId();
        }
        if (entity instanceof Balance row) {
            return row.getTenantId();
        }
        if (entity instanceof Ledger row) {
            return row.getTenantId();
        }
        if (entity instanceof InboundOrder row) {
            return row.getTenantId();
        }
        if (entity instanceof InboundLine row) {
            return row.getTenantId();
        }
        if (entity instanceof OutboundOrder row) {
            return row.getTenantId();
        }
        if (entity instanceof OutboundLine row) {
            return row.getTenantId();
        }
        if (entity instanceof ReturnOrder row) {
            return row.getTenantId();
        }
        if (entity instanceof ReturnLine row) {
            return row.getTenantId();
        }
        if (entity instanceof StocktakeOrder row) {
            return row.getTenantId();
        }
        if (entity instanceof StocktakeLine row) {
            return row.getTenantId();
        }
        if (entity instanceof MaterialReceipt row) {
            return row.getTenantId();
        }
        if (entity instanceof MaterialReceiptLine row) {
            return row.getTenantId();
        }
        if (entity instanceof AssetType row) {
            return row.getTenantId();
        }
        if (entity instanceof AssetDevice row) {
            return row.getTenantId();
        }
        if (entity instanceof AssetUsageLog row) {
            return row.getTenantId();
        }
        return null;
    }

    private List<Long> ids(Collection<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).distinct().toList();
    }
}
