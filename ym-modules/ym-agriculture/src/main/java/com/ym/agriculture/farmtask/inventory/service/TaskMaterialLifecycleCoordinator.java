package com.ym.agriculture.farmtask.inventory.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AllocationLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AllocationMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.BalanceMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.OutboundOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.ReturnOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.TaskMaterialMapper;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Allocation;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AllocationLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Balance;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Material;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceipt;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceiptLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OutboundOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.TaskMaterial;
import com.ym.agriculture.farmtask.inventory.support.InventoryQuantity;
import com.ym.agriculture.farmtask.inventory.support.InventoryI18nResourceRegistrar;
import com.ym.agriculture.farmtask.inventory.support.MaterialAllocationCalculator;
import com.ym.agriculture.farmtask.inventory.support.MaterialAllocationCalculator.LeaderAllocation;
import com.ym.agriculture.farmtask.inventory.support.MaterialAllocationCalculator.LeaderWeight;
import com.ym.agriculture.farmtask.inventory.support.MaterialAllocationCalculator.MaterialTotal;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTaskMaterialBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.service.packagecmd.SfStaskPackageContentPlanner.NormalizedWorkItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务生命周期与库存领域之间的唯一协调入口。
 *
 * <p>任务包正式提交负责建立总量、拆分快照和待接单领料单；首次有效接单激活领料单；
 * 到岗开放出库申请；作废时保持既有拆分不重算，并在全部关联工单作废后联动作废未领用单据。</p>
 */
@Component
@RequiredArgsConstructor
public class TaskMaterialLifecycleCoordinator {

    private static final String ALGORITHM_VERSION = "V1";

    private final InventoryInfrastructure infrastructure;
    private final MaterialMapper materialMapper;
    private final BalanceMapper balanceMapper;
    private final TaskMaterialMapper taskMaterialMapper;
    private final AllocationMapper allocationMapper;
    private final AllocationLineMapper allocationLineMapper;
    private final MaterialReceiptMapper receiptMapper;
    private final MaterialReceiptLineMapper receiptLineMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final SfFarmWorkDictMapper farmWorkDictMapper;
    private final SfFieldMapper fieldMapper;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final MaterialAllocationCalculator allocationCalculator;
    private final InventoryI18nResourceRegistrar i18nRegistrar;

