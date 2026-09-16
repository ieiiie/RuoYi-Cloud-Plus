package com.ym.agriculture.farmtask.inventory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.InboundLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.InboundOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.LedgerMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.OutboundLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.OutboundOrderMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.OperationLogMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.ReturnLineMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.ReturnOrderMapper;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.CorrectionDeleteBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.CorrectionDocumentBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.CorrectionLineBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundCorrectionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundCorrectionLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundLineVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.OutboundCorrectionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.OutboundCorrectionLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.ReturnCorrectionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.ReturnCorrectionLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.ReturnOperationLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Balance;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.InboundLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.InboundOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Ledger;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Material;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceipt;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceiptLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OutboundLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OutboundOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OperationLog;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OutboundLineVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OutboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnLineVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnVo;
import com.ym.agriculture.farmtask.inventory.service.IInventoryCorrectionService;
import com.ym.agriculture.farmtask.inventory.service.InventoryInfrastructure;
import com.ym.agriculture.farmtask.inventory.support.InventoryQuantity;
import com.ym.agriculture.farmtask.inventory.support.InventoryI18nResourceRegistrar;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 历史入库、出库和退库单的原单/原流水就地纠错实现。 */
@Service
@RequiredArgsConstructor
public class InventoryCorrectionServiceImpl implements IInventoryCorrectionService {

    private final InventoryInfrastructure infrastructure;
    private final LedgerMapper ledgerMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundLineMapper inboundLineMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundLineMapper outboundLineMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final ReturnLineMapper returnLineMapper;
    private final MaterialReceiptMapper receiptMapper;
    private final MaterialReceiptLineMapper receiptLineMapper;
    private final OperationLogMapper operationLogMapper;
    private final InventoryI18nResourceRegistrar i18nRegistrar;

    @Override
    public InboundVo getInbound(Long id) {
        return toInboundVo(requireInboundForHistory(id));
    }

