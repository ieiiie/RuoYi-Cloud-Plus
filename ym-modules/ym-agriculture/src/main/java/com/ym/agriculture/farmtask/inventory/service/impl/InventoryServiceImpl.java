package com.ym.agriculture.farmtask.inventory.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AssetDeviceMapper;
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
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.ReturnOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.StocktakeLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.StocktakeOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.TaskMaterialMapper;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundLineVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetDevice;
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
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.StocktakeLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.StocktakeOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.TaskMaterial;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.DirectOutboundSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.DocumentLineBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.InboundSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.CancelOutboundBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.LedgerQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.LedgerVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialCategoryCount;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialCategorySaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialCategoryVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OrderQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OutboundConfirmBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OutboundLineVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OutboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.PendingLineBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.PendingLinesSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeLineBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeDetailLineVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeDetailVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.WorkbenchVo;
import com.ym.agriculture.farmtask.inventory.service.IInventoryService;
import com.ym.agriculture.farmtask.inventory.service.InventoryInfrastructure;
import com.ym.agriculture.farmtask.inventory.support.InventoryQuantity;
import com.ym.agriculture.farmtask.inventory.support.InventoryI18nResourceRegistrar;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements IInventoryService {

    private final InventoryInfrastructure infrastructure;
    private final MaterialCategoryMapper materialCategoryMapper;
    private final MaterialMapper materialMapper;
    private final AssetDeviceMapper assetDeviceMapper;
    private final BalanceMapper balanceMapper;
    private final LedgerMapper ledgerMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundLineMapper inboundLineMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final OutboundLineMapper outboundLineMapper;
    private final StocktakeOrderMapper stocktakeOrderMapper;
    private final StocktakeLineMapper stocktakeLineMapper;
    private final MaterialReceiptMapper receiptMapper;
    private final MaterialReceiptLineMapper receiptLineMapper;
    private final TaskMaterialMapper taskMaterialMapper;
    private final InventoryI18nResourceRegistrar i18nRegistrar;

    @Override
    public List<MaterialCategoryVo> listMaterialCategories() {
        String tenantId = infrastructure.tenantId();
        Map<Long, Long> materialCounts = materialMapper.selectCategoryCounts(tenantId).stream()
            .collect(Collectors.toMap(MaterialCategoryCount::getCategoryId,
                MaterialCategoryCount::getMaterialCount));
        return materialCategoryMapper.selectList(Wrappers.<MaterialCategory>lambdaQuery()
                .eq(MaterialCategory::getTenantId, tenantId)
                .orderByAsc(MaterialCategory::getSortOrder)
                .orderByAsc(MaterialCategory::getCategoryId)).stream()
            .map(item -> toMaterialCategoryVo(item, materialCounts.getOrDefault(item.getCategoryId(), 0L)))
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo addMaterialCategory(MaterialCategorySaveBo bo) {
        String tenantId = infrastructure.tenantId();
        Date now = new Date();
        MaterialCategory category = new MaterialCategory();
        category.setCategoryId(IdWorker.getId());
        category.setTenantId(tenantId);
        copyMaterialCategoryFields(category, bo);
        category.setVersion(0L);
        category.setCreateBy(infrastructure.operatorId());
        category.setCreateTime(now);
        category.setUpdateBy(infrastructure.operatorId());
        category.setUpdateTime(now);
        try {
            materialCategoryMapper.insert(category);
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("物料分类名称已存在");
        }
        infrastructure.audit("MATERIAL_CATEGORY", category.getCategoryId(), "CREATE",
            category.getRemark(), null, category);
        i18nRegistrar.registerMaterialCategories(tenantId, List.of(category.getCategoryId()));
        return new MutationVo(category.getCategoryId(), category.getVersion(),
            Boolean.TRUE.equals(category.getEnabled()) ? "ENABLED" : "DISABLED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo updateMaterialCategory(Long categoryId, MaterialCategorySaveBo bo) {
        String tenantId = infrastructure.tenantId();
        MaterialCategory current = requireMaterialCategory(categoryId);
        requireVersion(current.getVersion(), bo.getVersion());
        String oldName = current.getCategoryName();
        MaterialCategory before = cloneMaterialCategory(current);
        copyMaterialCategoryFields(current, bo);
        current.setUpdateBy(infrastructure.operatorId());
        current.setUpdateTime(new Date());
        try {
            if (materialCategoryMapper.updateById(current) != 1) {
                throw versionConflict();
            }
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("物料分类名称已存在");
        }
        if (!current.getCategoryName().equals(oldName)) {
            materialMapper.update(null, Wrappers.<Material>lambdaUpdate()
                .eq(Material::getTenantId, tenantId)
                .eq(Material::getCategoryId, categoryId)
                .set(Material::getCategory, current.getCategoryName())
                .set(Material::getUpdateBy, infrastructure.operatorId())
                .set(Material::getUpdateTime, new Date()));
        }
        infrastructure.audit("MATERIAL_CATEGORY", categoryId, "UPDATE", current.getRemark(), before, current);
        i18nRegistrar.registerMaterialCategories(tenantId, List.of(categoryId));
        return new MutationVo(categoryId, current.getVersion(),
            Boolean.TRUE.equals(current.getEnabled()) ? "ENABLED" : "DISABLED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMaterialCategory(Long categoryId, Long version) {
        MaterialCategory category = requireMaterialCategory(categoryId);
        requireVersion(category.getVersion(), version);
        if (materialMapper.exists(Wrappers.<Material>lambdaQuery()
            .eq(Material::getTenantId, infrastructure.tenantId())
            .eq(Material::getCategoryId, categoryId))) {
            throw InventoryBusinessException.rule("物料分类已被物料引用，不能删除，可改为停用");
        }
        if (materialCategoryMapper.delete(Wrappers.<MaterialCategory>lambdaQuery()
            .eq(MaterialCategory::getTenantId, infrastructure.tenantId())
            .eq(MaterialCategory::getCategoryId, categoryId)
            .eq(MaterialCategory::getVersion, version)) != 1) {
            throw versionConflict();
        }
        infrastructure.audit("MATERIAL_CATEGORY", categoryId, "DELETE", "删除未使用物料分类", category, null);
    }

    @Override
    public PageResult<MaterialVo> pageMaterials(MaterialQuery query, PageQuery pageQuery) {
        String tenantId = infrastructure.tenantId();
        List<Balance> allBalances = balanceMapper.selectList(Wrappers.<Balance>lambdaQuery()
            .eq(Balance::getTenantId, tenantId));
        Map<Long, Balance> balances = allBalances.stream()
            .collect(Collectors.toMap(Balance::getMaterialId, Function.identity()));
        Set<Long> statusIds = switch (StrUtil.blankToDefault(query.getStatus(), "ALL").toUpperCase()) {
            case "ABNORMAL" -> allBalances.stream().filter(item -> Boolean.TRUE.equals(item.getOutboundLocked()))
                .map(Balance::getMaterialId).collect(Collectors.toSet());
            case "NORMAL" -> allBalances.stream().filter(item -> !Boolean.TRUE.equals(item.getOutboundLocked()))
                .map(Balance::getMaterialId).collect(Collectors.toSet());
            default -> Set.of();
        };
        if (StrUtil.isNotBlank(query.getStatus()) && statusIds.isEmpty()) {
            return com.ym.agriculture.shared.common.AgriculturePageResults.build(new Page<MaterialVo>(pageQuery.getPageNum(), pageQuery.getPageSize(), 0));
        }
        Page<Material> page = materialMapper.selectPage(pageQuery.build(), Wrappers.<Material>lambdaQuery()
            .eq(Material::getTenantId, tenantId)
            .eq(Material::getDelFlag, "0")
            .eq(query.getEnabled() != null, Material::getEnabled, query.getEnabled())
            .eq(query.getCategoryId() != null, Material::getCategoryId, query.getCategoryId())
            .eq(query.getCategoryId() == null && StrUtil.isNotBlank(query.getCategory()),
                Material::getCategory, StrUtil.trim(query.getCategory()))
            .in(StrUtil.isNotBlank(query.getStatus()), Material::getMaterialId, statusIds)
            .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                .like(Material::getMaterialCode, query.getKeyword())
                .or().like(Material::getMaterialName, query.getKeyword())
                .or().like(Material::getSpecification, query.getKeyword()))
            .orderByDesc(Material::getUpdateTime).orderByDesc(Material::getMaterialId));
        Page<MaterialVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(item -> toMaterialVo(item, balances.get(item.getMaterialId()))).toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public MaterialVo getMaterial(Long materialId) {
        Material material = infrastructure.requireMaterial(materialId);
        Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
            .eq(Balance::getTenantId, infrastructure.tenantId()).eq(Balance::getMaterialId, materialId));
        return toMaterialVo(material, balance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo addMaterial(MaterialSaveBo bo) {
        Date now = new Date();
        MaterialCategory category = resolveMaterialCategory(bo, null);
        Material material = new Material();
        material.setMaterialId(IdWorker.getId());
        material.setTenantId(infrastructure.tenantId());
        copyMaterialFields(material, bo, category);
        material.setVersion(0L);
        material.setDelFlag("0");
        material.setCreateBy(infrastructure.operatorId());
        material.setCreateTime(now);
        material.setUpdateBy(infrastructure.operatorId());
        material.setUpdateTime(now);
        try {
            materialMapper.insert(material);
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("物资编码已存在");
        }
        Balance balance = new Balance();
        balance.setBalanceId(IdWorker.getId());
        balance.setTenantId(material.getTenantId());
        balance.setMaterialId(material.getMaterialId());
        balance.setQuantity(InventoryQuantity.ZERO);
        balance.setOutboundLocked(false);
        balance.setVersion(0L);
        balance.setCreateBy(infrastructure.operatorId());
        balance.setCreateTime(now);
        balance.setUpdateBy(infrastructure.operatorId());
        balance.setUpdateTime(now);
        balanceMapper.insert(balance);
        infrastructure.audit("MATERIAL", material.getMaterialId(), "CREATE", bo.getRemark(), null, material);
        i18nRegistrar.registerMaterials(material.getTenantId(), List.of(material.getMaterialId()));
        return new MutationVo(material.getMaterialId(), material.getVersion(), Boolean.TRUE.equals(material.getEnabled()) ? "ENABLED" : "DISABLED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo updateMaterial(Long materialId, MaterialSaveBo bo) {
        Material current = infrastructure.requireMaterial(materialId);
        requireVersion(current.getVersion(), bo.getVersion());
        MaterialCategory category = resolveMaterialCategory(bo, current.getCategoryId());
        if (Boolean.TRUE.equals(current.getEnabled()) && Boolean.FALSE.equals(bo.getEnabled())) {
            List<Long> pendingOrderIds = outboundOrderMapper.selectList(Wrappers.<OutboundOrder>lambdaQuery()
                .eq(OutboundOrder::getTenantId, infrastructure.tenantId())
                .eq(OutboundOrder::getStatus, InventoryConstants.STATUS_PENDING)
                .select(OutboundOrder::getOutboundOrderId)).stream()
                .map(OutboundOrder::getOutboundOrderId).toList();
            boolean occupied = !pendingOrderIds.isEmpty() && outboundLineMapper.exists(Wrappers.<OutboundLine>lambdaQuery()
                .eq(OutboundLine::getTenantId, infrastructure.tenantId())
                .eq(OutboundLine::getMaterialId, materialId)
                .eq(OutboundLine::getDeletedByKeeper, false)
                .in(OutboundLine::getOutboundOrderId, pendingOrderIds));
            if (occupied) {
                throw InventoryBusinessException.rule("物资存在有效待出库单，请先处理后再停用");
            }
        }
        Material before = cloneMaterial(current);
        copyMaterialFields(current, bo, category);
        current.setUpdateBy(infrastructure.operatorId());
        current.setUpdateTime(new Date());
        try {
            if (materialMapper.updateById(current) != 1) {
                throw versionConflict();
            }
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("物资编码已存在");
        }
        infrastructure.audit("MATERIAL", materialId, "UPDATE", bo.getRemark(), before, current);
        i18nRegistrar.registerMaterials(current.getTenantId(), List.of(materialId));
        return new MutationVo(materialId, current.getVersion(), Boolean.TRUE.equals(current.getEnabled()) ? "ENABLED" : "DISABLED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMaterial(Long materialId) {
        if (!LoginHelper.isSuperAdmin()) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ACTION_FORBIDDEN, 403,
                "只有超级管理员可以删除物资");
        }
        Material material = infrastructure.requireMaterial(materialId);
        Balance balance = infrastructure.requireBalanceForUpdate(materialId);
        if (balance.getQuantity().compareTo(BigDecimal.ZERO) != 0
            || ledgerMapper.exists(Wrappers.<Ledger>lambdaQuery().eq(Ledger::getTenantId, infrastructure.tenantId())
                .eq(Ledger::getMaterialId, materialId))
            || taskMaterialMapper.exists(Wrappers.<TaskMaterial>lambdaQuery().eq(TaskMaterial::getTenantId, infrastructure.tenantId())
                .eq(TaskMaterial::getMaterialId, materialId))) {
            throw InventoryBusinessException.rule("物资已有库存或业务记录，不能删除");
        }
        Material before = cloneMaterial(material);
        material.setDelFlag("1");
        material.setUpdateBy(infrastructure.operatorId());
        material.setUpdateTime(new Date());
        materialMapper.updateById(material);
        infrastructure.audit("MATERIAL", materialId, "DELETE", "超级管理员删除未使用物资", before, material);
    }

    @Override
    public PageResult<LedgerVo> pageLedger(Long materialId, LedgerQuery query, PageQuery pageQuery) {
        infrastructure.requireMaterial(materialId);
        List<Ledger> ledgers = ledgerMapper.selectList(Wrappers.<Ledger>lambdaQuery()
            .eq(Ledger::getTenantId, infrastructure.tenantId()).eq(Ledger::getMaterialId, materialId)
            .eq(Ledger::getEffective, true)
            .orderByAsc(Ledger::getOccurredAt).orderByAsc(Ledger::getLedgerId));
        BigDecimal running = InventoryQuantity.ZERO;
        List<LedgerVo> rows = new ArrayList<>();
        for (Ledger ledger : ledgers) {
            running = running.add(ledger.getQuantityDelta()).setScale(InventoryConstants.QUANTITY_SCALE);
            LedgerVo vo = toLedgerVo(ledger);
            vo.setBalanceAfter(running);
            if (StrUtil.isBlank(query.getType()) || query.getType().equalsIgnoreCase(ledger.getLedgerType())) {
                rows.add(vo);
            }
        }
        Collections.reverse(rows);
        long pageNum = Math.max(1L, pageQuery.getPageNum());
        long pageSize = Math.max(1L, pageQuery.getPageSize());
        int from = (int) Math.min(rows.size(), (pageNum - 1L) * pageSize);
        int to = (int) Math.min(rows.size(), from + pageSize);
        Page<LedgerVo> result = new Page<>(pageNum, pageSize, rows.size());
        result.setRecords(new ArrayList<>(rows.subList(from, to)));
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public WorkbenchVo workbench() {
        String tenantId = infrastructure.tenantId();
        WorkbenchVo vo = new WorkbenchVo();
        vo.setPendingOutboundCount(outboundOrderMapper.selectCount(Wrappers.<OutboundOrder>lambdaQuery()
            .eq(OutboundOrder::getTenantId, tenantId).eq(OutboundOrder::getStatus, InventoryConstants.STATUS_PENDING)));
        vo.setPendingReturnCount(returnOrderMapper.selectCount(Wrappers.<ReturnOrder>lambdaQuery()
            .eq(ReturnOrder::getTenantId, tenantId)
            .eq(ReturnOrder::getStatus, InventoryConstants.STATUS_PENDING_RETURN)));
        vo.setAbnormalInventoryCount(balanceMapper.selectCount(Wrappers.<Balance>lambdaQuery()
            .eq(Balance::getTenantId, tenantId).eq(Balance::getOutboundLocked, true)));
        vo.setEnabledMaterialCount(materialMapper.selectCount(Wrappers.<Material>lambdaQuery()
            .eq(Material::getTenantId, tenantId).eq(Material::getDelFlag, "0").eq(Material::getEnabled, true)));
        vo.setAssetCount(assetDeviceMapper.selectCount(Wrappers.<AssetDevice>lambdaQuery()
            .eq(AssetDevice::getTenantId, tenantId).eq(AssetDevice::getDelFlag, "0")));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo createInbound(InboundSaveBo bo) {
        LinkedHashMap<Long, BigDecimal> quantities = normalizeDocumentLines(bo.getLines(), "入库数量");
        Date now = new Date();
        InboundOrder order = new InboundOrder();
        order.setInboundOrderId(IdWorker.getId());
        order.setTenantId(infrastructure.tenantId());
        String requestedOrderNo = StrUtil.trim(bo.getOrderNo());
        boolean generatedOrderNo = StrUtil.isBlank(requestedOrderNo);
        order.setStatus(InventoryConstants.STATUS_COMPLETED);
        order.setSupplierName(StrUtil.trim(bo.getSupplierName()));
        order.setBusinessDate(bo.getBusinessDate() == null ? now : bo.getBusinessDate());
        order.setVersion(0L);
        order.setCreateBy(infrastructure.operatorId());
        order.setCreateTime(now);
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(now);
        order.setRemark(bo.getRemark());
        int insertAttempts = generatedOrderNo ? 10 : 1;
        for (int attempt = 0; attempt < insertAttempts; attempt++) {
            order.setOrderNo(generatedOrderNo
                ? infrastructure.nextOrderNo("INBOUND") : requestedOrderNo);
            try {
                inboundOrderMapper.insert(order);
                break;
            } catch (DuplicateKeyException exception) {
                if (!generatedOrderNo || attempt == insertAttempts - 1) {
                    throw InventoryBusinessException.rule("入库单号已存在");
                }
            }
        }
        for (Map.Entry<Long, BigDecimal> entry : sortedEntries(quantities)) {
            Material material = infrastructure.requireEnabledMaterial(entry.getKey());
            InboundLine line = newInboundLine(order, material, entry.getValue(), now);
            inboundLineMapper.insert(line);
            infrastructure.applyDelta(material, line.getQuantity(), InventoryConstants.LEDGER_INBOUND,
                "WEB_INBOUND", "INBOUND", order.getInboundOrderId(), order.getOrderNo(), line.getInboundLineId(),
                false, null, order.getBusinessDate());
        }
        infrastructure.audit("INBOUND", order.getInboundOrderId(), "CREATE_AND_EFFECT", bo.getRemark(), null, order);
        i18nRegistrar.registerInboundOrders(order.getTenantId(), List.of(order.getInboundOrderId()));
        return new MutationVo(order.getInboundOrderId(), order.getVersion(), order.getStatus());
    }

    @Override
    public PageResult<InboundVo> pageInbound(OrderQuery query, PageQuery pageQuery) {
        Page<InboundOrder> page = inboundOrderMapper.selectPage(pageQuery.build(), Wrappers.<InboundOrder>lambdaQuery()
            .eq(InboundOrder::getTenantId, infrastructure.tenantId())
            .eq(StrUtil.isNotBlank(query.getStatus()), InboundOrder::getStatus, query.getStatus())
            .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                .like(InboundOrder::getOrderNo, query.getKeyword())
                .or().like(InboundOrder::getSupplierName, query.getKeyword()))
            .ge(query.getBeginDate() != null, InboundOrder::getBusinessDate, query.getBeginDate())
            .lt(query.getEndDate() != null, InboundOrder::getBusinessDate,
                query.getEndDate() == null ? null : DateUtil.offsetDay(query.getEndDate(), 1))
            .orderByDesc(InboundOrder::getBusinessDate).orderByDesc(InboundOrder::getInboundOrderId));
        Page<InboundVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toInboundSummary).toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo createDirectOutbound(DirectOutboundSaveBo bo) {
        LinkedHashMap<Long, BigDecimal> quantities = normalizeDocumentLines(bo.getLines(), "出库数量");
        if (bo.getMaterialReceiptId() == null) {
            return createStandaloneDirectOutbound(bo, quantities);
        }
        return createReceiptDirectOutbound(bo, quantities);
    }

    private MutationVo createStandaloneDirectOutbound(DirectOutboundSaveBo bo,
                                                        LinkedHashMap<Long, BigDecimal> quantities) {
        OutboundOrder order = newOutboundOrder(InventoryConstants.SOURCE_WEB_DIRECT,
            InventoryConstants.STATUS_COMPLETED, bo.getBusinessDate(), bo.getRemark());
        if (StrUtil.isNotBlank(bo.getOrderNo())) {
            order.setOrderNo(StrUtil.trim(bo.getOrderNo()));
        }
        order.setReceiverNameSnapshot(StrUtil.trim(bo.getReceiverName()));
        order.setConfirmedBy(infrastructure.operatorId());
        order.setConfirmedAt(new Date());
        try {
            outboundOrderMapper.insert(order);
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("出库单号已存在");
        }
        Date now = new Date();
        for (Map.Entry<Long, BigDecimal> entry : sortedEntries(quantities)) {
            Material material = infrastructure.requireEnabledMaterial(entry.getKey());
            OutboundLine line = newOutboundLine(order, material, null, entry.getValue(), now);
            outboundLineMapper.insert(line);
            infrastructure.applyDelta(material, line.getActualQuantity().negate(), InventoryConstants.LEDGER_OUTBOUND,
                "WEB_DIRECT", "OUTBOUND", order.getOutboundOrderId(), order.getOrderNo(), line.getOutboundLineId(),
                false, null, order.getBusinessDate());
        }
        infrastructure.audit("OUTBOUND", order.getOutboundOrderId(), "CREATE_AND_CONFIRM", bo.getRemark(), null, order);
        i18nRegistrar.registerOutboundOrders(order.getTenantId(), List.of(order.getOutboundOrderId()));
        return new MutationVo(order.getOutboundOrderId(), order.getVersion(), order.getStatus());
    }

    private MutationVo createReceiptDirectOutbound(DirectOutboundSaveBo bo,
                                                    LinkedHashMap<Long, BigDecimal> quantities) {
        MaterialReceipt receipt = receiptMapper.selectForUpdate(infrastructure.tenantId(), bo.getMaterialReceiptId());
        if (receipt == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, "领料单不存在");
        }
        if (!Boolean.TRUE.equals(receipt.getArrived())) {
            throw InventoryBusinessException.rule("关联工单尚未到岗，不能直接出库");
        }
        if (!InventoryConstants.RECEIPT_UNCLAIMED.equals(receipt.getStatus())) {
            throw InventoryBusinessException.rule("领料单状态已变化，请刷新后重新选择");
        }
        List<MaterialReceiptLine> receiptLines = receiptLineMapper.selectList(Wrappers.<MaterialReceiptLine>lambdaQuery()
            .eq(MaterialReceiptLine::getTenantId, infrastructure.tenantId())
            .eq(MaterialReceiptLine::getMaterialReceiptId, receipt.getMaterialReceiptId())
            .orderByAsc(MaterialReceiptLine::getMaterialId)
            .last("FOR UPDATE"));
        Map<Long, MaterialReceiptLine> receiptLinesByMaterial = receiptLines.stream()
            .collect(Collectors.toMap(MaterialReceiptLine::getMaterialId, Function.identity()));
        if (!receiptLinesByMaterial.keySet().containsAll(quantities.keySet())) {
            throw InventoryBusinessException.rule("关联出库只能选择领料单申请的物资");
        }
        for (Map.Entry<Long, BigDecimal> entry : quantities.entrySet()) {
            MaterialReceiptLine receiptLine = receiptLinesByMaterial.get(entry.getKey());
            if (entry.getValue().compareTo(receiptLine.getRequestedQuantity()) > 0) {
                throw InventoryBusinessException.rule("实际出库数量不能超过领料申请数量");
            }
        }

        OutboundOrder order = newOutboundOrder(InventoryConstants.SOURCE_MATERIAL_RECEIPT,
            InventoryConstants.STATUS_COMPLETED, bo.getBusinessDate(), bo.getRemark());
        if (StrUtil.isNotBlank(bo.getOrderNo())) {
            order.setOrderNo(StrUtil.trim(bo.getOrderNo()));
        }
        order.setMaterialReceiptId(receipt.getMaterialReceiptId());
        order.setTaskPackageId(receipt.getTaskPackageId());
        order.setFarmItemId(receipt.getFarmItemId());
        order.setLeaderEmployeeId(receipt.getLeaderEmployeeId());
        order.setReceiverNameSnapshot(StrUtil.trim(bo.getReceiverName()));
        order.setTaskNameSnapshot(receipt.getTaskNameSnapshot());
        order.setFarmWorkNameSnapshot(receipt.getFarmWorkNameSnapshot());
        order.setGreenhouseNamesSnapshot(receipt.getGreenhouseNamesSnapshot());
        Date now = new Date();
        order.setConfirmedBy(infrastructure.operatorId());
        order.setConfirmedAt(now);
        try {
            outboundOrderMapper.insert(order);
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("出库单号已存在");
        }
        for (MaterialReceiptLine receiptLine : receiptLines) {
            BigDecimal quantity = quantities.getOrDefault(receiptLine.getMaterialId(), InventoryQuantity.ZERO);
            receiptLine.setActualQuantity(quantity);
            receiptLine.setUpdateTime(now);
            receiptLineMapper.updateById(receiptLine);
            if (quantity.compareTo(InventoryQuantity.ZERO) == 0) {
                continue;
            }
            // 任务物资快照允许在物资停用后完成既有领料，库存扣减仍由服务端统一校验。
            Material material = infrastructure.requireMaterial(receiptLine.getMaterialId());
            OutboundLine line = newOutboundLine(order, material, receiptLine.getMaterialReceiptLineId(), quantity, now);
            outboundLineMapper.insert(line);
            infrastructure.applyDelta(material, quantity.negate(), InventoryConstants.LEDGER_OUTBOUND,
                InventoryConstants.SOURCE_MATERIAL_RECEIPT, "OUTBOUND", order.getOutboundOrderId(), order.getOrderNo(),
                line.getOutboundLineId(), false, null, order.getBusinessDate());
        }
        receipt.setStatus(InventoryConstants.RECEIPT_RECEIVED);
        receipt.setUpdateBy(infrastructure.operatorId());
        receipt.setUpdateTime(now);
        receiptMapper.updateById(receipt);
        infrastructure.audit("OUTBOUND", order.getOutboundOrderId(), "CREATE_AND_CONFIRM", bo.getRemark(), null, order);
        i18nRegistrar.registerOutboundOrders(order.getTenantId(), List.of(order.getOutboundOrderId()));
        i18nRegistrar.registerReceipts(receipt.getTenantId(), List.of(receipt.getMaterialReceiptId()));
        return new MutationVo(order.getOutboundOrderId(), order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo createPendingOutboundFromReceipt(Long receiptId) {
        MaterialReceipt receipt = receiptMapper.selectForUpdate(infrastructure.tenantId(), receiptId);
        if (receipt == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, "领料单不存在");
        }
        if (!Boolean.TRUE.equals(receipt.getArrived())) {
            throw InventoryBusinessException.rule("关联工单尚未到岗，不能申请出库");
        }
        if (!InventoryConstants.RECEIPT_UNCLAIMED.equals(receipt.getStatus())) {
            throw InventoryBusinessException.rule("领料单当前状态不能申请出库");
        }
        OutboundOrder existing = outboundOrderMapper.selectOne(Wrappers.<OutboundOrder>lambdaQuery()
            .eq(OutboundOrder::getTenantId, infrastructure.tenantId())
            .eq(OutboundOrder::getMaterialReceiptId, receiptId)
            .eq(OutboundOrder::getStatus, InventoryConstants.STATUS_PENDING));
        if (existing != null) {
            return new MutationVo(existing.getOutboundOrderId(), existing.getVersion(), existing.getStatus());
        }
        OutboundOrder order = newOutboundOrder(InventoryConstants.SOURCE_MATERIAL_RECEIPT,
            InventoryConstants.STATUS_PENDING, new Date(), null);
        order.setMaterialReceiptId(receiptId);
        order.setTaskPackageId(receipt.getTaskPackageId());
        order.setFarmItemId(receipt.getFarmItemId());
        order.setLeaderEmployeeId(receipt.getLeaderEmployeeId());
        order.setReceiverNameSnapshot(receipt.getLeaderNameSnapshot());
        order.setTaskNameSnapshot(receipt.getTaskNameSnapshot());
        order.setFarmWorkNameSnapshot(receipt.getFarmWorkNameSnapshot());
        order.setGreenhouseNamesSnapshot(receipt.getGreenhouseNamesSnapshot());
        outboundOrderMapper.insert(order);
        List<MaterialReceiptLine> receiptLines = receiptLineMapper.selectList(Wrappers.<MaterialReceiptLine>lambdaQuery()
            .eq(MaterialReceiptLine::getTenantId, infrastructure.tenantId())
            .eq(MaterialReceiptLine::getMaterialReceiptId, receiptId)
            .orderByAsc(MaterialReceiptLine::getMaterialId));
        Date now = new Date();
        for (MaterialReceiptLine receiptLine : receiptLines) {
            Material material = infrastructure.requireMaterial(receiptLine.getMaterialId());
            OutboundLine line = newOutboundLine(order, material, receiptLine.getMaterialReceiptLineId(),
                receiptLine.getRequestedQuantity(), now);
            outboundLineMapper.insert(line);
        }
        receipt.setStatus(InventoryConstants.RECEIPT_PENDING_OUTBOUND);
        // 已创建新的待出库单，上一轮被退回的理由不能继续展示。
        receipt.setKeeperCancelReason(null);
        receipt.setUpdateBy(infrastructure.operatorId());
        receipt.setUpdateTime(now);
        receiptMapper.updateById(receipt);
        infrastructure.audit("OUTBOUND", order.getOutboundOrderId(), "APPLY", null, null, order);
        i18nRegistrar.registerOutboundOrders(order.getTenantId(), List.of(order.getOutboundOrderId()));
        i18nRegistrar.registerReceipts(receipt.getTenantId(), List.of(receiptId));
        return new MutationVo(order.getOutboundOrderId(), order.getVersion(), order.getStatus());
    }

    @Override
    public PageResult<OutboundVo> pageOutbound(OrderQuery query, PageQuery pageQuery) {
        Page<OutboundOrder> page = outboundOrderMapper.selectPage(pageQuery.build(), Wrappers.<OutboundOrder>lambdaQuery()
            .eq(OutboundOrder::getTenantId, infrastructure.tenantId())
            .eq(StrUtil.isNotBlank(query.getStatus()), OutboundOrder::getStatus, query.getStatus())
            .eq(StrUtil.isNotBlank(query.getSource()), OutboundOrder::getSource, query.getSource())
            .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper.like(OutboundOrder::getOrderNo, query.getKeyword())
                .or().like(OutboundOrder::getReceiverNameSnapshot, query.getKeyword())
                .or().like(OutboundOrder::getTaskNameSnapshot, query.getKeyword()))
            .ge(query.getBeginDate() != null, OutboundOrder::getBusinessDate, query.getBeginDate())
            .lt(query.getEndDate() != null, OutboundOrder::getBusinessDate,
                query.getEndDate() == null ? null : DateUtil.offsetDay(query.getEndDate(), 1))
            .orderByDesc(OutboundOrder::getCreateTime).orderByDesc(OutboundOrder::getOutboundOrderId));
        Page<OutboundVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        Map<Long, String> receiptNos = receiptNos(page.getRecords().stream()
            .map(OutboundOrder::getMaterialReceiptId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList());
        result.setRecords(page.getRecords().stream().map(item -> toOutboundVo(item, false, receiptNos)).toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public OutboundVo getOutbound(Long id) {
        OutboundOrder order = outboundOrderMapper.selectOne(Wrappers.<OutboundOrder>lambdaQuery()
            .eq(OutboundOrder::getTenantId, infrastructure.tenantId()).eq(OutboundOrder::getOutboundOrderId, id));
        if (order == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, "出库单不存在");
        }
        return toOutboundVo(order, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo savePendingOutboundLines(Long id, PendingLinesSaveBo bo) {
        OutboundOrder order = requirePendingOutbound(id, bo.getVersion());
        applyPendingLines(order, bo.getLines());
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(new Date());
        if (outboundOrderMapper.updateById(order) != 1) {
            throw versionConflict();
        }
        infrastructure.audit("OUTBOUND", id, "UPDATE_PENDING_LINES", null, null, toOutboundVo(order, true));
        i18nRegistrar.registerOutboundOrders(order.getTenantId(), List.of(id));
        return new MutationVo(id, order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo confirmOutbound(Long id, OutboundConfirmBo bo) {
        return infrastructure.idempotent("OUTBOUND_CONFIRM", bo.getIdempotencyKey(), Map.of("id", id, "request", bo),
            () -> doConfirmOutbound(id, bo));
    }

    private MutationVo doConfirmOutbound(Long id, OutboundConfirmBo bo) {
        OutboundOrder order = requirePendingOutbound(id, bo.getVersion());
        applyPendingLines(order, bo.getLines());
        List<OutboundLine> lines = outboundLineMapper.selectList(Wrappers.<OutboundLine>lambdaQuery()
            .eq(OutboundLine::getTenantId, infrastructure.tenantId())
            .eq(OutboundLine::getOutboundOrderId, id).orderByAsc(OutboundLine::getMaterialId));
        if (lines.stream().noneMatch(line -> !Boolean.TRUE.equals(line.getDeletedByKeeper())
            && line.getActualQuantity().compareTo(BigDecimal.ZERO) > 0)) {
            throw InventoryBusinessException.rule("出库单至少保留一条实际数量大于0的明细");
        }
        Map<Long, MaterialReceiptLine> receiptLines = order.getMaterialReceiptId() == null ? Map.of()
            : receiptLineMapper.selectList(Wrappers.<MaterialReceiptLine>lambdaQuery()
                .eq(MaterialReceiptLine::getTenantId, infrastructure.tenantId())
                .eq(MaterialReceiptLine::getMaterialReceiptId, order.getMaterialReceiptId()))
                .stream().collect(Collectors.toMap(MaterialReceiptLine::getMaterialReceiptLineId, Function.identity()));
        for (OutboundLine line : lines) {
            if (Boolean.TRUE.equals(line.getDeletedByKeeper()) || line.getActualQuantity().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            // 任务创建时已固化物资快照。物资后续停用只禁止新选，不影响既有领料单完成出库。
            Material material = order.getMaterialReceiptId() == null
                ? infrastructure.requireEnabledMaterial(line.getMaterialId())
                : infrastructure.requireMaterial(line.getMaterialId());
            infrastructure.applyDelta(material, line.getActualQuantity().negate(), InventoryConstants.LEDGER_OUTBOUND,
                order.getSource(), "OUTBOUND", id, order.getOrderNo(), line.getOutboundLineId(), false, null);
            MaterialReceiptLine receiptLine = receiptLines.get(line.getMaterialReceiptLineId());
            if (receiptLine != null) {
                receiptLine.setActualQuantity(line.getActualQuantity());
                receiptLine.setUpdateTime(new Date());
                receiptLineMapper.updateById(receiptLine);
            }
        }
        order.setStatus(InventoryConstants.STATUS_COMPLETED);
        order.setConfirmedBy(infrastructure.operatorId());
        order.setConfirmedAt(new Date());
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(new Date());
        if (outboundOrderMapper.updateById(order) != 1) {
            throw versionConflict();
        }
        if (order.getMaterialReceiptId() != null) {
            MaterialReceipt receipt = receiptMapper.selectForUpdate(infrastructure.tenantId(), order.getMaterialReceiptId());
            receipt.setStatus(InventoryConstants.RECEIPT_RECEIVED);
            receipt.setUpdateBy(infrastructure.operatorId());
            receipt.setUpdateTime(new Date());
            receiptMapper.updateById(receipt);
        }
        infrastructure.audit("OUTBOUND", id, "CONFIRM", null, null, toOutboundVo(order, true));
        i18nRegistrar.registerOutboundOrders(order.getTenantId(), List.of(id));
        if (order.getMaterialReceiptId() != null) {
            i18nRegistrar.registerReceipts(order.getTenantId(), List.of(order.getMaterialReceiptId()));
        }
        return new MutationVo(id, order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo cancelOutbound(Long id, CancelOutboundBo bo) {
        OutboundOrder order = requirePendingOutbound(id, bo.getVersion());
        String keeperCancelReason = StrUtil.trim(bo.getKeeperCancelReason());
        if (StrUtil.isBlank(keeperCancelReason)) {
            keeperCancelReason = null;
        }
        order.setStatus(InventoryConstants.STATUS_CANCELLED);
        order.setCancelledBy(infrastructure.operatorId());
        order.setCancelledAt(new Date());
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(new Date());
        if (outboundOrderMapper.updateById(order) != 1) {
            throw versionConflict();
        }
        if (order.getMaterialReceiptId() != null) {
            MaterialReceipt receipt = receiptMapper.selectForUpdate(infrastructure.tenantId(), order.getMaterialReceiptId());
            if (receipt != null && InventoryConstants.RECEIPT_PENDING_OUTBOUND.equals(receipt.getStatus())) {
                receipt.setStatus(InventoryConstants.RECEIPT_UNCLAIMED);
                receipt.setKeeperCancelReason(keeperCancelReason);
                receipt.setUpdateBy(infrastructure.operatorId());
                receipt.setUpdateTime(new Date());
                receiptMapper.updateById(receipt);
            }
        }
        infrastructure.audit("OUTBOUND", id, "CANCEL", keeperCancelReason, null, order);
        i18nRegistrar.registerOutboundOrders(order.getTenantId(), List.of(id));
        if (order.getMaterialReceiptId() != null) {
            i18nRegistrar.registerReceipts(order.getTenantId(), List.of(order.getMaterialReceiptId()));
        }
        return new MutationVo(id, order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo createStocktake(StocktakeSaveBo bo) {
        Map<Long, BigDecimal> actualByMaterial = new LinkedHashMap<>();
        for (StocktakeLineBo line : bo.getLines()) {
            BigDecimal actual = InventoryQuantity.nonNegative(line.getActualQuantity(), "实盘数量");
            if (actualByMaterial.putIfAbsent(line.getInventoryMaterialId(), actual) != null) {
                throw InventoryBusinessException.rule("同一盘点单同一物资只能出现一行");
            }
        }
        Date now = new Date();
        StocktakeOrder order = new StocktakeOrder();
        order.setStocktakeOrderId(IdWorker.getId());
        order.setTenantId(infrastructure.tenantId());
        order.setOrderNo(infrastructure.nextOrderNo("STOCKTAKE"));
        order.setStatus(InventoryConstants.STATUS_COMPLETED);
        order.setBusinessDate(bo.getBusinessDate() == null ? now : bo.getBusinessDate());
        order.setVersion(0L);
        order.setCreateBy(infrastructure.operatorId());
        order.setCreateTime(now);
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(now);
        order.setRemark(bo.getRemark());
        stocktakeOrderMapper.insert(order);
        for (Map.Entry<Long, BigDecimal> entry : sortedEntries(actualByMaterial)) {
            Material material = infrastructure.requireMaterial(entry.getKey());
            Balance balance = infrastructure.requireBalanceForUpdate(entry.getKey());
            BigDecimal difference = entry.getValue().subtract(balance.getQuantity()).setScale(1);
            StocktakeLine line = new StocktakeLine();
            line.setStocktakeLineId(IdWorker.getId());
            line.setTenantId(order.getTenantId());
            line.setStocktakeOrderId(order.getStocktakeOrderId());
            line.setMaterialId(material.getMaterialId());
            snapshot(line, material);
            line.setSystemQuantity(balance.getQuantity());
            line.setActualQuantity(entry.getValue());
            line.setDifferenceQuantity(difference);
            line.setCreateTime(now);
            line.setUpdateTime(now);
            stocktakeLineMapper.insert(line);
            if (difference.compareTo(BigDecimal.ZERO) != 0) {
                infrastructure.applyDelta(material, difference, InventoryConstants.LEDGER_ADJUST,
                    "STOCKTAKE", "STOCKTAKE", order.getStocktakeOrderId(), order.getOrderNo(),
                    line.getStocktakeLineId(), false, null, order.getBusinessDate());
            }
        }
        infrastructure.audit("STOCKTAKE", order.getStocktakeOrderId(), "SUBMIT", bo.getRemark(), null, order);
        i18nRegistrar.registerStocktakeOrders(order.getTenantId(), List.of(order.getStocktakeOrderId()));
        return new MutationVo(order.getStocktakeOrderId(), order.getVersion(), order.getStatus());
    }

    @Override
    public PageResult<StocktakeVo> pageStocktake(OrderQuery query, PageQuery pageQuery) {
        Page<StocktakeOrder> page = stocktakeOrderMapper.selectPage(pageQuery.build(),
            Wrappers.<StocktakeOrder>lambdaQuery().eq(StocktakeOrder::getTenantId, infrastructure.tenantId())
                .eq(StrUtil.isNotBlank(query.getStatus()), StocktakeOrder::getStatus, query.getStatus())
                .like(StrUtil.isNotBlank(query.getKeyword()), StocktakeOrder::getOrderNo, query.getKeyword())
                .ge(query.getBeginDate() != null, StocktakeOrder::getBusinessDate, query.getBeginDate())
                .lt(query.getEndDate() != null, StocktakeOrder::getBusinessDate,
                    query.getEndDate() == null ? null : DateUtil.offsetDay(query.getEndDate(), 1))
                .orderByDesc(StocktakeOrder::getBusinessDate).orderByDesc(StocktakeOrder::getStocktakeOrderId));
        Page<StocktakeVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toStocktakeVo).toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public StocktakeDetailVo getStocktake(Long id) {
        String tenantId = infrastructure.tenantId();
        StocktakeOrder order = stocktakeOrderMapper.selectOne(Wrappers.<StocktakeOrder>lambdaQuery()
            .eq(StocktakeOrder::getTenantId, tenantId)
            .eq(StocktakeOrder::getStocktakeOrderId, id));
        if (order == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, "盘点单不存在");
        }
        List<StocktakeLine> lines = stocktakeLineMapper.selectList(Wrappers.<StocktakeLine>lambdaQuery()
            .eq(StocktakeLine::getTenantId, tenantId)
            .eq(StocktakeLine::getStocktakeOrderId, id)
            .orderByAsc(StocktakeLine::getCreateTime)
            .orderByAsc(StocktakeLine::getStocktakeLineId));
        StocktakeDetailVo detail = new StocktakeDetailVo();
        detail.setId(order.getStocktakeOrderId());
        detail.setOrderNo(order.getOrderNo());
        detail.setStatus(order.getStatus());
        detail.setBusinessDate(order.getBusinessDate());
        detail.setVersion(order.getVersion());
        detail.setCreateTime(order.getCreateTime());
        detail.setRemark(order.getRemark());
        detail.setLines(lines.stream().map(this::toStocktakeDetailLineVo).toList());
        return detail;
    }

    private InboundVo toInboundSummary(InboundOrder order) {
        InboundVo vo = new InboundVo();
        vo.setId(order.getInboundOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setSupplierName(order.getSupplierName());
        vo.setBusinessDate(order.getBusinessDate());
        vo.setVersion(order.getVersion());
        vo.setCreateTime(order.getCreateTime());
        vo.setRemark(order.getRemark());
        return vo;
    }

    private StocktakeVo toStocktakeVo(StocktakeOrder order) {
        StocktakeVo vo = new StocktakeVo();
        vo.setId(order.getStocktakeOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setBusinessDate(order.getBusinessDate());
        vo.setVersion(order.getVersion());
        vo.setCreateTime(order.getCreateTime());
        vo.setRemark(order.getRemark());
        return vo;
    }

    private StocktakeDetailLineVo toStocktakeDetailLineVo(StocktakeLine line) {
        StocktakeDetailLineVo vo = new StocktakeDetailLineVo();
        vo.setId(line.getStocktakeLineId());
        vo.setInventoryMaterialId(line.getMaterialId());
        vo.setMaterialCode(line.getMaterialCodeSnapshot());
        vo.setMaterialName(line.getMaterialNameSnapshot());
        vo.setSpecification(line.getSpecificationSnapshot());
        vo.setUnit(line.getUnitSnapshot());
        vo.setSystemQuantity(line.getSystemQuantity());
        vo.setActualQuantity(line.getActualQuantity());
        vo.setDifferenceQuantity(line.getDifferenceQuantity());
        return vo;
    }

    private OutboundOrder requirePendingOutbound(Long id, Long version) {
        OutboundOrder order = outboundOrderMapper.selectForUpdate(infrastructure.tenantId(), id);
        if (order == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, "出库单不存在");
        }
        requireVersion(order.getVersion(), version);
        if (!InventoryConstants.STATUS_PENDING.equals(order.getStatus())) {
            throw InventoryBusinessException.rule("只有待出库单可以执行该操作");
        }
        return order;
    }

    private void applyPendingLines(OutboundOrder order, List<PendingLineBo> requests) {
        List<OutboundLine> lines = outboundLineMapper.selectList(Wrappers.<OutboundLine>lambdaQuery()
            .eq(OutboundLine::getTenantId, infrastructure.tenantId())
            .eq(OutboundLine::getOutboundOrderId, order.getOutboundOrderId()));
        Map<Long, OutboundLine> byId = lines.stream().collect(Collectors.toMap(OutboundLine::getOutboundLineId, Function.identity()));
        Set<Long> submitted = requests.stream().map(PendingLineBo::getId).collect(Collectors.toSet());
        if (submitted.size() != requests.size() || !byId.keySet().containsAll(submitted)) {
            throw InventoryBusinessException.rule("出库明细不存在或不属于当前单据");
        }
        for (PendingLineBo request : requests) {
            OutboundLine line = byId.get(request.getId());
            boolean deleted = Boolean.TRUE.equals(request.getDeleted());
            line.setDeletedByKeeper(deleted);
            line.setActualQuantity(deleted ? InventoryQuantity.ZERO
                : InventoryQuantity.positive(request.getActualQuantity(), "实际出库数量"));
            line.setUpdateTime(new Date());
            outboundLineMapper.updateById(line);
        }
    }

    private LinkedHashMap<Long, BigDecimal> normalizeDocumentLines(List<DocumentLineBo> lines, String fieldName) {
        LinkedHashMap<Long, BigDecimal> result = new LinkedHashMap<>();
        for (DocumentLineBo line : lines) {
            BigDecimal quantity = InventoryQuantity.positive(line.getQuantity(), fieldName);
            if (result.putIfAbsent(line.getInventoryMaterialId(), quantity) != null) {
                throw InventoryBusinessException.rule("同一单据同一物资只能出现一行");
            }
        }
        return result;
    }

    private <T> List<Map.Entry<Long, T>> sortedEntries(Map<Long, T> map) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
    }

    private InboundLine newInboundLine(InboundOrder order, Material material, BigDecimal quantity, Date now) {
        InboundLine line = new InboundLine();
        line.setInboundLineId(IdWorker.getId());
        line.setTenantId(order.getTenantId());
        line.setInboundOrderId(order.getInboundOrderId());
        line.setMaterialId(material.getMaterialId());
        snapshot(line, material);
        line.setQuantity(quantity);
        line.setCreateTime(now);
        line.setUpdateTime(now);
        return line;
    }

    private OutboundOrder newOutboundOrder(String source, String status, Date businessDate, String remark) {
        Date now = new Date();
        OutboundOrder order = new OutboundOrder();
        order.setOutboundOrderId(IdWorker.getId());
        order.setTenantId(infrastructure.tenantId());
        order.setOrderNo(infrastructure.nextOrderNo("OUTBOUND"));
        order.setSource(source);
        order.setStatus(status);
        order.setBusinessDate(businessDate == null ? now : businessDate);
        order.setVersion(0L);
        order.setCreateBy(infrastructure.operatorId());
        order.setCreateTime(now);
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(now);
        order.setRemark(remark);
        return order;
    }

    private OutboundLine newOutboundLine(OutboundOrder order, Material material, Long receiptLineId,
                                         BigDecimal quantity, Date now) {
        OutboundLine line = new OutboundLine();
        line.setOutboundLineId(IdWorker.getId());
        line.setTenantId(order.getTenantId());
        line.setOutboundOrderId(order.getOutboundOrderId());
        line.setMaterialReceiptLineId(receiptLineId);
        line.setMaterialId(material.getMaterialId());
        snapshot(line, material);
        line.setRequestedQuantity(quantity);
        line.setActualQuantity(quantity);
        line.setDeletedByKeeper(false);
        line.setVersion(0L);
        line.setCreateTime(now);
        line.setUpdateTime(now);
        return line;
    }

    private OutboundVo toOutboundVo(OutboundOrder order, boolean detail) {
        List<Long> receiptIds = order.getMaterialReceiptId() == null
            ? List.of() : List.of(order.getMaterialReceiptId());
        return toOutboundVo(order, detail, receiptNos(receiptIds));
    }

    /** 列表按当前租户批量装配领料单号，避免逐条读取造成 N+1 查询。 */
    private Map<Long, String> receiptNos(List<Long> receiptIds) {
        if (receiptIds.isEmpty()) {
            return Map.of();
        }
        return receiptMapper.selectList(Wrappers.<MaterialReceipt>lambdaQuery()
                .eq(MaterialReceipt::getTenantId, infrastructure.tenantId())
                .in(MaterialReceipt::getMaterialReceiptId, receiptIds))
            .stream()
            .collect(Collectors.toMap(MaterialReceipt::getMaterialReceiptId, MaterialReceipt::getReceiptNo));
    }

    private OutboundVo toOutboundVo(OutboundOrder order, boolean detail, Map<Long, String> receiptNos) {
        OutboundVo vo = new OutboundVo();
        vo.setId(order.getOutboundOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setSource(order.getSource());
        vo.setStatus(order.getStatus());
        vo.setMaterialReceiptId(order.getMaterialReceiptId());
        vo.setReceiptNo(order.getMaterialReceiptId() == null ? null : receiptNos.get(order.getMaterialReceiptId()));
        vo.setTaskPackageId(order.getTaskPackageId());
        vo.setFarmItemId(order.getFarmItemId());
        vo.setReceiverName(order.getReceiverNameSnapshot());
        vo.setTaskName(order.getTaskNameSnapshot());
        vo.setFarmWorkName(order.getFarmWorkNameSnapshot());
        vo.setGreenhouseNames(order.getGreenhouseNamesSnapshot());
        vo.setBusinessDate(order.getBusinessDate());
        vo.setVersion(order.getVersion());
        vo.setCreateTime(order.getCreateTime());
        vo.setConfirmedAt(order.getConfirmedAt());
        vo.setRemark(order.getRemark());
        if (detail) {
            List<OutboundLine> lines = outboundLineMapper.selectList(Wrappers.<OutboundLine>lambdaQuery()
                .eq(OutboundLine::getTenantId, infrastructure.tenantId())
                .eq(OutboundLine::getOutboundOrderId, order.getOutboundOrderId())
                .orderByAsc(OutboundLine::getOutboundLineId));
            Map<Long, Balance> balances = lines.isEmpty() ? Map.of() : balanceMapper.selectList(Wrappers.<Balance>lambdaQuery()
                .eq(Balance::getTenantId, infrastructure.tenantId())
                .in(Balance::getMaterialId, lines.stream().map(OutboundLine::getMaterialId).toList()))
                .stream().collect(Collectors.toMap(Balance::getMaterialId, Function.identity()));
            vo.setLines(lines.stream().map(line -> toOutboundLineVo(line, balances.get(line.getMaterialId()))).toList());
        }
        return vo;
    }

    private OutboundLineVo toOutboundLineVo(OutboundLine line, Balance balance) {
        OutboundLineVo vo = new OutboundLineVo();
        vo.setId(line.getOutboundLineId());
        vo.setMaterialReceiptLineId(line.getMaterialReceiptLineId());
        vo.setInventoryMaterialId(line.getMaterialId());
        vo.setMaterialCode(line.getMaterialCodeSnapshot());
        vo.setMaterialName(line.getMaterialNameSnapshot());
        vo.setSpecification(line.getSpecificationSnapshot());
        vo.setUnit(line.getUnitSnapshot());
        vo.setRequestedQuantity(line.getRequestedQuantity());
        vo.setActualQuantity(line.getActualQuantity());
        vo.setAvailableQuantity(balance == null ? InventoryQuantity.ZERO : balance.getQuantity());
        vo.setDeletedByKeeper(line.getDeletedByKeeper());
        return vo;
    }

    private MaterialVo toMaterialVo(Material material, Balance balance) {
        MaterialVo vo = new MaterialVo();
        vo.setId(material.getMaterialId());
        vo.setMaterialCode(material.getMaterialCode());
        vo.setMaterialName(material.getMaterialName());
        vo.setCategoryId(material.getCategoryId());
        vo.setCategoryName(material.getCategory());
        vo.setCategory(material.getCategory());
        vo.setSpecification(material.getSpecification());
        vo.setUnit(material.getUnit());
        vo.setEnabled(material.getEnabled());
        vo.setVersion(material.getVersion());
        vo.setCreateTime(material.getCreateTime());
        vo.setUpdateTime(material.getUpdateTime());
        vo.setRemark(material.getRemark());
        vo.setQuantity(balance == null ? InventoryQuantity.ZERO : balance.getQuantity());
        vo.setAbnormalReason(balance == null ? null : balance.getAbnormalReason());
        vo.setOutboundLocked(balance != null && Boolean.TRUE.equals(balance.getOutboundLocked()));
        return vo;
    }

    private MaterialCategoryVo toMaterialCategoryVo(MaterialCategory category, long materialCount) {
        MaterialCategoryVo vo = new MaterialCategoryVo();
        vo.setId(category.getCategoryId());
        vo.setCategoryName(category.getCategoryName());
        vo.setSortOrder(category.getSortOrder());
        vo.setEnabled(category.getEnabled());
        vo.setMaterialCount(materialCount);
        vo.setVersion(category.getVersion());
        vo.setRemark(category.getRemark());
        return vo;
    }

    private LedgerVo toLedgerVo(Ledger ledger) {
        LedgerVo vo = new LedgerVo();
        vo.setId(ledger.getLedgerId());
        vo.setType(ledger.getLedgerType());
        vo.setBusinessSubtype(ledger.getBusinessSubtype());
        vo.setQuantity(ledger.getQuantityDelta());
        vo.setBalanceAfter(ledger.getBalanceAfter());
        vo.setRelatedOrderId(ledger.getRelatedOrderId());
        vo.setRelatedOrderNo(ledger.getRelatedOrderNo());
        vo.setRelatedOrderType(ledger.getRelatedOrderType());
        vo.setOperatorName(ledger.getOperatorNameSnapshot());
        vo.setOccurredAt(ledger.getOccurredAt());
        return vo;
    }

    private void copyMaterialFields(Material material, MaterialSaveBo bo, MaterialCategory category) {
        material.setMaterialCode(StrUtil.trim(bo.getMaterialCode()));
        material.setMaterialName(StrUtil.trim(bo.getMaterialName()));
        material.setCategoryId(category.getCategoryId());
        material.setCategory(category.getCategoryName());
        material.setSpecification(StrUtil.trim(bo.getSpecification()));
        material.setUnit(StrUtil.trim(bo.getUnit()));
        material.setEnabled(!Boolean.FALSE.equals(bo.getEnabled()));
        material.setRemark(StrUtil.trim(bo.getRemark()));
    }

    private void copyMaterialCategoryFields(MaterialCategory category, MaterialCategorySaveBo bo) {
        String name = StrUtil.trim(bo.getCategoryName());
        if (StrUtil.isBlank(name)) {
            throw InventoryBusinessException.rule("分类名称不能为空");
        }
        category.setCategoryName(name);
        category.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        category.setEnabled(!Boolean.FALSE.equals(bo.getEnabled()));
        category.setRemark(StrUtil.trim(bo.getRemark()));
    }

    private MaterialCategory resolveMaterialCategory(MaterialSaveBo bo, Long currentCategoryId) {
        String tenantId = infrastructure.tenantId();
        MaterialCategory category;
        if (bo.getCategoryId() != null) {
            category = materialCategoryMapper.selectTenantById(tenantId, bo.getCategoryId());
        } else if (StrUtil.isNotBlank(bo.getCategory())) {
            category = materialCategoryMapper.selectOne(Wrappers.<MaterialCategory>lambdaQuery()
                .eq(MaterialCategory::getTenantId, tenantId)
                .eq(MaterialCategory::getCategoryName, StrUtil.trim(bo.getCategory())));
        } else {
            category = null;
        }
        if (category == null) {
            throw InventoryBusinessException.rule("请选择有效的物料分类");
        }
        if (bo.getCategoryId() != null && StrUtil.isNotBlank(bo.getCategory())
            && !category.getCategoryName().equals(StrUtil.trim(bo.getCategory()))) {
            throw InventoryBusinessException.rule("物料分类ID与名称不一致");
        }
        boolean changed = currentCategoryId == null || !category.getCategoryId().equals(currentCategoryId);
        if (changed && !Boolean.TRUE.equals(category.getEnabled())) {
            throw InventoryBusinessException.rule("物料分类已停用，不能选择");
        }
        return category;
    }

    private MaterialCategory requireMaterialCategory(Long categoryId) {
        MaterialCategory category = materialCategoryMapper.selectTenantById(infrastructure.tenantId(), categoryId);
        if (category == null) {
            throw InventoryBusinessException.rule("物料分类不存在");
        }
        return category;
    }

    private Material cloneMaterial(Material source) {
        Material target = new Material();
        target.setMaterialId(source.getMaterialId());
        target.setTenantId(source.getTenantId());
        target.setMaterialCode(source.getMaterialCode());
        target.setMaterialName(source.getMaterialName());
        target.setCategoryId(source.getCategoryId());
        target.setCategory(source.getCategory());
        target.setSpecification(source.getSpecification());
        target.setUnit(source.getUnit());
        target.setEnabled(source.getEnabled());
        target.setVersion(source.getVersion());
        target.setDelFlag(source.getDelFlag());
        target.setRemark(source.getRemark());
        return target;
    }

    private MaterialCategory cloneMaterialCategory(MaterialCategory source) {
        MaterialCategory target = new MaterialCategory();
        target.setCategoryId(source.getCategoryId());
        target.setTenantId(source.getTenantId());
        target.setCategoryName(source.getCategoryName());
        target.setSortOrder(source.getSortOrder());
        target.setEnabled(source.getEnabled());
        target.setVersion(source.getVersion());
        target.setRemark(source.getRemark());
        return target;
    }

    private void requireVersion(Long current, Long request) {
        if (request == null || !request.equals(current)) {
            throw versionConflict();
        }
    }

    private InventoryBusinessException versionConflict() {
        return new InventoryBusinessException(InventoryConstants.ERROR_VERSION_CONFLICT,
            "数据已被其他人修改，请刷新后重试");
    }

    private void snapshot(InboundLine line, Material material) {
        line.setMaterialCodeSnapshot(material.getMaterialCode());
        line.setMaterialNameSnapshot(material.getMaterialName());
        line.setSpecificationSnapshot(material.getSpecification());
        line.setUnitSnapshot(material.getUnit());
    }

    private void snapshot(OutboundLine line, Material material) {
        line.setMaterialCodeSnapshot(material.getMaterialCode());
        line.setMaterialNameSnapshot(material.getMaterialName());
        line.setSpecificationSnapshot(material.getSpecification());
        line.setUnitSnapshot(material.getUnit());
    }

    private void snapshot(StocktakeLine line, Material material) {
        line.setMaterialCodeSnapshot(material.getMaterialCode());
        line.setMaterialNameSnapshot(material.getMaterialName());
        line.setSpecificationSnapshot(material.getSpecification());
        line.setUnitSnapshot(material.getUnit());
    }
}
