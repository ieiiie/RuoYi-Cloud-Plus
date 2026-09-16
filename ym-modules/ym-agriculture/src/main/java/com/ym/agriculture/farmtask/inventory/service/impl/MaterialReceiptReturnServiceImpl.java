package com.ym.agriculture.farmtask.inventory.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.ReturnLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.ReturnOrderMapper;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Material;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceipt;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceiptLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.DirectReturnSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.DocumentLineBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OrderQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReceiptApplyBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReceiptLineVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReceiptVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnConfirmBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnConfirmLineBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnCreateBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnUpdateBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnLineVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnRequestLineBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnVo;
import com.ym.agriculture.farmtask.inventory.service.IInventoryService;
import com.ym.agriculture.farmtask.inventory.service.IMaterialReceiptReturnService;
import com.ym.agriculture.farmtask.inventory.service.InventoryInfrastructure;
import com.ym.agriculture.farmtask.inventory.support.InventoryQuantity;
import com.ym.agriculture.farmtask.inventory.support.InventoryI18nResourceRegistrar;
import com.ym.agriculture.farmtask.inventory.support.MaterialReceiptTechnicianAssembler;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialReceiptReturnServiceImpl implements IMaterialReceiptReturnService {

    private final InventoryInfrastructure infrastructure;
    private final IInventoryService inventoryService;
    private final MaterialMapper materialMapper;
    private final MaterialReceiptMapper receiptMapper;
    private final MaterialReceiptLineMapper receiptLineMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final ReturnLineMapper returnLineMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final MaterialReceiptTechnicianAssembler receiptTechnicianAssembler;
    private final InventoryI18nResourceRegistrar i18nRegistrar;

    @Override
    public PageResult<ReceiptVo> pageDirectOutboundReceipts(OrderQuery query, PageQuery pageQuery) {
        Page<MaterialReceipt> page = receiptMapper.selectPage(pageQuery.build(), Wrappers.<MaterialReceipt>lambdaQuery()
            .eq(MaterialReceipt::getTenantId, infrastructure.tenantId())
            .eq(MaterialReceipt::getArrived, true)
            .eq(MaterialReceipt::getStatus, InventoryConstants.RECEIPT_UNCLAIMED)
            .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper.like(MaterialReceipt::getReceiptNo, query.getKeyword())
                .or().like(MaterialReceipt::getTaskNameSnapshot, query.getKeyword())
                .or().like(MaterialReceipt::getFarmWorkNameSnapshot, query.getKeyword())
                .or().like(MaterialReceipt::getLeaderNameSnapshot, query.getKeyword()))
            .orderByDesc(MaterialReceipt::getCreateTime).orderByDesc(MaterialReceipt::getMaterialReceiptId));
        Page<ReceiptVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        Map<Long, SfStaskTaskPackage> taskPackages = receiptTaskPackages(page.getRecords());
        result.setRecords(page.getRecords().stream()
            .map(item -> toReceiptVo(item, taskPackages.get(item.getTaskPackageId()), false))
            .toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public ReceiptVo getDirectOutboundReceipt(Long id) {
        MaterialReceipt receipt = receiptMapper.selectOne(Wrappers.<MaterialReceipt>lambdaQuery()
            .eq(MaterialReceipt::getTenantId, infrastructure.tenantId())
            .eq(MaterialReceipt::getMaterialReceiptId, id)
            .eq(MaterialReceipt::getArrived, true)
            .eq(MaterialReceipt::getStatus, InventoryConstants.RECEIPT_UNCLAIMED));
        if (receipt == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404,
                "领料单不存在或当前状态不能直接出库");
        }
        SfStaskTaskPackage taskPackage = receiptTaskPackages(List.of(receipt)).get(receipt.getTaskPackageId());
        return toReceiptVo(receipt, taskPackage, true);
    }

    @Override
    public PageResult<ReceiptVo> pageReceipts(OrderQuery query, PageQuery pageQuery,
                                                 String roleCode, Long employeeId) {
        Set<Long> packageIds = scopedPackageIds(roleCode, employeeId);
        if (isTechnician(roleCode) && packageIds.isEmpty()) {
            return com.ym.agriculture.shared.common.AgriculturePageResults.build(new Page<ReceiptVo>(pageQuery.getPageNum(), pageQuery.getPageSize(), 0));
        }
        Page<MaterialReceipt> page = receiptMapper.selectPage(pageQuery.build(), Wrappers.<MaterialReceipt>lambdaQuery()
            .eq(MaterialReceipt::getTenantId, infrastructure.tenantId())
            .eq(EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(roleCode),
                MaterialReceipt::getLeaderEmployeeId, employeeId)
            .in(isTechnician(roleCode), MaterialReceipt::getTaskPackageId, packageIds)
            .eq(StrUtil.isNotBlank(query.getStatus()), MaterialReceipt::getStatus, query.getStatus())
            .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper.like(MaterialReceipt::getReceiptNo, query.getKeyword())
                .or().like(MaterialReceipt::getTaskNameSnapshot, query.getKeyword())
                .or().like(MaterialReceipt::getFarmWorkNameSnapshot, query.getKeyword()))
            .orderByDesc(MaterialReceipt::getCreateTime).orderByDesc(MaterialReceipt::getMaterialReceiptId));
        Page<ReceiptVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        Map<Long, SfStaskTaskPackage> taskPackages = receiptTaskPackages(page.getRecords());
        result.setRecords(page.getRecords().stream()
            .map(item -> toReceiptVo(item, taskPackages.get(item.getTaskPackageId()), false))
            .toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public ReceiptVo getReceipt(Long id, String roleCode, Long employeeId) {
        MaterialReceipt receipt = requireReceipt(id);
        ensureReceiptScope(receipt, roleCode, employeeId);
        SfStaskTaskPackage taskPackage = receiptTaskPackages(List.of(receipt)).get(receipt.getTaskPackageId());
        return toReceiptVo(receipt, taskPackage, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo applyOutbound(Long id, ReceiptApplyBo bo, Long employeeId) {
        return infrastructure.idempotent("RECEIPT_APPLY_OUTBOUND", bo.getIdempotencyKey(), Map.of("id", id, "request", bo), () -> {
            MaterialReceipt receipt = receiptMapper.selectForUpdate(infrastructure.tenantId(), id);
            if (receipt == null) {
                throw notFound("领料单不存在");
            }
            if (!receipt.getLeaderEmployeeId().equals(employeeId)) {
                throw forbidden();
            }
            requireVersion(receipt.getVersion(), bo.getVersion());
            return inventoryService.createPendingOutboundFromReceipt(id);
        });
    }

    @Override
    public PageResult<ReturnVo> pageReturns(OrderQuery query, PageQuery pageQuery,
                                               String roleCode, Long employeeId) {
        Set<Long> packageIds = scopedPackageIds(roleCode, employeeId);
        if (isTechnician(roleCode) && packageIds.isEmpty()) {
            return com.ym.agriculture.shared.common.AgriculturePageResults.build(new Page<ReturnVo>(pageQuery.getPageNum(), pageQuery.getPageSize(), 0));
        }
        Page<ReturnOrder> page = returnOrderMapper.selectPage(pageQuery.build(), Wrappers.<ReturnOrder>lambdaQuery()
            .eq(ReturnOrder::getTenantId, infrastructure.tenantId())
            .eq(EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(roleCode), ReturnOrder::getLeaderEmployeeId, employeeId)
            .in(isTechnician(roleCode), ReturnOrder::getTaskPackageId, packageIds)
            .eq(StrUtil.isNotBlank(query.getStatus()), ReturnOrder::getStatus, query.getStatus())
            .eq(StrUtil.isNotBlank(query.getSource()), ReturnOrder::getSource, query.getSource())
            .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper.like(ReturnOrder::getOrderNo, query.getKeyword())
                .or().like(ReturnOrder::getReturnerNameSnapshot, query.getKeyword())
                .or().like(ReturnOrder::getTaskNameSnapshot, query.getKeyword()))
            .ge(query.getBeginDate() != null, ReturnOrder::getBusinessDate, query.getBeginDate())
            .lt(query.getEndDate() != null, ReturnOrder::getBusinessDate,
                query.getEndDate() == null ? null : DateUtil.offsetDay(query.getEndDate(), 1))
            .orderByDesc(ReturnOrder::getCreateTime).orderByDesc(ReturnOrder::getReturnOrderId));
        Page<ReturnVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(item -> toReturnVo(item, false)).toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public ReturnVo getReturn(Long id, String roleCode, Long employeeId) {
        ReturnOrder order = requireReturn(id);
        ensureReturnScope(order, roleCode, employeeId);
        return toReturnVo(order, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo createReturn(ReturnCreateBo bo, Long leaderEmployeeId) {
        return infrastructure.idempotent("RETURN_CREATE", bo.getIdempotencyKey(), bo,
            () -> doCreateReturn(bo, leaderEmployeeId));
    }

    private MutationVo doCreateReturn(ReturnCreateBo bo, Long leaderEmployeeId) {
        MaterialReceipt receipt = receiptMapper.selectForUpdate(infrastructure.tenantId(), bo.getMaterialReceiptId());
        if (receipt == null || !InventoryConstants.RECEIPT_RECEIVED.equals(receipt.getStatus())) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_SOURCE_INVALID,
                "原领料单不存在或尚未领用");
        }
        if (!receipt.getLeaderEmployeeId().equals(leaderEmployeeId)) {
            throw forbidden();
        }
        requireVersion(receipt.getVersion(), bo.getVersion());
        boolean pending = returnOrderMapper.exists(Wrappers.<ReturnOrder>lambdaQuery()
            .eq(ReturnOrder::getTenantId, infrastructure.tenantId())
            .eq(ReturnOrder::getMaterialReceiptId, receipt.getMaterialReceiptId())
            .eq(ReturnOrder::getStatus, InventoryConstants.STATUS_PENDING_RETURN));
        if (pending) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_PENDING_EXISTS,
                "该领料单已有待退库申请");
        }
        Map<Long, ReturnRequestLineBo> requested = normalizeReturnRequests(bo.getLines());
        Map<Long, MaterialReceiptLine> receiptLines = receiptLineMapper.selectList(Wrappers.<MaterialReceiptLine>lambdaQuery()
            .eq(MaterialReceiptLine::getTenantId, infrastructure.tenantId())
            .eq(MaterialReceiptLine::getMaterialReceiptId, receipt.getMaterialReceiptId())
            .in(MaterialReceiptLine::getMaterialReceiptLineId, requested.keySet())
            .last("FOR UPDATE"))
            .stream().collect(Collectors.toMap(MaterialReceiptLine::getMaterialReceiptLineId, Function.identity()));
        if (receiptLines.size() != requested.size()) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_SOURCE_INVALID,
                "退库明细不属于当前领料单");
        }
        Date now = new Date();
        ReturnOrder order = new ReturnOrder();
        order.setReturnOrderId(IdWorker.getId());
        order.setTenantId(infrastructure.tenantId());
        order.setOrderNo(infrastructure.nextOrderNo("RETURN"));
        order.setSource(InventoryConstants.SOURCE_LEADER_APPLY);
        order.setStatus(InventoryConstants.STATUS_PENDING_RETURN);
        order.setMaterialReceiptId(receipt.getMaterialReceiptId());
        order.setReceiptNoSnapshot(receipt.getReceiptNo());
        order.setTaskPackageId(receipt.getTaskPackageId());
        order.setFarmItemId(receipt.getFarmItemId());
        order.setLeaderEmployeeId(receipt.getLeaderEmployeeId());
        order.setReturnerNameSnapshot(receipt.getLeaderNameSnapshot());
        order.setTaskNameSnapshot(receipt.getTaskNameSnapshot());
        order.setFarmWorkNameSnapshot(receipt.getFarmWorkNameSnapshot());
        order.setGreenhouseNamesSnapshot(receipt.getGreenhouseNamesSnapshot());
        order.setBusinessDate(now);
        order.setVersion(0L);
        order.setCreateBy(infrastructure.operatorId());
        order.setCreateTime(now);
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(now);
        order.setRemark(bo.getNote());
        returnOrderMapper.insert(order);
        for (Map.Entry<Long, ReturnRequestLineBo> entry : requested.entrySet()) {
            MaterialReceiptLine receiptLine = receiptLines.get(entry.getKey());
            BigDecimal remaining = remaining(receiptLine);
            BigDecimal quantity = InventoryQuantity.positive(entry.getValue().getRequestedReturnQuantity(), "申请退库数量");
            if (quantity.compareTo(remaining) > 0) {
                throw quantityExceeded(receiptLine, remaining);
            }
            returnLineMapper.insert(newReturnLine(order, receiptLine, quantity, now));
        }
        infrastructure.audit("RETURN", order.getReturnOrderId(), "CREATE", bo.getNote(), null, toReturnVo(order, true));
        i18nRegistrar.registerReturnOrders(order.getTenantId(), List.of(order.getReturnOrderId()));
        return new MutationVo(order.getReturnOrderId(), order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo updateReturn(Long id, ReturnUpdateBo bo, Long leaderEmployeeId) {
        ReturnOrder order = requirePendingReturnForUpdate(id, bo.getVersion());
        if (!order.getLeaderEmployeeId().equals(leaderEmployeeId)) {
            throw forbidden();
        }
        if (!order.getMaterialReceiptId().equals(bo.getMaterialReceiptId())) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_SOURCE_INVALID,
                "不能修改退库单来源领料单");
        }
        ReturnVo before = toReturnVo(order, true);
        Map<Long, ReturnRequestLineBo> requested = normalizeReturnRequests(bo.getLines());
        Map<Long, MaterialReceiptLine> receiptLines = receiptLineMapper.selectList(Wrappers.<MaterialReceiptLine>lambdaQuery()
            .eq(MaterialReceiptLine::getTenantId, infrastructure.tenantId())
            .eq(MaterialReceiptLine::getMaterialReceiptId, order.getMaterialReceiptId())
            .in(MaterialReceiptLine::getMaterialReceiptLineId, requested.keySet()).last("FOR UPDATE"))
            .stream().collect(Collectors.toMap(MaterialReceiptLine::getMaterialReceiptLineId, Function.identity()));
        if (receiptLines.size() != requested.size()) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_SOURCE_INVALID,
                "退库明细不属于来源领料单");
        }
        returnLineMapper.delete(Wrappers.<ReturnLine>lambdaQuery()
            .eq(ReturnLine::getTenantId, infrastructure.tenantId()).eq(ReturnLine::getReturnOrderId, id));
        Date now = new Date();
        for (Map.Entry<Long, ReturnRequestLineBo> entry : requested.entrySet()) {
            MaterialReceiptLine receiptLine = receiptLines.get(entry.getKey());
            BigDecimal remaining = remaining(receiptLine);
            BigDecimal quantity = InventoryQuantity.positive(entry.getValue().getRequestedReturnQuantity(), "申请退库数量");
            if (quantity.compareTo(remaining) > 0) {
                throw quantityExceeded(receiptLine, remaining);
            }
            returnLineMapper.insert(newReturnLine(order, receiptLine, quantity, now));
        }
        order.setRemark(bo.getNote());
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(now);
        if (returnOrderMapper.updateById(order) != 1) {
            throw versionConflict();
        }
        infrastructure.audit("RETURN", id, "UPDATE", bo.getNote(), before, toReturnVo(order, true));
        i18nRegistrar.registerReturnOrders(order.getTenantId(), List.of(id));
        return new MutationVo(id, order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReturn(Long id, Long version, Long employeeId, boolean keeper) {
        ReturnOrder order = requirePendingReturnForUpdate(id, version);
        if (!keeper && !order.getLeaderEmployeeId().equals(employeeId)) {
            throw forbidden();
        }
        ReturnVo before = toReturnVo(order, true);
        returnLineMapper.delete(Wrappers.<ReturnLine>lambdaQuery()
            .eq(ReturnLine::getTenantId, infrastructure.tenantId()).eq(ReturnLine::getReturnOrderId, id));
        if (returnOrderMapper.deleteById(id) != 1) {
            throw notFound("退库单已被删除");
        }
        infrastructure.audit("RETURN", id, "PHYSICAL_DELETE", "待退库单删除并释放再次发起资格", before, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo confirmReturn(Long id, ReturnConfirmBo bo) {
        return infrastructure.idempotent("RETURN_CONFIRM", bo.getIdempotencyKey(), Map.of("id", id, "request", bo),
            () -> doConfirmReturn(id, bo));
    }

    private MutationVo doConfirmReturn(Long id, ReturnConfirmBo bo) {
        ReturnOrder order = requirePendingReturnForUpdate(id, bo.getVersion());
        // 确认前先固化申请快照；操作记录需要展示申请数量到实际退库数量的变化。
        ReturnVo before = toReturnVo(order, true);
        if (order.getMaterialReceiptId() == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_SOURCE_INVALID,
                "退库来源领料单已失效");
        }
        MaterialReceipt receipt = receiptMapper.selectForUpdate(infrastructure.tenantId(), order.getMaterialReceiptId());
        if (receipt == null || !InventoryConstants.RECEIPT_RECEIVED.equals(receipt.getStatus())) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_SOURCE_INVALID,
                "退库来源领料单或出库单已失效");
        }
        List<ReturnLine> lines = returnLineMapper.selectList(Wrappers.<ReturnLine>lambdaQuery()
            .eq(ReturnLine::getTenantId, infrastructure.tenantId()).eq(ReturnLine::getReturnOrderId, id)
            .orderByAsc(ReturnLine::getMaterialId).last("FOR UPDATE"));
        Map<Long, ReturnLine> byId = lines.stream().collect(Collectors.toMap(ReturnLine::getReturnLineId, Function.identity()));
        Set<Long> submittedLineIds = bo.getLines().stream().map(ReturnConfirmLineBo::getId)
            .collect(Collectors.toSet());
        if (submittedLineIds.size() != bo.getLines().size() || !byId.keySet().equals(submittedLineIds)) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_LINE_NOT_REQUESTED,
                "退库确认明细必须完整，且不能新增组长未申请的物资");
        }
        Map<Long, MaterialReceiptLine> receiptLines = receiptLineMapper.selectList(Wrappers.<MaterialReceiptLine>lambdaQuery()
            .eq(MaterialReceiptLine::getTenantId, infrastructure.tenantId())
            .eq(MaterialReceiptLine::getMaterialReceiptId, receipt.getMaterialReceiptId()).last("FOR UPDATE"))
            .stream().collect(Collectors.toMap(MaterialReceiptLine::getMaterialReceiptLineId, Function.identity()));
        for (ReturnConfirmLineBo request : bo.getLines()) {
            ReturnLine line = byId.get(request.getId());
            boolean deleted = Boolean.TRUE.equals(request.getDeletedByKeeper());
            line.setDeletedByKeeper(deleted);
            line.setActualReturnQuantity(deleted ? InventoryQuantity.ZERO
                : InventoryQuantity.positive(request.getActualReturnQuantity(), "实际退库数量"));
            if (!deleted && line.getActualReturnQuantity().compareTo(line.getRequestedReturnQuantity()) > 0) {
                throw quantityExceeded(receiptLines.get(line.getMaterialReceiptLineId()),
                    remaining(receiptLines.get(line.getMaterialReceiptLineId())));
            }
            line.setUpdateTime(new Date());
            returnLineMapper.updateById(line);
        }
        for (ReturnLine line : lines) {
            if (Boolean.TRUE.equals(line.getDeletedByKeeper()) || line.getActualReturnQuantity() == null
                || line.getActualReturnQuantity().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            MaterialReceiptLine receiptLine = receiptLines.get(line.getMaterialReceiptLineId());
            if (receiptLine == null) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_SOURCE_INVALID,
                    "原领料明细已失效");
            }
            BigDecimal currentRemaining = remaining(receiptLine);
            if (line.getActualReturnQuantity().compareTo(currentRemaining) > 0) {
                throw quantityExceeded(receiptLine, currentRemaining);
            }
            Material material = infrastructure.requireMaterial(line.getMaterialId());
            infrastructure.applyDelta(material, line.getActualReturnQuantity(), InventoryConstants.LEDGER_RETURN,
                InventoryConstants.SOURCE_LEADER_APPLY, "RETURN", id, order.getOrderNo(), line.getReturnLineId(), false, null);
            receiptLine.setReturnedQuantity(returned(receiptLine).add(line.getActualReturnQuantity()).setScale(1));
            receiptLine.setUpdateTime(new Date());
            receiptLineMapper.updateById(receiptLine);
        }
        order.setStatus(InventoryConstants.STATUS_RETURNED);
        order.setConfirmedBy(infrastructure.operatorId());
        order.setConfirmedAt(new Date());
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(new Date());
        if (returnOrderMapper.updateById(order) != 1) {
            throw versionConflict();
        }
        infrastructure.audit("RETURN", id, "CONFIRM", null, before, toReturnVo(order, true));
        i18nRegistrar.registerReturnOrders(order.getTenantId(), List.of(id));
        return new MutationVo(id, order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo createDirectReturn(DirectReturnSaveBo bo) {
        TreeMap<Long, BigDecimal> quantities = new TreeMap<>();
        for (DocumentLineBo request : bo.getLines()) {
            BigDecimal quantity = InventoryQuantity.positive(request.getQuantity(), "实际退库数量");
            if (quantities.putIfAbsent(request.getInventoryMaterialId(), quantity) != null) {
                throw InventoryBusinessException.rule("同一退库单同一物资只能出现一行");
            }
        }
        MaterialReceipt receipt = bo.getMaterialReceiptId() == null ? null
            : receiptMapper.selectForUpdate(infrastructure.tenantId(), bo.getMaterialReceiptId());
        Map<Long, MaterialReceiptLine> receiptLines = Map.of();
        if (bo.getMaterialReceiptId() != null) {
            if (receipt == null || !InventoryConstants.RECEIPT_RECEIVED.equals(receipt.getStatus())) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_SOURCE_INVALID,
                    "关联领料单不存在或尚未完成出库");
            }
            boolean pending = returnOrderMapper.exists(Wrappers.<ReturnOrder>lambdaQuery()
                .eq(ReturnOrder::getTenantId, infrastructure.tenantId())
                .eq(ReturnOrder::getMaterialReceiptId, receipt.getMaterialReceiptId())
                .eq(ReturnOrder::getStatus, InventoryConstants.STATUS_PENDING_RETURN));
            if (pending) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_PENDING_EXISTS,
                    "该领料单已有待退库申请");
            }
            receiptLines = receiptLineMapper.selectList(Wrappers.<MaterialReceiptLine>lambdaQuery()
                .eq(MaterialReceiptLine::getTenantId, infrastructure.tenantId())
                .eq(MaterialReceiptLine::getMaterialReceiptId, receipt.getMaterialReceiptId())
                .in(MaterialReceiptLine::getMaterialId, quantities.keySet())
                .orderByAsc(MaterialReceiptLine::getMaterialId).last("FOR UPDATE"))
                .stream().collect(Collectors.toMap(MaterialReceiptLine::getMaterialId, Function.identity()));
            if (receiptLines.size() != quantities.size()) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_LINE_NOT_REQUESTED,
                    "关联退库只能选择原领料单已实发的物资");
            }
        }
        Date now = new Date();
        ReturnOrder order = new ReturnOrder();
        order.setReturnOrderId(IdWorker.getId());
        order.setTenantId(infrastructure.tenantId());
        String requestedOrderNo = StrUtil.trim(bo.getOrderNo());
        order.setOrderNo(StrUtil.isBlank(requestedOrderNo)
            ? infrastructure.nextOrderNo("RETURN") : requestedOrderNo);
        order.setSource(InventoryConstants.SOURCE_WEB_DIRECT);
        order.setStatus(InventoryConstants.STATUS_RETURNED);
        order.setReturnerNameSnapshot(StrUtil.trim(bo.getReturnerName()));
        order.setBusinessDate(bo.getBusinessDate() == null ? now : bo.getBusinessDate());
        order.setConfirmedBy(infrastructure.operatorId());
        order.setConfirmedAt(now);
        order.setVersion(0L);
        order.setCreateBy(infrastructure.operatorId());
        order.setCreateTime(now);
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(now);
        order.setRemark(bo.getRemark());
        if (receipt != null) {
            order.setMaterialReceiptId(receipt.getMaterialReceiptId());
            order.setReceiptNoSnapshot(receipt.getReceiptNo());
            order.setTaskPackageId(receipt.getTaskPackageId());
            order.setFarmItemId(receipt.getFarmItemId());
            order.setLeaderEmployeeId(receipt.getLeaderEmployeeId());
            order.setTaskNameSnapshot(receipt.getTaskNameSnapshot());
            order.setFarmWorkNameSnapshot(receipt.getFarmWorkNameSnapshot());
            order.setGreenhouseNamesSnapshot(receipt.getGreenhouseNamesSnapshot());
        }
        try {
            returnOrderMapper.insert(order);
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("退库单号已存在");
        }
        for (Map.Entry<Long, BigDecimal> entry : quantities.entrySet()) {
            MaterialReceiptLine receiptLine = receiptLines.get(entry.getKey());
            Material material = receiptLine == null ? infrastructure.requireEnabledMaterial(entry.getKey())
                : infrastructure.requireMaterial(entry.getKey());
            if (receiptLine != null && entry.getValue().compareTo(remaining(receiptLine)) > 0) {
                throw quantityExceeded(receiptLine, remaining(receiptLine));
            }
            ReturnLine line;
            if (receiptLine == null) {
                line = new ReturnLine();
                line.setReturnLineId(IdWorker.getId());
                line.setTenantId(order.getTenantId());
                line.setReturnOrderId(order.getReturnOrderId());
                line.setMaterialId(material.getMaterialId());
                snapshot(line, material);
                line.setIssuedQuantitySnapshot(InventoryQuantity.ZERO);
                line.setReturnedBeforeSnapshot(InventoryQuantity.ZERO);
                line.setRemainingBeforeSnapshot(InventoryQuantity.ZERO);
                line.setRequestedReturnQuantity(entry.getValue());
                line.setDeletedByKeeper(false);
                line.setVersion(0L);
                line.setCreateTime(now);
                line.setUpdateTime(now);
            } else {
                line = newReturnLine(order, receiptLine, entry.getValue(), now);
            }
            line.setActualReturnQuantity(entry.getValue());
            returnLineMapper.insert(line);
            infrastructure.applyDelta(material, entry.getValue(), InventoryConstants.LEDGER_RETURN,
                InventoryConstants.SOURCE_WEB_DIRECT, "RETURN", order.getReturnOrderId(), order.getOrderNo(),
                line.getReturnLineId(), false, null, order.getBusinessDate());
            if (receiptLine != null) {
                receiptLine.setReturnedQuantity(returned(receiptLine).add(entry.getValue())
                    .setScale(InventoryConstants.QUANTITY_SCALE));
                receiptLine.setUpdateTime(now);
                receiptLineMapper.updateById(receiptLine);
            }
        }
        infrastructure.audit("RETURN", order.getReturnOrderId(), "CREATE_AND_CONFIRM", bo.getRemark(),
            null, toReturnVo(order, true));
        i18nRegistrar.registerReturnOrders(order.getTenantId(), List.of(order.getReturnOrderId()));
        return new MutationVo(order.getReturnOrderId(), order.getVersion(), order.getStatus());
    }

    @Override
    public long pendingReturnCount() {
        return returnOrderMapper.selectCount(Wrappers.<ReturnOrder>lambdaQuery()
            .eq(ReturnOrder::getTenantId, infrastructure.tenantId())
            .eq(ReturnOrder::getStatus, InventoryConstants.STATUS_PENDING_RETURN));
    }

    private MaterialReceipt requireReceipt(Long id) {
        MaterialReceipt receipt = receiptMapper.selectOne(Wrappers.<MaterialReceipt>lambdaQuery()
            .eq(MaterialReceipt::getTenantId, infrastructure.tenantId())
            .eq(MaterialReceipt::getMaterialReceiptId, id));
        if (receipt == null) {
            throw notFound("领料单不存在");
        }
        return receipt;
    }

    private ReturnOrder requireReturn(Long id) {
        ReturnOrder order = returnOrderMapper.selectOne(Wrappers.<ReturnOrder>lambdaQuery()
            .eq(ReturnOrder::getTenantId, infrastructure.tenantId()).eq(ReturnOrder::getReturnOrderId, id));
        if (order == null) {
            throw notFound("退库单不存在或已删除");
        }
        return order;
    }

    private ReturnOrder requirePendingReturnForUpdate(Long id, Long version) {
        ReturnOrder order = returnOrderMapper.selectForUpdate(infrastructure.tenantId(), id);
        if (order == null) {
            throw notFound("退库单不存在或已删除");
        }
        requireVersion(order.getVersion(), version);
        if (!InventoryConstants.STATUS_PENDING_RETURN.equals(order.getStatus())) {
            throw InventoryBusinessException.rule("只有待退库单可以执行该操作");
        }
        return order;
    }

    private Map<Long, SfStaskTaskPackage> receiptTaskPackages(List<MaterialReceipt> receipts) {
        List<Long> packageIds = receipts.stream()
            .map(MaterialReceipt::getTaskPackageId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (packageIds.isEmpty()) {
            return Map.of();
        }
        String tenantId = infrastructure.tenantId();
        return taskPackageMapper.selectList(Wrappers.<SfStaskTaskPackage>lambdaQuery()
                .eq(SfStaskTaskPackage::getTenantId, tenantId)
                .in(SfStaskTaskPackage::getPackageId, packageIds))
            .stream()
            .collect(Collectors.toMap(SfStaskTaskPackage::getPackageId, Function.identity()));
    }

    private ReceiptVo toReceiptVo(MaterialReceipt receipt, SfStaskTaskPackage taskPackage, boolean detail) {
        ReceiptVo vo = new ReceiptVo();
        vo.setId(receipt.getMaterialReceiptId());
        vo.setReceiptNo(receipt.getReceiptNo());
        vo.setTaskPackageId(receipt.getTaskPackageId());
        vo.setFarmItemId(receipt.getFarmItemId());
        vo.setLeaderEmployeeId(receipt.getLeaderEmployeeId());
        vo.setLeaderName(receipt.getLeaderNameSnapshot());
        receiptTechnicianAssembler.apply(vo, taskPackage);
        vo.setTaskName(receipt.getTaskNameSnapshot());
        vo.setFarmWorkName(receipt.getFarmWorkNameSnapshot());
        vo.setGreenhouseNames(receipt.getGreenhouseNamesSnapshot());
        vo.setKeeperCancelReason(receipt.getKeeperCancelReason());
        vo.setStatus(receipt.getStatus());
        vo.setArrived(receipt.getArrived());
        vo.setVersion(receipt.getVersion());
        vo.setCreateTime(receipt.getCreateTime());
        if (detail) {
            List<MaterialReceiptLine> lines = receiptLineMapper.selectList(Wrappers.<MaterialReceiptLine>lambdaQuery()
                .eq(MaterialReceiptLine::getTenantId, infrastructure.tenantId())
                .eq(MaterialReceiptLine::getMaterialReceiptId, receipt.getMaterialReceiptId())
                .orderByAsc(MaterialReceiptLine::getMaterialReceiptLineId));
            Map<Long, Material> materials = lines.isEmpty() ? Map.of() : materialMapper.selectList(Wrappers.<Material>lambdaQuery()
                .eq(Material::getTenantId, infrastructure.tenantId())
                .in(Material::getMaterialId, lines.stream().map(MaterialReceiptLine::getMaterialId).toList()))
                .stream().collect(Collectors.toMap(Material::getMaterialId, Function.identity()));
            vo.setLines(lines.stream().map(line -> toReceiptLineVo(line, materials.get(line.getMaterialId()))).toList());
        }
        return vo;
    }

    private ReceiptLineVo toReceiptLineVo(MaterialReceiptLine line, Material material) {
        ReceiptLineVo vo = new ReceiptLineVo();
        vo.setId(line.getMaterialReceiptLineId());
        vo.setInventoryMaterialId(line.getMaterialId());
        vo.setMaterialCode(line.getMaterialCodeSnapshot());
        vo.setMaterialName(line.getMaterialNameSnapshot());
        vo.setSpecification(line.getSpecificationSnapshot());
        vo.setUnit(line.getUnitSnapshot());
        vo.setRequestedQuantity(line.getRequestedQuantity());
        vo.setActualQuantity(line.getActualQuantity());
        vo.setReturnedQuantity(line.getReturnedQuantity());
        vo.setRemainingReturnableQuantity(remaining(line));
        vo.setEnabled(material != null && Boolean.TRUE.equals(material.getEnabled()) && "0".equals(material.getDelFlag()));
        return vo;
    }

    private ReturnVo toReturnVo(ReturnOrder order, boolean detail) {
        ReturnVo vo = new ReturnVo();
        vo.setId(order.getReturnOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setSource(order.getSource());
        vo.setStatus(order.getStatus());
        vo.setMaterialReceiptId(order.getMaterialReceiptId());
        vo.setReceiptNo(order.getReceiptNoSnapshot());
        vo.setTaskName(order.getTaskNameSnapshot());
        vo.setFarmWorkName(order.getFarmWorkNameSnapshot());
        vo.setGreenhouseNames(order.getGreenhouseNamesSnapshot());
        vo.setReturnerName(order.getReturnerNameSnapshot());
        vo.setInvalidReason(order.getInvalidReason());
        vo.setBusinessDate(order.getBusinessDate());
        vo.setVersion(order.getVersion());
        vo.setCreateTime(order.getCreateTime());
        vo.setConfirmedAt(order.getConfirmedAt());
        vo.setRemark(order.getRemark());
        if (detail) {
            List<ReturnLine> lines = returnLineMapper.selectList(Wrappers.<ReturnLine>lambdaQuery()
                .eq(ReturnLine::getTenantId, infrastructure.tenantId())
                .eq(ReturnLine::getReturnOrderId, order.getReturnOrderId())
                .orderByAsc(ReturnLine::getReturnLineId));
            Map<Long, MaterialReceiptLine> receiptLines = order.getMaterialReceiptId() == null ? Map.of()
                : receiptLineMapper.selectList(Wrappers.<MaterialReceiptLine>lambdaQuery()
                    .eq(MaterialReceiptLine::getTenantId, infrastructure.tenantId())
                    .eq(MaterialReceiptLine::getMaterialReceiptId, order.getMaterialReceiptId()))
                    .stream().collect(Collectors.toMap(MaterialReceiptLine::getMaterialReceiptLineId, Function.identity()));
            vo.setLines(lines.stream().map(line -> toReturnLineVo(line,
                line.getMaterialReceiptLineId() == null ? null
                    : receiptLines.get(line.getMaterialReceiptLineId()))).toList());
        }
        return vo;
    }

    private ReturnLineVo toReturnLineVo(ReturnLine line, MaterialReceiptLine receiptLine) {
        ReturnLineVo vo = new ReturnLineVo();
        vo.setId(line.getReturnLineId());
        vo.setMaterialReceiptLineId(line.getMaterialReceiptLineId());
        vo.setInventoryMaterialId(line.getMaterialId());
        vo.setMaterialCode(line.getMaterialCodeSnapshot());
        vo.setMaterialName(line.getMaterialNameSnapshot());
        vo.setSpecification(line.getSpecificationSnapshot());
        vo.setUnit(line.getUnitSnapshot());
        vo.setIssuedQuantity(line.getIssuedQuantitySnapshot());
        vo.setReturnedQuantity(receiptLine == null ? line.getReturnedBeforeSnapshot() : receiptLine.getReturnedQuantity());
        vo.setRemainingReturnableQuantity(receiptLine == null ? line.getRemainingBeforeSnapshot() : remaining(receiptLine));
        vo.setRequestedReturnQuantity(line.getRequestedReturnQuantity());
        vo.setActualReturnQuantity(line.getActualReturnQuantity());
        vo.setDeletedByKeeper(line.getDeletedByKeeper());
        vo.setInvalidReason(line.getInvalidReason());
        return vo;
    }

    private ReturnLine newReturnLine(ReturnOrder order, MaterialReceiptLine receiptLine,
                                     BigDecimal quantity, Date now) {
        ReturnLine line = new ReturnLine();
        line.setReturnLineId(IdWorker.getId());
        line.setTenantId(order.getTenantId());
        line.setReturnOrderId(order.getReturnOrderId());
        line.setMaterialReceiptLineId(receiptLine.getMaterialReceiptLineId());
        line.setMaterialId(receiptLine.getMaterialId());
        line.setMaterialCodeSnapshot(receiptLine.getMaterialCodeSnapshot());
        line.setMaterialNameSnapshot(receiptLine.getMaterialNameSnapshot());
        line.setSpecificationSnapshot(receiptLine.getSpecificationSnapshot());
        line.setUnitSnapshot(receiptLine.getUnitSnapshot());
        line.setIssuedQuantitySnapshot(receiptLine.getActualQuantity());
        line.setReturnedBeforeSnapshot(returned(receiptLine));
        line.setRemainingBeforeSnapshot(remaining(receiptLine));
        line.setRequestedReturnQuantity(quantity);
        line.setDeletedByKeeper(false);
        line.setVersion(0L);
        line.setCreateTime(now);
        line.setUpdateTime(now);
        return line;
    }

    private Map<Long, ReturnRequestLineBo> normalizeReturnRequests(List<ReturnRequestLineBo> lines) {
        Map<Long, ReturnRequestLineBo> result = new LinkedHashMap<>();
        for (ReturnRequestLineBo line : lines) {
            InventoryQuantity.positive(line.getRequestedReturnQuantity(), "申请退库数量");
            if (result.putIfAbsent(line.getMaterialReceiptLineId(), line) != null) {
                throw InventoryBusinessException.rule("同一领料明细只能出现一行");
            }
        }
        return result;
    }

    private BigDecimal remaining(MaterialReceiptLine line) {
        return line.getActualQuantity().subtract(returned(line)).max(InventoryQuantity.ZERO).setScale(1);
    }

    private BigDecimal returned(MaterialReceiptLine line) {
        return line.getReturnedQuantity() == null
            ? InventoryQuantity.ZERO : line.getReturnedQuantity().setScale(InventoryConstants.QUANTITY_SCALE);
    }

    private InventoryBusinessException quantityExceeded(MaterialReceiptLine line, BigDecimal remaining) {
        InventoryBusinessException.LineError error = new InventoryBusinessException.LineError(
            line == null ? null : line.getMaterialReceiptLineId(), line == null ? null : line.getMaterialId(),
            InventoryConstants.ERROR_RETURN_QUANTITY_EXCEEDED, "退库数量超过最新剩余可退量", null,
            remaining == null ? null : remaining.toPlainString());
        return new InventoryBusinessException(InventoryConstants.ERROR_RETURN_QUANTITY_EXCEEDED, 409,
            "退库数量超过剩余可退量", List.of(error));
    }

    private Set<Long> scopedPackageIds(String roleCode, Long employeeId) {
        if (!isTechnician(roleCode)) {
            return Set.of();
        }
        return taskPackageMapper.selectList(Wrappers.<SfStaskTaskPackage>lambdaQuery()
            .eq(SfStaskTaskPackage::getTenantId, infrastructure.tenantId())
            .and(wrapper -> wrapper.eq(SfStaskTaskPackage::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskTaskPackage::getHandlerTechnicianEmployeeId, employeeId)))
            .stream().map(SfStaskTaskPackage::getPackageId).collect(Collectors.toSet());
    }

    private boolean isTechnician(String roleCode) {
        return EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(roleCode);
    }

    private void ensureReceiptScope(MaterialReceipt receipt, String roleCode, Long employeeId) {
        if (isTaskReadonlyViewer(roleCode)) {
            ensureReadonlyTaskAssociation(receipt);
            return;
        }
        if (EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(roleCode)
            && !receipt.getLeaderEmployeeId().equals(employeeId)) {
            throw forbidden();
        }
        if (isTechnician(roleCode) && !scopedPackageIds(roleCode, employeeId).contains(receipt.getTaskPackageId())) {
            throw forbidden();
        }
    }

    private boolean isTaskReadonlyViewer(String roleCode) {
        return EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN.equals(roleCode)
            || EmployeeConstants.APP_ROLE_STASK_LEADER.equals(roleCode);
    }

    private void ensureReadonlyTaskAssociation(MaterialReceipt receipt) {
        SfStaskTaskPackage taskPackage = receipt.getTaskPackageId() == null
            ? null : taskPackageMapper.selectById(receipt.getTaskPackageId());
        if (taskPackage == null
            || !Objects.equals(taskPackage.getTenantId(), infrastructure.tenantId())
            || !Objects.equals(taskPackage.getTenantId(), receipt.getTenantId())
            || StaskOrderStatus.DRAFT.equals(taskPackage.getStatus())) {
            throw forbidden();
        }
    }

    private void ensureReturnScope(ReturnOrder order, String roleCode, Long employeeId) {
        if (EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(roleCode)
            && !employeeId.equals(order.getLeaderEmployeeId())) {
            throw forbidden();
        }
        if (isTechnician(roleCode) && !scopedPackageIds(roleCode, employeeId).contains(order.getTaskPackageId())) {
            throw forbidden();
        }
    }

    private void snapshot(ReturnLine line, Material material) {
        line.setMaterialCodeSnapshot(material.getMaterialCode());
        line.setMaterialNameSnapshot(material.getMaterialName());
        line.setSpecificationSnapshot(material.getSpecification());
        line.setUnitSnapshot(material.getUnit());
    }

    private void requireVersion(Long current, Long request) {
        if (request == null || !request.equals(current)) {
            throw versionConflict();
        }
    }

    private InventoryBusinessException versionConflict() {
        return new InventoryBusinessException(InventoryConstants.ERROR_VERSION_CONFLICT,
            "数据版本已变化，请刷新后重试");
    }

    private InventoryBusinessException notFound(String message) {
        return new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, message);
    }

    private InventoryBusinessException forbidden() {
        return new InventoryBusinessException(InventoryConstants.ERROR_ACTION_FORBIDDEN, 403, "无权访问该单据");
    }
}