    @Override
    public PageResult<InboundCorrectionLogVo> pageInboundCorrectionLogs(Long id, PageQuery pageQuery) {
        requireInboundForHistory(id);
        IPage<OperationLog> page = operationLogMapper.selectPage(pageQuery.build(),
            Wrappers.<OperationLog>lambdaQuery()
                .eq(OperationLog::getTenantId, infrastructure.tenantId())
                .eq(OperationLog::getAggregateType, "INBOUND")
                .eq(OperationLog::getAggregateId, id)
                .eq(OperationLog::getAction, "HISTORICAL_CORRECTION")
                .orderByDesc(OperationLog::getCreateTime)
                .orderByDesc(OperationLog::getOperationLogId));
        IPage<InboundCorrectionLogVo> result = page.convert(this::toInboundCorrectionLogVo);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public PageResult<OutboundCorrectionLogVo> pageOutboundCorrectionLogs(Long id, PageQuery pageQuery) {
        requireOutboundForHistory(id);
        IPage<OperationLog> page = operationLogMapper.selectPage(pageQuery.build(),
            Wrappers.<OperationLog>lambdaQuery()
                .eq(OperationLog::getTenantId, infrastructure.tenantId())
                .eq(OperationLog::getAggregateType, "OUTBOUND")
                .eq(OperationLog::getAggregateId, id)
                .eq(OperationLog::getAction, "HISTORICAL_CORRECTION")
                .orderByDesc(OperationLog::getCreateTime)
                .orderByDesc(OperationLog::getOperationLogId));
        IPage<OutboundCorrectionLogVo> result = page.convert(this::toOutboundCorrectionLogVo);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public PageResult<ReturnCorrectionLogVo> pageReturnCorrectionLogs(Long id, PageQuery pageQuery) {
        requireReturnForHistory(id);
        IPage<OperationLog> page = operationLogMapper.selectPage(pageQuery.build(),
            Wrappers.<OperationLog>lambdaQuery()
                .eq(OperationLog::getTenantId, infrastructure.tenantId())
                .eq(OperationLog::getAggregateType, "RETURN")
                .eq(OperationLog::getAggregateId, id)
                .eq(OperationLog::getAction, "HISTORICAL_CORRECTION")
                .orderByDesc(OperationLog::getCreateTime)
                .orderByDesc(OperationLog::getOperationLogId));
        IPage<ReturnCorrectionLogVo> result = page.convert(this::toReturnCorrectionLogVo);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public PageResult<ReturnOperationLogVo> pageReturnOperationLogs(Long id, PageQuery pageQuery) {
        requireReturnForHistory(id);
        IPage<OperationLog> page = operationLogMapper.selectPage(pageQuery.build(),
            Wrappers.<OperationLog>lambdaQuery()
                .eq(OperationLog::getTenantId, infrastructure.tenantId())
                .eq(OperationLog::getAggregateType, "RETURN")
                .eq(OperationLog::getAggregateId, id)
                .in(OperationLog::getAction, List.of("CREATE", "UPDATE", "CONFIRM", "CREATE_AND_CONFIRM",
                    "HISTORICAL_CORRECTION", "VOID"))
                .orderByDesc(OperationLog::getCreateTime)
                .orderByDesc(OperationLog::getOperationLogId));
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(page.convert(this::toReturnOperationLogVo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo correctInbound(Long id, InboundCorrectionBo bo) {
        InboundOrder order = requireInboundForCorrection(id, bo.getVersion());
        InboundVo before = toInboundVo(order);
        TreeMap<Long, BigDecimal> requested = normalizeLines(bo.getLines(), "入库数量");
        List<InboundLine> currentLines = inboundLineMapper.selectList(Wrappers.<InboundLine>lambdaQuery()
            .eq(InboundLine::getTenantId, infrastructure.tenantId())
            .eq(InboundLine::getInboundOrderId, id)
            .orderByAsc(InboundLine::getMaterialId).last("FOR UPDATE"));
        Date now = new Date();
        for (InboundLine line : currentLines) {
            BigDecimal oldQuantity = effective(line.getQuantity());
            BigDecimal newQuantity = requested.remove(line.getMaterialId());
            if (newQuantity == null) {
                newQuantity = InventoryQuantity.ZERO;
            }
            Material material = infrastructure.requireMaterial(line.getMaterialId());
            Balance balance = infrastructure.adjustBalanceForCorrection(material,
                newQuantity.subtract(oldQuantity), Boolean.TRUE.equals(bo.getAllowNegativeCorrection()),
                bo.getCorrectionReason(), line.getInboundLineId());
            line.setQuantity(newQuantity);
            line.setUpdateTime(now);
            inboundLineMapper.updateById(line);
            syncLedger("INBOUND", id, bo.getOrderNo(), line.getInboundLineId(), material,
                InventoryConstants.LEDGER_INBOUND, "WEB_INBOUND", newQuantity,
                newQuantity.compareTo(BigDecimal.ZERO) > 0, bo.getBusinessDate(), bo.getCorrectionReason(), balance);
        }
        for (Map.Entry<Long, BigDecimal> entry : requested.entrySet()) {
            Material material = infrastructure.requireEnabledMaterial(entry.getKey());
            InboundLine line = new InboundLine();
            line.setInboundLineId(IdWorker.getId());
            line.setTenantId(order.getTenantId());
            line.setInboundOrderId(id);
            line.setMaterialId(material.getMaterialId());
            snapshot(line, material);
            line.setQuantity(entry.getValue());
            line.setCreateTime(now);
            line.setUpdateTime(now);
            inboundLineMapper.insert(line);
            Balance balance = infrastructure.adjustBalanceForCorrection(material, entry.getValue(), false,
                bo.getCorrectionReason(), line.getInboundLineId());
            syncLedger("INBOUND", id, bo.getOrderNo(), line.getInboundLineId(), material,
                InventoryConstants.LEDGER_INBOUND, "WEB_INBOUND", entry.getValue(), true,
                bo.getBusinessDate(), bo.getCorrectionReason(), balance);
        }
        order.setOrderNo(StrUtil.trim(bo.getOrderNo()));
        order.setSupplierName(StrUtil.trim(bo.getSupplierName()));
        order.setBusinessDate(bo.getBusinessDate());
        order.setRemark(bo.getRemark());
        touch(order, now);
        updateInbound(order);
        infrastructure.audit("INBOUND", id, "HISTORICAL_CORRECTION", correctionReason(bo),
            before, toInboundVo(order));
        i18nRegistrar.registerInboundOrders(order.getTenantId(), List.of(id));
        return new MutationVo(id, order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo deleteInbound(Long id, CorrectionDeleteBo bo) {
        InboundOrder order = requireInboundForCorrection(id, bo.getVersion());
        InboundVo before = toInboundVo(order);
        List<InboundLine> lines = inboundLineMapper.selectList(Wrappers.<InboundLine>lambdaQuery()
            .eq(InboundLine::getTenantId, infrastructure.tenantId())
            .eq(InboundLine::getInboundOrderId, id)
            .orderByAsc(InboundLine::getMaterialId).last("FOR UPDATE"));
        for (InboundLine line : lines) {
            BigDecimal quantity = effective(line.getQuantity());
            if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Material material = infrastructure.requireMaterial(line.getMaterialId());
            Balance balance = infrastructure.adjustBalanceForCorrection(material, quantity.negate(),
                Boolean.TRUE.equals(bo.getAllowNegativeCorrection()), bo.getCorrectionReason(), line.getInboundLineId());
            syncLedger("INBOUND", id, order.getOrderNo(), line.getInboundLineId(), material,
                InventoryConstants.LEDGER_INBOUND, "WEB_INBOUND", InventoryQuantity.ZERO,
                false, order.getBusinessDate(), bo.getCorrectionReason(), balance);
        }
        order.setStatus(InventoryConstants.STATUS_VOIDED);
        touch(order, new Date());
        updateInbound(order);
        infrastructure.audit("INBOUND", id, "VOID", bo.getCorrectionReason(), before, toInboundVo(order));
        i18nRegistrar.registerInboundOrders(order.getTenantId(), List.of(id));
        return new MutationVo(id, order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo correctOutbound(Long id, OutboundCorrectionBo bo) {
        OutboundOrder order = requireCompletedOutbound(id, bo.getVersion());
        Object before = outboundSnapshot(order);
        TreeMap<Long, BigDecimal> requested = normalizeLines(bo.getLines(), "实发数量");
        List<OutboundLine> lines = outboundLinesForUpdate(id);
        MaterialReceipt receipt = order.getMaterialReceiptId() == null ? null
            : receiptMapper.selectForUpdate(infrastructure.tenantId(), order.getMaterialReceiptId());
        Map<Long, MaterialReceiptLine> receiptByMaterial = receipt == null ? Map.of()
            : receiptLinesForUpdate(receipt.getMaterialReceiptId()).stream()
                .collect(Collectors.toMap(MaterialReceiptLine::getMaterialId, Function.identity()));
        if (receipt != null && !receiptByMaterial.keySet().containsAll(requested.keySet())) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_LINE_NOT_REQUESTED,
                "任务出库纠错不能新增领料单中不存在的物资");
        }
        Date now = new Date();
        for (OutboundLine line : lines) {
            BigDecimal oldQuantity = Boolean.TRUE.equals(line.getDeletedByKeeper())
                ? InventoryQuantity.ZERO : effective(line.getActualQuantity());
            BigDecimal newQuantity = requested.remove(line.getMaterialId());
            if (newQuantity == null) {
                newQuantity = InventoryQuantity.ZERO;
            }
            MaterialReceiptLine receiptLine = receiptByMaterial.get(line.getMaterialId());
            validateOutboundAgainstReturns(receiptLine, newQuantity);
            Material material = infrastructure.requireMaterial(line.getMaterialId());
            Balance balance = infrastructure.adjustBalanceForCorrection(material,
                oldQuantity.subtract(newQuantity), Boolean.TRUE.equals(bo.getAllowNegativeCorrection()),
                bo.getCorrectionReason(), line.getOutboundLineId());
            line.setActualQuantity(newQuantity);
            line.setDeletedByKeeper(newQuantity.compareTo(BigDecimal.ZERO) == 0);
            line.setUpdateTime(now);
            outboundLineMapper.updateById(line);
            syncLedger("OUTBOUND", id, bo.getOrderNo(), line.getOutboundLineId(), material,
                InventoryConstants.LEDGER_OUTBOUND, order.getSource(), newQuantity.negate(),
                newQuantity.compareTo(BigDecimal.ZERO) > 0, bo.getBusinessDate(), bo.getCorrectionReason(), balance);
            updateReceiptActual(receiptLine, newQuantity, now);
        }
        for (Map.Entry<Long, BigDecimal> entry : requested.entrySet()) {
            MaterialReceiptLine receiptLine = receiptByMaterial.get(entry.getKey());
            validateOutboundAgainstReturns(receiptLine, entry.getValue());
            Material material = receiptLine == null ? infrastructure.requireEnabledMaterial(entry.getKey())
                : infrastructure.requireMaterial(entry.getKey());
            OutboundLine line = newOutboundLine(order, material, receiptLine, entry.getValue(), now);
            outboundLineMapper.insert(line);
            Balance balance = infrastructure.adjustBalanceForCorrection(material, entry.getValue().negate(),
                Boolean.TRUE.equals(bo.getAllowNegativeCorrection()), bo.getCorrectionReason(), line.getOutboundLineId());
            syncLedger("OUTBOUND", id, bo.getOrderNo(), line.getOutboundLineId(), material,
                InventoryConstants.LEDGER_OUTBOUND, order.getSource(), entry.getValue().negate(), true,
                bo.getBusinessDate(), bo.getCorrectionReason(), balance);
            updateReceiptActual(receiptLine, entry.getValue(), now);
        }
        order.setOrderNo(StrUtil.trim(bo.getOrderNo()));
        order.setReceiverNameSnapshot(StrUtil.trim(bo.getReceiverName()));
        order.setBusinessDate(bo.getBusinessDate());
        order.setConfirmedAt(bo.getBusinessDate());
        order.setRemark(bo.getRemark());
        touch(order, now);
        updateOutbound(order);
        infrastructure.audit("OUTBOUND", id, "HISTORICAL_CORRECTION", correctionReason(bo),
            before, outboundSnapshot(order));
        i18nRegistrar.registerOutboundOrders(order.getTenantId(), List.of(id));
        return new MutationVo(id, order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo deleteOutbound(Long id, CorrectionDeleteBo bo) {
        OutboundOrder order = requireCompletedOutbound(id, bo.getVersion());
        Object before = outboundSnapshot(order);
        List<OutboundLine> lines = outboundLinesForUpdate(id);
        MaterialReceipt receipt = order.getMaterialReceiptId() == null ? null
            : receiptMapper.selectForUpdate(infrastructure.tenantId(), order.getMaterialReceiptId());
        Map<Long, MaterialReceiptLine> receiptById = receipt == null ? Map.of()
            : receiptLinesForUpdate(receipt.getMaterialReceiptId()).stream()
                .collect(Collectors.toMap(MaterialReceiptLine::getMaterialReceiptLineId, Function.identity()));
        if (receiptById.values().stream().anyMatch(line -> effective(line.getReturnedQuantity()).compareTo(BigDecimal.ZERO) > 0)) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_OUTBOUND_RETURN_EXISTS,
                "原出库单已有完成退库，请先纠错或删除关联退库单");
        }
        Date now = new Date();
        for (OutboundLine line : lines) {
            BigDecimal quantity = Boolean.TRUE.equals(line.getDeletedByKeeper())
                ? InventoryQuantity.ZERO : effective(line.getActualQuantity());
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                Material material = infrastructure.requireMaterial(line.getMaterialId());
                Balance balance = infrastructure.adjustBalanceForCorrection(material, quantity,
                    false, bo.getCorrectionReason(), line.getOutboundLineId());
                syncLedger("OUTBOUND", id, order.getOrderNo(), line.getOutboundLineId(), material,
                    InventoryConstants.LEDGER_OUTBOUND, order.getSource(), InventoryQuantity.ZERO,
                    false, order.getBusinessDate(), bo.getCorrectionReason(), balance);
            }
            updateReceiptActual(receiptById.get(line.getMaterialReceiptLineId()), InventoryQuantity.ZERO, now);
        }
        if (receipt != null && !InventoryConstants.RECEIPT_VOIDED.equals(receipt.getStatus())) {
            receipt.setStatus(InventoryConstants.RECEIPT_UNCLAIMED);
            receipt.setUpdateBy(infrastructure.operatorId());
            receipt.setUpdateTime(now);
            receiptMapper.updateById(receipt);
        }
        order.setStatus(InventoryConstants.STATUS_VOIDED);
        touch(order, now);
        updateOutbound(order);
        infrastructure.audit("OUTBOUND", id, "VOID", bo.getCorrectionReason(), before, outboundSnapshot(order));
        i18nRegistrar.registerOutboundOrders(order.getTenantId(), List.of(id));
        return new MutationVo(id, order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo correctReturn(Long id, ReturnCorrectionBo bo) {
        ReturnOrder order = requireReturnedForCorrection(id, bo.getVersion());
        Object before = returnSnapshot(order);
        TreeMap<Long, BigDecimal> requested = normalizeLines(bo.getLines(), "实际退库数量");
        List<ReturnLine> lines = returnLinesForUpdate(id);
        MaterialReceipt receipt = order.getMaterialReceiptId() == null ? null
            : receiptMapper.selectForUpdate(infrastructure.tenantId(), order.getMaterialReceiptId());
        Map<Long, MaterialReceiptLine> receiptByMaterial = receipt == null ? Map.of()
            : receiptLinesForUpdate(receipt.getMaterialReceiptId()).stream()
                .collect(Collectors.toMap(MaterialReceiptLine::getMaterialId, Function.identity()));
        if (receipt != null && !receiptByMaterial.keySet().containsAll(requested.keySet())) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_LINE_NOT_REQUESTED,
                "关联退库纠错不能新增原领料单未实发的物资");
        }
        Date now = new Date();
        for (ReturnLine line : lines) {
            BigDecimal oldQuantity = Boolean.TRUE.equals(line.getDeletedByKeeper())
                ? InventoryQuantity.ZERO : effective(line.getActualReturnQuantity());
            BigDecimal newQuantity = requested.remove(line.getMaterialId());
            if (newQuantity == null) {
                newQuantity = InventoryQuantity.ZERO;
            }
            MaterialReceiptLine receiptLine = receiptByMaterial.get(line.getMaterialId());
            validateAndUpdateReturned(receiptLine, oldQuantity, newQuantity, now);
            Material material = infrastructure.requireMaterial(line.getMaterialId());
            Balance balance = infrastructure.adjustBalanceForCorrection(material,
                newQuantity.subtract(oldQuantity), Boolean.TRUE.equals(bo.getAllowNegativeCorrection()),
                bo.getCorrectionReason(), line.getReturnLineId());
            line.setRequestedReturnQuantity(newQuantity);
            line.setActualReturnQuantity(newQuantity);
            line.setDeletedByKeeper(newQuantity.compareTo(BigDecimal.ZERO) == 0);
            line.setUpdateTime(now);
            returnLineMapper.updateById(line);
            syncLedger("RETURN", id, bo.getOrderNo(), line.getReturnLineId(), material,
                InventoryConstants.LEDGER_RETURN, order.getSource(), newQuantity,
                newQuantity.compareTo(BigDecimal.ZERO) > 0, bo.getBusinessDate(), bo.getCorrectionReason(), balance);
        }
        for (Map.Entry<Long, BigDecimal> entry : requested.entrySet()) {
            MaterialReceiptLine receiptLine = receiptByMaterial.get(entry.getKey());
            validateAndUpdateReturned(receiptLine, InventoryQuantity.ZERO, entry.getValue(), now);
            Material material = receiptLine == null ? infrastructure.requireEnabledMaterial(entry.getKey())
                : infrastructure.requireMaterial(entry.getKey());
            ReturnLine line = newReturnLine(order, material, receiptLine, entry.getValue(), now);
            returnLineMapper.insert(line);
            Balance balance = infrastructure.adjustBalanceForCorrection(material, entry.getValue(), false,
                bo.getCorrectionReason(), line.getReturnLineId());
            syncLedger("RETURN", id, bo.getOrderNo(), line.getReturnLineId(), material,
                InventoryConstants.LEDGER_RETURN, order.getSource(), entry.getValue(), true,
                bo.getBusinessDate(), bo.getCorrectionReason(), balance);
        }
        order.setOrderNo(StrUtil.trim(bo.getOrderNo()));
        order.setReturnerNameSnapshot(StrUtil.trim(bo.getReturnerName()));
        order.setBusinessDate(bo.getBusinessDate());
        order.setConfirmedAt(bo.getBusinessDate());
        order.setRemark(bo.getRemark());
        touch(order, now);
        updateReturn(order);
        infrastructure.audit("RETURN", id, "HISTORICAL_CORRECTION", correctionReason(bo),
            before, returnSnapshot(order));
        i18nRegistrar.registerReturnOrders(order.getTenantId(), List.of(id));
        return new MutationVo(id, order.getVersion(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo deleteReturn(Long id, CorrectionDeleteBo bo) {
        ReturnOrder order = returnOrderMapper.selectForUpdate(infrastructure.tenantId(), id);
        if (order == null) {
            throw notFound("退库单不存在");
        }
        requireVersion(order.getVersion(), bo.getVersion());
        String voidReason = voidReason(bo.getCorrectionReason());
        if (InventoryConstants.STATUS_PENDING_RETURN.equals(order.getStatus())) {
            Object before = returnSnapshot(order);
            order.setStatus(InventoryConstants.STATUS_VOIDED);
            touch(order, new Date());
            updateReturn(order);
            infrastructure.audit("RETURN", id, "VOID", voidReason, before, returnSnapshot(order));
            i18nRegistrar.registerReturnOrders(order.getTenantId(), List.of(id));
            return new MutationVo(id, order.getVersion(), InventoryConstants.STATUS_VOIDED);
        }
        if (!InventoryConstants.STATUS_RETURNED.equals(order.getStatus())) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_ALREADY_PROCESSED,
                "只有待退库或已退库单可以作废");
        }
        Object before = returnSnapshot(order);
        List<ReturnLine> lines = returnLinesForUpdate(id);
        MaterialReceipt receipt = order.getMaterialReceiptId() == null ? null
            : receiptMapper.selectForUpdate(infrastructure.tenantId(), order.getMaterialReceiptId());
        Map<Long, MaterialReceiptLine> receiptById = receipt == null ? Map.of()
            : receiptLinesForUpdate(receipt.getMaterialReceiptId()).stream()
                .collect(Collectors.toMap(MaterialReceiptLine::getMaterialReceiptLineId, Function.identity()));
        Date now = new Date();
        for (ReturnLine line : lines) {
            BigDecimal quantity = Boolean.TRUE.equals(line.getDeletedByKeeper())
                ? InventoryQuantity.ZERO : effective(line.getActualReturnQuantity());
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                Material material = infrastructure.requireMaterial(line.getMaterialId());
                infrastructure.applyCorrectionDelta(material, quantity.negate(), InventoryConstants.LEDGER_ADJUST,
                    InventoryConstants.SUBTYPE_RETURN_VOID, "RETURN", id, order.getOrderNo(),
                    returnVoidLedgerSourceLineId(line.getReturnLineId()),
                    Boolean.TRUE.equals(bo.getAllowNegativeCorrection()), voidReason, now);
            }
            MaterialReceiptLine receiptLine = line.getMaterialReceiptLineId() == null ? null
                : receiptById.get(line.getMaterialReceiptLineId());
            if (receiptLine != null) {
                receiptLine.setReturnedQuantity(effective(receiptLine.getReturnedQuantity()).subtract(quantity).max(BigDecimal.ZERO)
                    .setScale(InventoryConstants.QUANTITY_SCALE));
                receiptLine.setUpdateTime(now);
                receiptLineMapper.updateById(receiptLine);
            }
        }
        order.setStatus(InventoryConstants.STATUS_VOIDED);
        touch(order, now);
        updateReturn(order);
        infrastructure.audit("RETURN", id, "VOID", voidReason, before, returnSnapshot(order));
        i18nRegistrar.registerReturnOrders(order.getTenantId(), List.of(id));
        i18nRegistrar.registerMaterials(order.getTenantId(),
            lines.stream().map(ReturnLine::getMaterialId).distinct().toList());
        return new MutationVo(id, order.getVersion(), InventoryConstants.STATUS_VOIDED);
    }

    private InboundOrder requireInboundForCorrection(Long id, Long version) {
        InboundOrder order = inboundOrderMapper.selectForUpdate(infrastructure.tenantId(), id);
        if (order == null) {
            throw notFound("入库单不存在");
        }
        requireVersion(order.getVersion(), version);
        if (!InventoryConstants.STATUS_COMPLETED.equals(order.getStatus())) {
            throw alreadyProcessed("只有已生效入库单可以纠错或作废");
        }
        return order;
    }

    private OutboundOrder requireOutboundForHistory(Long id) {
        OutboundOrder order = outboundOrderMapper.selectOne(Wrappers.<OutboundOrder>lambdaQuery()
            .eq(OutboundOrder::getTenantId, infrastructure.tenantId())
            .eq(OutboundOrder::getOutboundOrderId, id));
        if (order == null) {
            throw notFound("出库单不存在");
        }
        return order;
    }

    private ReturnOrder requireReturnForHistory(Long id) {
        ReturnOrder order = returnOrderMapper.selectOne(Wrappers.<ReturnOrder>lambdaQuery()
            .eq(ReturnOrder::getTenantId, infrastructure.tenantId())
            .eq(ReturnOrder::getReturnOrderId, id));
        if (order == null) {
            throw notFound("退库单不存在");
        }
        return order;
    }

    private OutboundOrder requireCompletedOutbound(Long id, Long version) {
        OutboundOrder order = outboundOrderMapper.selectForUpdate(infrastructure.tenantId(), id);
        if (order == null) {
            throw notFound("出库单不存在");
        }
        requireVersion(order.getVersion(), version);
        if (!InventoryConstants.STATUS_COMPLETED.equals(order.getStatus())) {
            throw alreadyProcessed("只有已出库单可以执行历史纠错或删除");
        }
        return order;
    }

    private ReturnOrder requireReturnedForCorrection(Long id, Long version) {
        ReturnOrder order = returnOrderMapper.selectForUpdate(infrastructure.tenantId(), id);
        if (order == null) {
            throw notFound("退库单不存在");
        }
        requireVersion(order.getVersion(), version);
        if (!InventoryConstants.STATUS_RETURNED.equals(order.getStatus())) {
            throw alreadyProcessed("只有已退库单可以执行历史纠错");
        }
        return order;
    }

    private TreeMap<Long, BigDecimal> normalizeLines(List<CorrectionLineBo> lines, String fieldName) {
        TreeMap<Long, BigDecimal> result = new TreeMap<>();
        for (CorrectionLineBo line : lines) {
            BigDecimal quantity = InventoryQuantity.positive(line.getQuantity(), fieldName);
            if (result.putIfAbsent(line.getInventoryMaterialId(), quantity) != null) {
                throw InventoryBusinessException.rule("同一单据同一物资只能出现一行");
            }
        }
        return result;
    }

    private List<OutboundLine> outboundLinesForUpdate(Long id) {
        return outboundLineMapper.selectList(Wrappers.<OutboundLine>lambdaQuery()
            .eq(OutboundLine::getTenantId, infrastructure.tenantId())
            .eq(OutboundLine::getOutboundOrderId, id)
            .orderByAsc(OutboundLine::getMaterialId).last("FOR UPDATE"));
    }

    private List<ReturnLine> returnLinesForUpdate(Long id) {
        return returnLineMapper.selectList(Wrappers.<ReturnLine>lambdaQuery()
            .eq(ReturnLine::getTenantId, infrastructure.tenantId())
            .eq(ReturnLine::getReturnOrderId, id)
            .orderByAsc(ReturnLine::getMaterialId).last("FOR UPDATE"));
    }

    private List<MaterialReceiptLine> receiptLinesForUpdate(Long receiptId) {
        return receiptLineMapper.selectList(Wrappers.<MaterialReceiptLine>lambdaQuery()
            .eq(MaterialReceiptLine::getTenantId, infrastructure.tenantId())
            .eq(MaterialReceiptLine::getMaterialReceiptId, receiptId)
            .orderByAsc(MaterialReceiptLine::getMaterialId).last("FOR UPDATE"));
    }

    private void validateOutboundAgainstReturns(MaterialReceiptLine receiptLine, BigDecimal newQuantity) {
        if (receiptLine == null) {
            return;
        }
        BigDecimal returned = effective(receiptLine.getReturnedQuantity());
        if (newQuantity.compareTo(returned) < 0) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_OUTBOUND_RETURN_EXISTS,
                "实发数量不能小于累计已退数量，请先处理关联退库单");
        }
    }

    private void updateReceiptActual(MaterialReceiptLine receiptLine, BigDecimal quantity, Date now) {
        if (receiptLine == null) {
            return;
        }
        receiptLine.setActualQuantity(quantity);
        receiptLine.setUpdateTime(now);
        receiptLineMapper.updateById(receiptLine);
    }

    private void validateAndUpdateReturned(MaterialReceiptLine receiptLine, BigDecimal oldQuantity,
                                           BigDecimal newQuantity, Date now) {
        if (receiptLine == null) {
            return;
        }
        BigDecimal returned = effective(receiptLine.getReturnedQuantity());
        BigDecimal otherReturned = returned.subtract(oldQuantity).max(BigDecimal.ZERO)
            .setScale(InventoryConstants.QUANTITY_SCALE);
        BigDecimal maximum = effective(receiptLine.getActualQuantity()).subtract(otherReturned)
            .max(BigDecimal.ZERO).setScale(InventoryConstants.QUANTITY_SCALE);
        if (newQuantity.compareTo(maximum) > 0) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_RETURN_QUANTITY_EXCEEDED,
                "纠错后退库数量超过原实发扣除其他已退库后的可退数量");
        }
        receiptLine.setReturnedQuantity(otherReturned.add(newQuantity).setScale(InventoryConstants.QUANTITY_SCALE));
        receiptLine.setUpdateTime(now);
        receiptLineMapper.updateById(receiptLine);
    }

    private void syncLedger(String orderType, Long orderId, String orderNo, Long sourceLineId,
                            Material material, String ledgerType, String subtype, BigDecimal newDelta,
                            boolean effective, Date occurredAt, String reason, Balance balance) {
        Ledger ledger = ledgerMapper.selectOne(Wrappers.<Ledger>lambdaQuery()
            .eq(Ledger::getTenantId, infrastructure.tenantId())
            .eq(Ledger::getRelatedOrderType, orderType)
            .eq(Ledger::getSourceLineId, sourceLineId).last("FOR UPDATE"));
        boolean insert = ledger == null;
        Date now = new Date();
        if (insert) {
            if (!effective) {
                return;
            }
            ledger = new Ledger();
            ledger.setLedgerId(IdWorker.getId());
            ledger.setTenantId(infrastructure.tenantId());
            ledger.setMaterialId(material.getMaterialId());
            ledger.setRelatedOrderType(orderType);
            ledger.setRelatedOrderId(orderId);
            ledger.setSourceLineId(sourceLineId);
            ledger.setVersion(0L);
            ledger.setCreateTime(now);
        } else if (!orderId.equals(ledger.getRelatedOrderId())) {
            throw InventoryBusinessException.rule("库存流水来源行关联异常");
        }
        ledger.setLedgerType(ledgerType);
        ledger.setBusinessSubtype(subtype);
        ledger.setQuantityDelta(newDelta.setScale(InventoryConstants.QUANTITY_SCALE));
        ledger.setBalanceAfter(balance.getQuantity());
        ledger.setRelatedOrderNo(StrUtil.trim(orderNo));
        ledger.setEffective(effective);
        ledger.setCorrectionReason(StrUtil.trim(reason));
        ledger.setOperatorEmployeeId(infrastructure.operatorId());
        ledger.setOperatorNameSnapshot(infrastructure.operatorName());
        ledger.setOccurredAt(occurredAt);
        ledger.setUpdateTime(now);
        if (insert) {
            ledgerMapper.insert(ledger);
        } else if (ledgerMapper.updateById(ledger) != 1) {
            throw versionConflict();
        }
        infrastructure.recomputeLedgerBalances(material.getMaterialId());
    }

    private OutboundLine newOutboundLine(OutboundOrder order, Material material,
                                         MaterialReceiptLine receiptLine, BigDecimal quantity, Date now) {
        OutboundLine line = new OutboundLine();
        line.setOutboundLineId(IdWorker.getId());
        line.setTenantId(order.getTenantId());
        line.setOutboundOrderId(order.getOutboundOrderId());
        line.setMaterialReceiptLineId(receiptLine == null ? null : receiptLine.getMaterialReceiptLineId());
        line.setMaterialId(material.getMaterialId());
        if (receiptLine == null) {
            snapshot(line, material);
        } else {
            line.setMaterialCodeSnapshot(receiptLine.getMaterialCodeSnapshot());
            line.setMaterialNameSnapshot(receiptLine.getMaterialNameSnapshot());
            line.setSpecificationSnapshot(receiptLine.getSpecificationSnapshot());
            line.setUnitSnapshot(receiptLine.getUnitSnapshot());
        }
        line.setRequestedQuantity(receiptLine == null ? quantity : receiptLine.getRequestedQuantity());
        line.setActualQuantity(quantity);
        line.setDeletedByKeeper(false);
        line.setVersion(0L);
        line.setCreateTime(now);
        line.setUpdateTime(now);
        return line;
    }

    private ReturnLine newReturnLine(ReturnOrder order, Material material,
                                     MaterialReceiptLine receiptLine, BigDecimal quantity, Date now) {
        ReturnLine line = new ReturnLine();
        line.setReturnLineId(IdWorker.getId());
        line.setTenantId(order.getTenantId());
        line.setReturnOrderId(order.getReturnOrderId());
        line.setMaterialReceiptLineId(receiptLine == null ? null : receiptLine.getMaterialReceiptLineId());
        line.setMaterialId(material.getMaterialId());
        if (receiptLine == null) {
            snapshot(line, material);
            line.setIssuedQuantitySnapshot(InventoryQuantity.ZERO);
            line.setReturnedBeforeSnapshot(InventoryQuantity.ZERO);
            line.setRemainingBeforeSnapshot(InventoryQuantity.ZERO);
        } else {
            line.setMaterialCodeSnapshot(receiptLine.getMaterialCodeSnapshot());
            line.setMaterialNameSnapshot(receiptLine.getMaterialNameSnapshot());
            line.setSpecificationSnapshot(receiptLine.getSpecificationSnapshot());
            line.setUnitSnapshot(receiptLine.getUnitSnapshot());
            line.setIssuedQuantitySnapshot(effective(receiptLine.getActualQuantity()));
            line.setReturnedBeforeSnapshot(effective(receiptLine.getReturnedQuantity()).subtract(quantity)
                .max(BigDecimal.ZERO).setScale(InventoryConstants.QUANTITY_SCALE));
            line.setRemainingBeforeSnapshot(effective(receiptLine.getActualQuantity())
                .subtract(line.getReturnedBeforeSnapshot()).max(BigDecimal.ZERO)
                .setScale(InventoryConstants.QUANTITY_SCALE));
        }
        line.setRequestedReturnQuantity(quantity);
        line.setActualReturnQuantity(quantity);
        line.setDeletedByKeeper(false);
        line.setVersion(0L);
        line.setCreateTime(now);
        line.setUpdateTime(now);
        return line;
    }

    private InboundVo toInboundVo(InboundOrder order) {
        InboundVo vo = new InboundVo();
        vo.setId(order.getInboundOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setSupplierName(order.getSupplierName());
        vo.setBusinessDate(order.getBusinessDate());
        vo.setVersion(order.getVersion());
        vo.setCreateTime(order.getCreateTime());
        vo.setRemark(order.getRemark());
        vo.setLines(inboundLineMapper.selectList(Wrappers.<InboundLine>lambdaQuery()
            .eq(InboundLine::getTenantId, infrastructure.tenantId())
            .eq(InboundLine::getInboundOrderId, order.getInboundOrderId())
            .gt(InboundLine::getQuantity, BigDecimal.ZERO)
            .orderByAsc(InboundLine::getInboundLineId)).stream().map(this::toInboundLineVo).toList());
        return vo;
    }

    private InboundCorrectionLogVo toInboundCorrectionLogVo(OperationLog log) {
        InboundVo before = infrastructure.readJsonOrNull(log.getBeforeSnapshot(), InboundVo.class);
        InboundVo after = infrastructure.readJsonOrNull(log.getAfterSnapshot(), InboundVo.class);
        InboundCorrectionLogVo vo = new InboundCorrectionLogVo();
        vo.setId(log.getOperationLogId());
        vo.setAction(log.getAction());
        vo.setReason(log.getReason());
        vo.setOperatorName(log.getOperatorNameSnapshot());
        vo.setCreateTime(log.getCreateTime());
        vo.setSnapshotAvailable(before != null && after != null);
        vo.setBefore(before);
        vo.setAfter(after);
        return vo;
    }

    private InboundLineVo toInboundLineVo(InboundLine line) {
        InboundLineVo vo = new InboundLineVo();
        vo.setId(line.getInboundLineId());
        vo.setInventoryMaterialId(line.getMaterialId());
        vo.setMaterialCode(line.getMaterialCodeSnapshot());
        vo.setMaterialName(line.getMaterialNameSnapshot());
        vo.setSpecification(line.getSpecificationSnapshot());
        vo.setUnit(line.getUnitSnapshot());
        vo.setQuantity(line.getQuantity());
        return vo;
    }

    /**
     * 出库、退库历史审计保存的是 order + lines 实体快照。读取时只投影为 Web 详情字段，
     * 避免把租户、审计和内部维护字段直接暴露给客户端。
     */
    private OutboundCorrectionLogVo toOutboundCorrectionLogVo(OperationLog log) {
        OutboundAuditSnapshot before = infrastructure.readJsonOrNull(log.getBeforeSnapshot(), OutboundAuditSnapshot.class);
        OutboundAuditSnapshot after = infrastructure.readJsonOrNull(log.getAfterSnapshot(), OutboundAuditSnapshot.class);
        OutboundVo beforeVo = toOutboundSnapshotVo(before);
        OutboundVo afterVo = toOutboundSnapshotVo(after);
        OutboundCorrectionLogVo vo = new OutboundCorrectionLogVo();
        vo.setId(log.getOperationLogId());
        vo.setAction(log.getAction());
        vo.setReason(log.getReason());
        vo.setOperatorName(log.getOperatorNameSnapshot());
        vo.setCreateTime(log.getCreateTime());
        vo.setSnapshotAvailable(beforeVo != null && afterVo != null);
        vo.setBefore(beforeVo);
        vo.setAfter(afterVo);
        return vo;
    }

    private ReturnCorrectionLogVo toReturnCorrectionLogVo(OperationLog log) {
        ReturnAuditSnapshot before = infrastructure.readJsonOrNull(log.getBeforeSnapshot(), ReturnAuditSnapshot.class);
        ReturnAuditSnapshot after = infrastructure.readJsonOrNull(log.getAfterSnapshot(), ReturnAuditSnapshot.class);
        ReturnVo beforeVo = toReturnSnapshotVo(before);
        ReturnVo afterVo = toReturnSnapshotVo(after);
        ReturnCorrectionLogVo vo = new ReturnCorrectionLogVo();
        vo.setId(log.getOperationLogId());
        vo.setAction(log.getAction());
        vo.setReason(log.getReason());
        vo.setOperatorName(log.getOperatorNameSnapshot());
        vo.setCreateTime(log.getCreateTime());
        vo.setSnapshotAvailable(beforeVo != null && afterVo != null);
        vo.setBefore(beforeVo);
        vo.setAfter(afterVo);
        return vo;
    }

    private ReturnOperationLogVo toReturnOperationLogVo(OperationLog log) {
        // 退库创建、修改、确认审计早期保存的是 ReturnVo；纠错和作废保存 order + lines。
        // 读取端同时兼容两种已落库格式，避免正常历史记录被误降级为快照不可用。
        ReturnVo beforeVo = readReturnOperationSnapshot(log.getBeforeSnapshot());
        ReturnVo afterVo = readReturnOperationSnapshot(log.getAfterSnapshot());
        ReturnOperationLogVo vo = new ReturnOperationLogVo();
        vo.setId(log.getOperationLogId());
        vo.setAction(log.getAction());
        vo.setReason(log.getReason());
        vo.setOperatorName(log.getOperatorNameSnapshot());
        vo.setCreateTime(log.getCreateTime());
        vo.setSnapshotAvailable(beforeVo != null || afterVo != null);
        vo.setBefore(beforeVo);
        vo.setAfter(afterVo);
        return vo;
    }

    private ReturnVo readReturnOperationSnapshot(String snapshotJson) {
        ReturnAuditSnapshot snapshot = infrastructure.readJsonOrNull(snapshotJson, ReturnAuditSnapshot.class);
        ReturnVo projected = toReturnSnapshotVo(snapshot);
        return projected != null ? projected : infrastructure.readJsonOrNull(snapshotJson, ReturnVo.class);
    }

    private OutboundVo toOutboundSnapshotVo(OutboundAuditSnapshot snapshot) {
        if (snapshot == null || snapshot.getOrder() == null || snapshot.getLines() == null) {
            return null;
        }
        OutboundOrder order = snapshot.getOrder();
        OutboundVo vo = new OutboundVo();
        vo.setId(order.getOutboundOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setSource(order.getSource());
        vo.setStatus(order.getStatus());
        vo.setMaterialReceiptId(order.getMaterialReceiptId());
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
        vo.setLines(snapshot.getLines().stream().map(this::toOutboundLineVo).toList());
        return vo;
    }

    private ReturnVo toReturnSnapshotVo(ReturnAuditSnapshot snapshot) {
        if (snapshot == null || snapshot.getOrder() == null || snapshot.getLines() == null) {
            return null;
        }
        ReturnOrder order = snapshot.getOrder();
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
        vo.setLines(snapshot.getLines().stream().map(this::toReturnLineVo).toList());
        return vo;
    }

    private OutboundLineVo toOutboundLineVo(OutboundLine line) {
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
        vo.setDeletedByKeeper(line.getDeletedByKeeper());
        return vo;
    }

    private ReturnLineVo toReturnLineVo(ReturnLine line) {
        ReturnLineVo vo = new ReturnLineVo();
        vo.setId(line.getReturnLineId());
        vo.setMaterialReceiptLineId(line.getMaterialReceiptLineId());
        vo.setInventoryMaterialId(line.getMaterialId());
        vo.setMaterialCode(line.getMaterialCodeSnapshot());
        vo.setMaterialName(line.getMaterialNameSnapshot());
        vo.setSpecification(line.getSpecificationSnapshot());
        vo.setUnit(line.getUnitSnapshot());
        vo.setIssuedQuantity(line.getIssuedQuantitySnapshot());
        vo.setReturnedQuantity(line.getReturnedBeforeSnapshot());
        vo.setRemainingReturnableQuantity(line.getRemainingBeforeSnapshot());
        vo.setRequestedReturnQuantity(line.getRequestedReturnQuantity());
        vo.setActualReturnQuantity(line.getActualReturnQuantity());
        vo.setDeletedByKeeper(line.getDeletedByKeeper());
        vo.setInvalidReason(line.getInvalidReason());
        return vo;
    }

    @lombok.Data
    private static class OutboundAuditSnapshot {
        private OutboundOrder order;
        private List<OutboundLine> lines;
    }

    @lombok.Data
    private static class ReturnAuditSnapshot {
        private ReturnOrder order;
        private List<ReturnLine> lines;
    }

    private Map<String, Object> outboundSnapshot(OutboundOrder order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("order", BeanUtil.copyProperties(order, OutboundOrder.class));
        snapshot.put("lines", outboundLineMapper.selectList(Wrappers.<OutboundLine>lambdaQuery()
            .eq(OutboundLine::getTenantId, infrastructure.tenantId())
            .eq(OutboundLine::getOutboundOrderId, order.getOutboundOrderId())
            .orderByAsc(OutboundLine::getOutboundLineId)));
        return snapshot;
    }

    private Map<String, Object> returnSnapshot(ReturnOrder order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("order", BeanUtil.copyProperties(order, ReturnOrder.class));
        snapshot.put("lines", returnLineMapper.selectList(Wrappers.<ReturnLine>lambdaQuery()
            .eq(ReturnLine::getTenantId, infrastructure.tenantId())
            .eq(ReturnLine::getReturnOrderId, order.getReturnOrderId())
            .orderByAsc(ReturnLine::getReturnLineId)));
        return snapshot;
    }

    private String voidReason(String reason) {
        return StrUtil.isBlank(reason) ? "未填写作废原因" : StrUtil.trim(reason);
    }

    /**
     * 原退库流水已使用退库明细 ID 作为来源键。作废冲销需要新增一条流水，使用其负值既保留与原明细的
     * 稳定对应关系，也避免触发 {@code (tenant_id, related_order_type, source_line_id)} 唯一约束。
     */
    private Long returnVoidLedgerSourceLineId(Long returnLineId) {
        if (returnLineId == null || returnLineId <= 0) {
            throw InventoryBusinessException.rule("退库明细来源行异常");
        }
        return -returnLineId;
    }

    private void updateInbound(InboundOrder order) {
        try {
            if (inboundOrderMapper.updateById(order) != 1) {
                throw versionConflict();
            }
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("入库单号已存在");
        }
    }

    private InboundOrder requireInboundForHistory(Long id) {
        InboundOrder order = inboundOrderMapper.selectOne(Wrappers.<InboundOrder>lambdaQuery()
            .eq(InboundOrder::getTenantId, infrastructure.tenantId())
            .eq(InboundOrder::getInboundOrderId, id));
        if (order == null) {
            throw notFound("入库单不存在");
        }
        return order;
    }

    private void updateOutbound(OutboundOrder order) {
        try {
            if (outboundOrderMapper.updateById(order) != 1) {
                throw versionConflict();
            }
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("出库单号已存在");
        }
    }

    private void updateReturn(ReturnOrder order) {
        try {
            if (returnOrderMapper.updateById(order) != 1) {
                throw versionConflict();
            }
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("退库单号已存在");
        }
    }

    private void touch(InboundOrder order, Date now) {
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(now);
    }

    private void touch(OutboundOrder order, Date now) {
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(now);
    }

    private void touch(ReturnOrder order, Date now) {
        order.setUpdateBy(infrastructure.operatorId());
        order.setUpdateTime(now);
    }

    private String correctionReason(CorrectionDocumentBo bo) {
        return StrUtil.blankToDefault(StrUtil.trim(bo.getCorrectionReason()), StrUtil.trim(bo.getRemark()));
    }

    private BigDecimal effective(BigDecimal value) {
        return value == null ? InventoryQuantity.ZERO : value.setScale(InventoryConstants.QUANTITY_SCALE);
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

    private InventoryBusinessException alreadyProcessed(String message) {
        return new InventoryBusinessException(InventoryConstants.ERROR_ORDER_ALREADY_PROCESSED, message);
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

    private void snapshot(ReturnLine line, Material material) {
        line.setMaterialCodeSnapshot(material.getMaterialCode());
        line.setMaterialNameSnapshot(material.getMaterialName());
        line.setSpecificationSnapshot(material.getSpecification());
        line.setUnitSnapshot(material.getUnit());
    }
}