    /**
     * 替换任务包物料快照。正式提交时同时校验全包库存并固化组长拆分；草稿仅保存总量。
     */
    public void replacePackageMaterials(String tenantId, Long packageId, String creatorRoleCode,
                                        List<NormalizedWorkItem> normalizedItems, boolean submit) {
        if (StaskCreatorRole.PRODUCTION_ADMIN.equals(creatorRoleCode)) {
            deletePackageSnapshots(tenantId, packageId);
            return;
        }
        Map<Long, List<SfStaskTaskMaterialBo>> canonical = canonicalMaterials(normalizedItems);
        Map<Long, SfFarmWorkDict> farmItems = loadFarmItems(tenantId, canonical.keySet());
        Map<Long, LinkedHashMap<Long, MaterialDemand>> demands = new LinkedHashMap<>();
        for (Map.Entry<Long, List<SfStaskTaskMaterialBo>> entry : canonical.entrySet()) {
            Long farmItemId = entry.getKey();
            LinkedHashMap<Long, MaterialDemand> lines = normalizeDemands(entry.getValue());
            SfFarmWorkDict farmItem = farmItems.get(farmItemId);
            if (farmItem == null) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_TASK_MATERIAL_INVALID,
                    "农事项不存在或已停用");
            }
            if (!Boolean.TRUE.equals(farmItem.getRequiresMaterial()) && !lines.isEmpty()) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_TASK_MATERIAL_NOT_ALLOWED,
                    "农事项“" + farmItem.getDictName() + "”未启用领料，不能配置物资");
            }
            if (!lines.isEmpty()) {
                demands.put(farmItemId, lines);
            }
        }
        if (submit) {
            validatePackageStock(demands);
        }

        deletePackageSnapshots(tenantId, packageId);
        if (demands.isEmpty()) {
            return;
        }
        Date now = new Date();
        Map<ScopeMaterialKey, TaskMaterial> saved = persistTaskMaterials(
            tenantId, packageId, demands, now);
        if (submit) {
            persistAllocations(tenantId, packageId, normalizedItems, demands, saved, now);
        }
    }

    /** 技术员正式提交并完成拆单后，按任务包、农事项、组长建立待接单领料单。 */
    public void syncPendingReceiptsForPackage(String tenantId, Long packageId) {
        List<SfStaskWorkOrder> packageOrders = workOrderMapper.selectByPackageId(tenantId, packageId);
        Map<String, List<Long>> registeredReceiptIds = new LinkedHashMap<>();
        for (OrderScope scope : distinctScopes(packageOrders)) {
            syncPendingReceipt(scope, registeredReceiptIds);
        }
        registeredReceiptIds.forEach(i18nRegistrar::registerReceipts);
    }

    /** 首次有效接单激活待接单领料单；兼容变更前开发数据缺单时按原规则补建。 */
    public void ensureReceiptsForAcceptedOrders(Collection<SfStaskWorkOrder> acceptedOrders) {
        Map<String, List<Long>> createdReceiptIds = new LinkedHashMap<>();
        for (OrderScope scope : distinctScopes(acceptedOrders)) {
            MaterialReceipt existing = selectReceiptForUpdate(scope);
            if (existing != null) {
                activateReceiptOnLeaderAccept(existing);
                continue;
            }
            ReceiptSource source = loadReceiptSource(scope);
            if (source != null) {
                MaterialReceipt receipt = createReceipt(scope, source, InventoryConstants.RECEIPT_UNCLAIMED,
                    "CREATE_ON_LEADER_ACCEPT_COMPAT");
                createdReceiptIds.computeIfAbsent(scope.tenantId(), ignored -> new ArrayList<>())
                    .add(receipt.getMaterialReceiptId());
            }
        }
        createdReceiptIds.forEach(i18nRegistrar::registerReceipts);
    }

    /** 技术员撤回全部未接单任务时作废待接单领料单，供重新提交后按唯一键复用。 */
    public void onPackageWithdrawn(String tenantId, Long packageId) {
        List<MaterialReceipt> receipts = receiptMapper.selectList(Wrappers.<MaterialReceipt>lambdaQuery()
            .eq(MaterialReceipt::getTenantId, tenantId)
            .eq(MaterialReceipt::getTaskPackageId, packageId)
            .orderByAsc(MaterialReceipt::getMaterialReceiptId)
            .last("FOR UPDATE"));
        for (MaterialReceipt receipt : receipts) {
            if (InventoryConstants.RECEIPT_VOIDED.equals(receipt.getStatus())) {
                continue;
            }
            if (!InventoryConstants.RECEIPT_PENDING_ACCEPTANCE.equals(receipt.getStatus())) {
                throw InventoryBusinessException.rule("领料单已进入领用流程，不能撤回任务");
            }
            voidReceipt(receipt, "VOID_ON_TASK_WITHDRAW");
        }
    }

    private void syncPendingReceipt(OrderScope scope, Map<String, List<Long>> registeredReceiptIds) {
        ReceiptSource source = loadReceiptSource(scope);
        if (source == null) {
            return;
        }
        MaterialReceipt existing = selectReceiptForUpdate(scope);
        MaterialReceipt receipt;
        if (existing == null) {
            receipt = createReceipt(scope, source, InventoryConstants.RECEIPT_PENDING_ACCEPTANCE,
                "CREATE_ON_TASK_SUBMIT");
        } else if (InventoryConstants.RECEIPT_PENDING_ACCEPTANCE.equals(existing.getStatus())) {
            return;
        } else if (InventoryConstants.RECEIPT_VOIDED.equals(existing.getStatus())) {
            receipt = reactivateReceipt(scope, source, existing);
        } else {
            throw InventoryBusinessException.rule("领料单已进入领用流程，不能刷新任务物料");
        }
        registeredReceiptIds.computeIfAbsent(scope.tenantId(), ignored -> new ArrayList<>())
            .add(receipt.getMaterialReceiptId());
    }

    private ReceiptSource loadReceiptSource(OrderScope scope) {
        Allocation allocation = allocationMapper.selectOne(Wrappers.<Allocation>lambdaQuery()
            .eq(Allocation::getTenantId, scope.tenantId())
            .eq(Allocation::getTaskPackageId, scope.packageId())
            .eq(Allocation::getFarmItemId, scope.farmItemId())
            .eq(Allocation::getLeaderEmployeeId, scope.leaderId())
            .last("FOR UPDATE"));
        if (allocation == null) {
            return null;
        }
        List<AllocationLine> allocationLines = allocationLineMapper.selectList(
            Wrappers.<AllocationLine>lambdaQuery()
                .eq(AllocationLine::getTenantId, scope.tenantId())
                .eq(AllocationLine::getAllocationId, allocation.getAllocationId())
                .orderByAsc(AllocationLine::getMaterialId));
        if (allocationLines.isEmpty()) {
            return null;
        }
        Map<Long, TaskMaterial> taskMaterials = taskMaterialMapper.selectBatchIds(allocationLines.stream()
                .map(AllocationLine::getTaskMaterialId).filter(Objects::nonNull).toList())
            .stream().collect(Collectors.toMap(TaskMaterial::getTaskMaterialId, Function.identity()));
        if (taskMaterials.size() != allocationLines.size()) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_TASK_MATERIAL_INVALID,
                "任务物料拆分快照不完整");
        }
        return new ReceiptSource(allocation, allocationLines, taskMaterials,
            taskPackageMapper.selectById(scope.packageId()));
    }

    private MaterialReceipt createReceipt(OrderScope scope, ReceiptSource source, String status, String auditAction) {
        Date now = new Date();
        MaterialReceipt receipt = new MaterialReceipt();
        receipt.setMaterialReceiptId(IdWorker.getId());
        receipt.setTenantId(scope.tenantId());
        receipt.setReceiptNo(infrastructure.nextOrderNo("RECEIPT"));
        receipt.setTaskPackageId(scope.packageId());
        receipt.setFarmItemId(scope.farmItemId());
        receipt.setLeaderEmployeeId(scope.leaderId());
        applyReceiptSnapshot(receipt, scope, source);
        receipt.setStatus(status);
        receipt.setArrived(false);
        receipt.setVersion(0L);
        receipt.setCreateBy(infrastructure.operatorId());
        receipt.setCreateTime(now);
        receipt.setUpdateBy(infrastructure.operatorId());
        receipt.setUpdateTime(now);
        if (receiptMapper.insert(receipt) != 1) {
            throw InventoryBusinessException.rule("领料单创建失败");
        }
        insertReceiptLines(receipt, source, now);
        infrastructure.audit("MATERIAL_RECEIPT", receipt.getMaterialReceiptId(), auditAction,
            null, null, receipt);
        return receipt;
    }

    private MaterialReceipt reactivateReceipt(OrderScope scope, ReceiptSource source, MaterialReceipt receipt) {
        if (hasReceiptBusinessFacts(receipt)) {
            throw InventoryBusinessException.rule("领料单已有出库或退库记录，不能通过任务重提刷新");
        }
        Map<String, Object> before = Map.of(
            "status", receipt.getStatus(),
            "version", receipt.getVersion());
        receiptLineMapper.delete(Wrappers.<MaterialReceiptLine>lambdaQuery()
            .eq(MaterialReceiptLine::getTenantId, scope.tenantId())
            .eq(MaterialReceiptLine::getMaterialReceiptId, receipt.getMaterialReceiptId()));
        applyReceiptSnapshot(receipt, scope, source);
        receipt.setStatus(InventoryConstants.RECEIPT_PENDING_ACCEPTANCE);
        receipt.setArrived(false);
        receipt.setUpdateBy(infrastructure.operatorId());
        Date now = new Date();
        receipt.setUpdateTime(now);
        updateReceipt(receipt);
        insertReceiptLines(receipt, source, now);
        infrastructure.audit("MATERIAL_RECEIPT", receipt.getMaterialReceiptId(),
            "REACTIVATE_ON_TASK_RESUBMIT", null, before, receipt);
        return receipt;
    }

    private void activateReceiptOnLeaderAccept(MaterialReceipt receipt) {
        if (InventoryConstants.RECEIPT_PENDING_ACCEPTANCE.equals(receipt.getStatus())) {
            Map<String, Object> before = Map.of(
                "status", receipt.getStatus(),
                "version", receipt.getVersion());
            receipt.setStatus(InventoryConstants.RECEIPT_UNCLAIMED);
            receipt.setUpdateBy(infrastructure.operatorId());
            receipt.setUpdateTime(new Date());
            updateReceipt(receipt);
            infrastructure.audit("MATERIAL_RECEIPT", receipt.getMaterialReceiptId(),
                "ACTIVATE_ON_LEADER_ACCEPT", null, before, receipt);
            return;
        }
        if (InventoryConstants.RECEIPT_VOIDED.equals(receipt.getStatus())) {
            throw InventoryBusinessException.rule("领料单已作废，不能完成组长接单");
        }
    }

    private void applyReceiptSnapshot(MaterialReceipt receipt, OrderScope scope, ReceiptSource source) {
        receipt.setLeaderNameSnapshot(defaultText(source.allocation().getLeaderNameSnapshot(), scope.leaderId()));
        SfStaskTaskPackage taskPackage = source.taskPackage();
        receipt.setTaskNameSnapshot(taskPackage == null ? String.valueOf(scope.packageId())
            : taskPackage.getPackageNo());
        receipt.setFarmWorkNameSnapshot(defaultText(scope.farmWorkName(), scope.farmItemId()));
        receipt.setGreenhouseNamesSnapshot(source.allocation().getGreenhouseNamesSnapshot());
    }

    private void insertReceiptLines(MaterialReceipt receipt, ReceiptSource source, Date now) {
        for (AllocationLine allocationLine : source.allocationLines()) {
            TaskMaterial taskMaterial = source.taskMaterials().get(allocationLine.getTaskMaterialId());
            if (taskMaterial == null) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_TASK_MATERIAL_INVALID,
                    "任务物料拆分快照不完整");
            }
            MaterialReceiptLine line = new MaterialReceiptLine();
            line.setMaterialReceiptLineId(IdWorker.getId());
            line.setTenantId(receipt.getTenantId());
            line.setMaterialReceiptId(receipt.getMaterialReceiptId());
            line.setMaterialId(taskMaterial.getMaterialId());
            line.setMaterialCodeSnapshot(taskMaterial.getMaterialCodeSnapshot());
            line.setMaterialNameSnapshot(taskMaterial.getMaterialNameSnapshot());
            line.setSpecificationSnapshot(taskMaterial.getSpecificationSnapshot());
            line.setUnitSnapshot(taskMaterial.getUnitSnapshot());
            line.setRequestedQuantity(allocationLine.getAllocatedQuantity());
            line.setActualQuantity(InventoryQuantity.ZERO);
            line.setReturnedQuantity(InventoryQuantity.ZERO);
            line.setVersion(0L);
            line.setCreateTime(now);
            line.setUpdateTime(now);
            receiptLineMapper.insert(line);
        }
    }

    private boolean hasReceiptBusinessFacts(MaterialReceipt receipt) {
        return outboundOrderMapper.exists(Wrappers.<OutboundOrder>lambdaQuery()
            .eq(OutboundOrder::getTenantId, receipt.getTenantId())
            .eq(OutboundOrder::getMaterialReceiptId, receipt.getMaterialReceiptId()))
            || returnOrderMapper.exists(Wrappers.<ReturnOrder>lambdaQuery()
            .eq(ReturnOrder::getTenantId, receipt.getTenantId())
            .eq(ReturnOrder::getMaterialReceiptId, receipt.getMaterialReceiptId()));
    }

    /** 任一关联工单到岗即开放该领料分组的出库申请。 */
    public void markArrivedForOrders(Collection<SfStaskWorkOrder> arrivedOrders) {
        for (OrderScope scope : distinctScopes(arrivedOrders)) {
            MaterialReceipt receipt = selectReceiptForUpdate(scope);
            if (receipt == null || Boolean.TRUE.equals(receipt.getArrived())
                || InventoryConstants.RECEIPT_VOIDED.equals(receipt.getStatus())) {
                continue;
            }
            receipt.setArrived(true);
            receipt.setUpdateBy(infrastructure.operatorId());
            receipt.setUpdateTime(new Date());
            updateReceipt(receipt);
        }
    }

    /** 已存在有效出库时禁止作废关联任务。 */
    public void assertCanVoidOrders(Collection<SfStaskWorkOrder> orders) {
        for (OrderScope scope : distinctScopes(orders)) {
            MaterialReceipt receipt = selectReceipt(scope);
            if (receipt != null && outboundOrderMapper.exists(Wrappers.<OutboundOrder>lambdaQuery()
                .eq(OutboundOrder::getTenantId, scope.tenantId())
                .eq(OutboundOrder::getMaterialReceiptId, receipt.getMaterialReceiptId())
                .eq(OutboundOrder::getStatus, InventoryConstants.STATUS_COMPLETED))) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_TASK_VOID_OUTBOUND_EXISTS,
                    "关联物资已经出库，不能作废任务");
            }
        }
    }

    /** 包级作废前的出库保护。 */
    public void assertCanVoidPackage(String tenantId, Long packageId) {
        if (outboundOrderMapper.exists(Wrappers.<OutboundOrder>lambdaQuery()
            .eq(OutboundOrder::getTenantId, tenantId)
            .eq(OutboundOrder::getTaskPackageId, packageId)
            .eq(OutboundOrder::getStatus, InventoryConstants.STATUS_COMPLETED))) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_TASK_VOID_OUTBOUND_EXISTS,
                "任务包存在已出库物资，不能作废");
        }
    }

    /** 单条或部分工单作废后，仅在同一领料分组全部终止时联动作废。 */
    public void onOrdersVoided(Collection<SfStaskWorkOrder> orders) {
        for (OrderScope scope : distinctScopes(orders)) {
            List<SfStaskWorkOrder> packageOrders = workOrderMapper.selectByPackageId(
                scope.tenantId(), scope.packageId());
            boolean allVoided = packageOrders.stream()
                .filter(order -> Objects.equals(order.getWorkItemId(), scope.farmItemId())
                    && Objects.equals(order.getLeaderId(), scope.leaderId()))
                .allMatch(order -> StaskOrderStatus.VOIDED.equals(order.getStatus())
                    || StaskOrderStatus.CANCELLED.equals(order.getStatus()));
            if (allVoided) {
                voidReceiptScope(scope);
            }
        }
    }

    /** 整包作废后关闭全部未领用领料单与有效待出库单。 */
    public void onPackageVoided(String tenantId, Long packageId) {
        List<MaterialReceipt> receipts = receiptMapper.selectList(Wrappers.<MaterialReceipt>lambdaQuery()
            .eq(MaterialReceipt::getTenantId, tenantId)
            .eq(MaterialReceipt::getTaskPackageId, packageId));
        for (MaterialReceipt receipt : receipts) {
            voidReceipt(receipt);
        }
    }

    /** 删除可编辑任务包时清理尚未形成业务单据的物料快照。 */
    public void deletePackageSnapshots(String tenantId, Long packageId) {
        List<Long> allocationIds = allocationMapper.selectList(Wrappers.<Allocation>lambdaQuery()
                .eq(Allocation::getTenantId, tenantId)
                .eq(Allocation::getTaskPackageId, packageId))
            .stream().map(Allocation::getAllocationId).toList();
        if (!allocationIds.isEmpty()) {
            allocationLineMapper.delete(Wrappers.<AllocationLine>lambdaQuery()
                .eq(AllocationLine::getTenantId, tenantId)
                .in(AllocationLine::getAllocationId, allocationIds));
        }
        allocationMapper.delete(Wrappers.<Allocation>lambdaQuery()
            .eq(Allocation::getTenantId, tenantId)
            .eq(Allocation::getTaskPackageId, packageId));
        taskMaterialMapper.delete(Wrappers.<TaskMaterial>lambdaQuery()
            .eq(TaskMaterial::getTenantId, tenantId)
            .eq(TaskMaterial::getTaskPackageId, packageId));
    }

    private Map<Long, List<SfStaskTaskMaterialBo>> canonicalMaterials(List<NormalizedWorkItem> items) {
        Map<Long, List<SfStaskTaskMaterialBo>> result = new LinkedHashMap<>();
        for (NormalizedWorkItem normalized : CollUtil.emptyIfNull(items)) {
            if (normalized != null && normalized.workItem() != null
                && normalized.workItem().getWorkItemId() != null) {
                result.putIfAbsent(normalized.workItem().getWorkItemId(),
                    CollUtil.emptyIfNull(normalized.workItem().getMaterials()));
            }
        }
        return result;
    }

    private Map<Long, SfFarmWorkDict> loadFarmItems(String tenantId, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return farmWorkDictMapper.selectList(Wrappers.<SfFarmWorkDict>lambdaQuery()
                .eq(SfFarmWorkDict::getTenantId, tenantId)
                .in(SfFarmWorkDict::getDictId, ids))
            .stream().collect(Collectors.toMap(SfFarmWorkDict::getDictId, Function.identity(), (a, b) -> a));
    }

    private LinkedHashMap<Long, MaterialDemand> normalizeDemands(List<SfStaskTaskMaterialBo> requests) {
        LinkedHashMap<Long, MaterialDemand> result = new LinkedHashMap<>();
        for (SfStaskTaskMaterialBo request : CollUtil.emptyIfNull(requests)) {
            if (request == null || request.getInventoryMaterialId() == null) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_TASK_MATERIAL_INVALID,
                    "物资ID不能为空");
            }
            BigDecimal quantity = InventoryQuantity.scale(request.getQuantity(), "物资数量");
            if (quantity.compareTo(InventoryQuantity.ZERO) < 0) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_TASK_MATERIAL_INVALID,
                    "物资数量不能小于0");
            }
            if (quantity.compareTo(InventoryQuantity.ZERO) == 0) {
                continue;
            }
            Material material = infrastructure.requireEnabledMaterial(request.getInventoryMaterialId());
            if (result.putIfAbsent(material.getMaterialId(), new MaterialDemand(material, quantity)) != null) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_TASK_MATERIAL_INVALID,
                    "同一农事项不能重复选择同一物资");
            }
        }
        return result;
    }

    private void validatePackageStock(Map<Long, LinkedHashMap<Long, MaterialDemand>> demands) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        demands.values().forEach(lines -> lines.forEach((materialId, demand) ->
            totals.merge(materialId, demand.quantity(), BigDecimal::add)));
        if (totals.isEmpty()) {
            return;
        }
        Map<Long, Balance> balances = balanceMapper.selectList(Wrappers.<Balance>lambdaQuery()
                .eq(Balance::getTenantId, infrastructure.tenantId())
                .in(Balance::getMaterialId, totals.keySet()))
            .stream().collect(Collectors.toMap(Balance::getMaterialId, Function.identity()));
        List<InventoryBusinessException.LineError> errors = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : totals.entrySet()) {
            Balance balance = balances.get(entry.getKey());
            BigDecimal available = balance == null ? InventoryQuantity.ZERO : balance.getQuantity();
            if (balance != null && Boolean.TRUE.equals(balance.getOutboundLocked())) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_INVENTORY_LOCKED,
                    "任务物资包含异常锁定物资，不能提交");
            }
            if (available.compareTo(entry.getValue()) < 0) {
                errors.add(new InventoryBusinessException.LineError(null, entry.getKey(),
                    InventoryConstants.ERROR_INVENTORY_INSUFFICIENT, "账面库存不足",
                    available.toPlainString(), null));
            }
        }
        if (!errors.isEmpty()) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_INVENTORY_INSUFFICIENT,
                409, "任务物资总量超过当前账面库存", errors);
        }
    }

    private Map<ScopeMaterialKey, TaskMaterial> persistTaskMaterials(String tenantId, Long packageId,
        Map<Long, LinkedHashMap<Long, MaterialDemand>> demands, Date now) {
        Map<ScopeMaterialKey, TaskMaterial> result = new LinkedHashMap<>();
        for (Map.Entry<Long, LinkedHashMap<Long, MaterialDemand>> scope : demands.entrySet()) {
            for (MaterialDemand demand : scope.getValue().values()) {
                Material material = demand.material();
                TaskMaterial row = new TaskMaterial();
                row.setTaskMaterialId(IdWorker.getId());
                row.setTenantId(tenantId);
                row.setTaskPackageId(packageId);
                row.setFarmItemId(scope.getKey());
                row.setMaterialId(material.getMaterialId());
                row.setMaterialCodeSnapshot(material.getMaterialCode());
                row.setMaterialNameSnapshot(material.getMaterialName());
                row.setSpecificationSnapshot(material.getSpecification());
                row.setUnitSnapshot(material.getUnit());
                row.setTotalQuantity(demand.quantity());
                row.setCreateTime(now);
                row.setUpdateTime(now);
                taskMaterialMapper.insert(row);
                result.put(new ScopeMaterialKey(scope.getKey(), material.getMaterialId()), row);
            }
        }
        return result;
    }

    private void persistAllocations(String tenantId, Long packageId, List<NormalizedWorkItem> normalizedItems,
        Map<Long, LinkedHashMap<Long, MaterialDemand>> demands,
        Map<ScopeMaterialKey, TaskMaterial> taskMaterials, Date now) {
        Map<Long, String> greenhouseNames = loadGreenhouseNames(tenantId, normalizedItems);
        for (Map.Entry<Long, LinkedHashMap<Long, MaterialDemand>> scope : demands.entrySet()) {
            Long farmItemId = scope.getKey();
            List<LeaderWeight> leaders = leaderWeights(farmItemId, normalizedItems, greenhouseNames);
            List<MaterialTotal> materials = scope.getValue().entrySet().stream()
                .map(entry -> new MaterialTotal(entry.getKey(), entry.getValue().quantity())).toList();
            List<LeaderAllocation> allocations = allocationCalculator.allocate(materials, leaders);
            for (LeaderAllocation calculated : allocations) {
                Allocation allocation = new Allocation();
                allocation.setAllocationId(IdWorker.getId());
                allocation.setTenantId(tenantId);
                allocation.setTaskPackageId(packageId);
                allocation.setFarmItemId(farmItemId);
                allocation.setLeaderEmployeeId(calculated.leaderEmployeeId());
                allocation.setLeaderNameSnapshot(defaultText(calculated.leaderName(), calculated.leaderEmployeeId()));
                allocation.setGreenhouseCount(calculated.greenhouseCount());
                allocation.setLeaderOrder(calculated.order());
                allocation.setGreenhouseNamesSnapshot(String.join("、", calculated.greenhouseNames()));
                allocation.setAlgorithmVersion(ALGORITHM_VERSION);
                allocation.setCreateTime(now);
                allocation.setUpdateTime(now);
                allocationMapper.insert(allocation);
                for (MaterialAllocationCalculator.MaterialAllocation lineResult : calculated.materials()) {
                    TaskMaterial taskMaterial = taskMaterials.get(
                        new ScopeMaterialKey(farmItemId, lineResult.inventoryMaterialId()));
                    AllocationLine line = new AllocationLine();
                    line.setAllocationLineId(IdWorker.getId());
                    line.setTenantId(tenantId);
                    line.setAllocationId(allocation.getAllocationId());
                    line.setTaskMaterialId(taskMaterial.getTaskMaterialId());
                    line.setMaterialId(lineResult.inventoryMaterialId());
                    line.setAllocatedQuantity(lineResult.quantity());
                    line.setCreateTime(now);
                    line.setUpdateTime(now);
                    allocationLineMapper.insert(line);
                }
            }
        }
    }

    private Map<Long, String> loadGreenhouseNames(String tenantId, List<NormalizedWorkItem> items) {
        List<Long> ids = CollUtil.emptyIfNull(items).stream()
            .filter(Objects::nonNull).map(NormalizedWorkItem::workItem).filter(Objects::nonNull)
            .flatMap(item -> CollUtil.emptyIfNull(item.getGreenhouseIds()).stream())
            .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return fieldMapper.selectList(Wrappers.<SfField>lambdaQuery()
                .eq(SfField::getTenantId, tenantId).in(SfField::getFieldId, ids))
            .stream().collect(Collectors.toMap(SfField::getFieldId, SfField::getFieldName, (a, b) -> a));
    }

    private List<LeaderWeight> leaderWeights(Long farmItemId, List<NormalizedWorkItem> normalizedItems,
                                              Map<Long, String> greenhouseNames) {
        LinkedHashMap<Long, LeaderAccumulator> accumulators = new LinkedHashMap<>();
        for (NormalizedWorkItem normalized : CollUtil.emptyIfNull(normalizedItems)) {
            if (normalized == null || normalized.workItem() == null
                || !Objects.equals(farmItemId, normalized.workItem().getWorkItemId())) {
                continue;
            }
            if (normalized.leaderId() == null) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_TASK_MATERIAL_INVALID,
                    "物料拆分前必须完成全部大棚的组长分配");
            }
            LeaderAccumulator accumulator = accumulators.computeIfAbsent(normalized.leaderId(),
                ignored -> new LeaderAccumulator(normalized.leaderId(), normalized.leaderName(), accumulators.size()));
            for (Long greenhouseId : CollUtil.emptyIfNull(normalized.workItem().getGreenhouseIds())) {
                if (greenhouseId != null && accumulator.greenhouseIds().add(greenhouseId)) {
                    accumulator.greenhouseNames().add(defaultText(greenhouseNames.get(greenhouseId), greenhouseId));
                }
            }
        }
        return accumulators.values().stream()
            .map(item -> new LeaderWeight(item.leaderId(), item.leaderName(), item.greenhouseIds().size(),
                item.order(), List.copyOf(item.greenhouseNames())))
            .toList();
    }

    private List<OrderScope> distinctScopes(Collection<SfStaskWorkOrder> orders) {
        Map<String, OrderScope> scopes = new LinkedHashMap<>();
        for (SfStaskWorkOrder order : (orders == null ? List.<SfStaskWorkOrder>of() : orders)) {
            if (order == null || order.getPackageId() == null || order.getWorkItemId() == null
                || order.getLeaderId() == null) {
                continue;
            }
            OrderScope scope = new OrderScope(order.getTenantId(), order.getPackageId(),
                order.getWorkItemId(), order.getLeaderId(), order.getWorkItemNameSnapshot());
            scopes.putIfAbsent(scope.key(), scope);
        }
        return scopes.values().stream()
            .sorted(Comparator.comparing(OrderScope::packageId)
                .thenComparing(OrderScope::farmItemId).thenComparing(OrderScope::leaderId))
            .toList();
    }

    private MaterialReceipt selectReceipt(OrderScope scope) {
        return receiptMapper.selectOne(Wrappers.<MaterialReceipt>lambdaQuery()
            .eq(MaterialReceipt::getTenantId, scope.tenantId())
            .eq(MaterialReceipt::getTaskPackageId, scope.packageId())
            .eq(MaterialReceipt::getFarmItemId, scope.farmItemId())
            .eq(MaterialReceipt::getLeaderEmployeeId, scope.leaderId()));
    }

    private MaterialReceipt selectReceiptForUpdate(OrderScope scope) {
        return receiptMapper.selectOne(Wrappers.<MaterialReceipt>lambdaQuery()
            .eq(MaterialReceipt::getTenantId, scope.tenantId())
            .eq(MaterialReceipt::getTaskPackageId, scope.packageId())
            .eq(MaterialReceipt::getFarmItemId, scope.farmItemId())
            .eq(MaterialReceipt::getLeaderEmployeeId, scope.leaderId())
            .last("FOR UPDATE"));
    }

    private void voidReceiptScope(OrderScope scope) {
        MaterialReceipt receipt = selectReceiptForUpdate(scope);
        if (receipt != null) {
            voidReceipt(receipt);
        }
    }

    private void voidReceipt(MaterialReceipt receipt) {
        voidReceipt(receipt, "VOID_WITH_TASK");
    }

    private void voidReceipt(MaterialReceipt receipt, String auditAction) {
        if (InventoryConstants.RECEIPT_VOIDED.equals(receipt.getStatus())) {
            return;
        }
        List<OutboundOrder> pending = outboundOrderMapper.selectList(Wrappers.<OutboundOrder>lambdaQuery()
            .eq(OutboundOrder::getTenantId, receipt.getTenantId())
            .eq(OutboundOrder::getMaterialReceiptId, receipt.getMaterialReceiptId())
            .eq(OutboundOrder::getStatus, InventoryConstants.STATUS_PENDING)
            .last("FOR UPDATE"));
        Date now = new Date();
        for (OutboundOrder order : pending) {
            order.setStatus(InventoryConstants.STATUS_CANCELLED);
            order.setCancelledBy(infrastructure.operatorId());
            order.setCancelledAt(now);
            order.setUpdateBy(infrastructure.operatorId());
            order.setUpdateTime(now);
            outboundOrderMapper.updateById(order);
        }
        receipt.setStatus(InventoryConstants.RECEIPT_VOIDED);
        receipt.setUpdateBy(infrastructure.operatorId());
        receipt.setUpdateTime(now);
        updateReceipt(receipt);
        infrastructure.audit("MATERIAL_RECEIPT", receipt.getMaterialReceiptId(),
            auditAction, null, null, receipt);
    }

    private void updateReceipt(MaterialReceipt receipt) {
        if (receiptMapper.updateById(receipt) != 1) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_VERSION_CONFLICT,
                "领料单已发生变化，请刷新后重试");
        }
    }

    private static String defaultText(String value, Object fallback) {
        return StrUtil.isNotBlank(value) ? value : String.valueOf(fallback);
    }

    private record MaterialDemand(Material material, BigDecimal quantity) {
    }

    private record ScopeMaterialKey(Long farmItemId, Long materialId) {
    }

    private record ReceiptSource(Allocation allocation, List<AllocationLine> allocationLines,
                                 Map<Long, TaskMaterial> taskMaterials, SfStaskTaskPackage taskPackage) {
    }

    private record LeaderAccumulator(Long leaderId, String leaderName, int order,
                                     Set<Long> greenhouseIds, List<String> greenhouseNames) {
        private LeaderAccumulator(Long leaderId, String leaderName, int order) {
            this(leaderId, leaderName, order, new LinkedHashSet<>(), new ArrayList<>());
        }
    }

    private record OrderScope(String tenantId, Long packageId, Long farmItemId,
                              Long leaderId, String farmWorkName) {
        private String key() {
            return tenantId + ":" + packageId + ":" + farmItemId + ":" + leaderId;
        }
    }
}
