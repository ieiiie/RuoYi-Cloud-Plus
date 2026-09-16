package com.ym.agriculture.farmtask.inventory.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.BalanceMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.BusinessSequenceMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.IdempotencyMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.LedgerMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.LegacyMaterialMapMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.OperationLogMapper;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Balance;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.BusinessSequence;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Idempotency;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Ledger;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.LegacyMaterialMap;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Material;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OperationLog;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;
import com.ym.agriculture.farmtask.inventory.support.InventoryQuantity;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

/** 库存事务共享的租户、余额、编号、审计和持久化幂等能力。 */
@Component
@RequiredArgsConstructor
public class InventoryInfrastructure {

    private final MaterialMapper materialMapper;
    private final BalanceMapper balanceMapper;
    private final LedgerMapper ledgerMapper;
    private final BusinessSequenceMapper sequenceMapper;
    private final IdempotencyMapper idempotencyMapper;
    private final OperationLogMapper operationLogMapper;
    private final LegacyMaterialMapMapper legacyMaterialMapMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final ObjectMapper objectMapper;

    public String tenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            throw InventoryBusinessException.rule("缺少租户上下文");
        }
        return tenantId;
    }

    public Long operatorId() {
        return LoginHelper.getUserId();
    }

    public String operatorName() {
        Long userId = operatorId();
        if (userId != null) {
            List<SysEmployeeVo> rows = employeeAccessor.queryBasicByIds(List.of(userId));
            if (!rows.isEmpty() && StrUtil.isNotBlank(rows.get(0).getName())) {
                return rows.get(0).getName();
            }
        }
        try {
            return LoginHelper.getLoginUser() == null ? "系统" : LoginHelper.getLoginUser().getUsername();
        } catch (RuntimeException ignored) {
            return "系统";
        }
    }

    public Material requireMaterial(Long materialId) {
        Material material = materialMapper.selectTenantById(tenantId(), materialId);
        if (material == null) {
            boolean legacy = legacyMaterialMapMapper.exists(Wrappers.<LegacyMaterialMap>lambdaQuery()
                .eq(LegacyMaterialMap::getTenantId, tenantId())
                .eq(LegacyMaterialMap::getLegacyMaterialId, materialId));
            if (legacy) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_LEGACY_MATERIAL_ID,
                    "旧库存物资ID已停用，请刷新后选择新物资");
            }
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404,
                "物资不存在或无权访问");
        }
        return material;
    }

    public Material requireEnabledMaterial(Long materialId) {
        Material material = requireMaterial(materialId);
        if (!Boolean.TRUE.equals(material.getEnabled())) {
            throw InventoryBusinessException.rule("物资已停用：" + material.getMaterialName());
        }
        return material;
    }

    public Balance requireBalanceForUpdate(Long materialId) {
        Balance balance = balanceMapper.selectForUpdate(tenantId(), materialId);
        if (balance == null) {
            throw InventoryBusinessException.rule("物资余额不存在，请先执行库存初始化");
        }
        return balance;
    }

    public Balance applyDelta(Material material, BigDecimal delta, String ledgerType, String subtype,
                              String orderType, Long orderId, String orderNo, Long sourceLineId,
                              boolean allowNegative, String correctionReason) {
        return applyDelta(material, delta, ledgerType, subtype, orderType, orderId, orderNo, sourceLineId,
            allowNegative, correctionReason, new Date());
    }

    /** 按明确的业务发生时间新增库存事实流水。 */
    public Balance applyDelta(Material material, BigDecimal delta, String ledgerType, String subtype,
                              String orderType, Long orderId, String orderNo, Long sourceLineId,
                              boolean allowNegative, String correctionReason, Date occurredAt) {
        Balance balance = requireBalanceForUpdate(material.getMaterialId());
        if (delta.compareTo(BigDecimal.ZERO) < 0 && Boolean.TRUE.equals(balance.getOutboundLocked())) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_INVENTORY_LOCKED,
                "物资已进入异常锁定状态，禁止出库");
        }
        BigDecimal next = balance.getQuantity().add(delta).setScale(1);
        if (next.compareTo(BigDecimal.ZERO) < 0 && !allowNegative) {
            InventoryBusinessException.LineError lineError = new InventoryBusinessException.LineError(
                sourceLineId, material.getMaterialId(), InventoryConstants.ERROR_INVENTORY_INSUFFICIENT,
                "库存不足", balance.getQuantity().toPlainString(), null);
            throw new InventoryBusinessException(InventoryConstants.ERROR_INVENTORY_INSUFFICIENT, 409,
                "库存不足，整单未生效", List.of(lineError));
        }
        Date now = new Date();
        balance.setQuantity(next);
        balance.setUpdateBy(operatorId());
        balance.setUpdateTime(now);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            balance.setOutboundLocked(true);
            if (StrUtil.isNotBlank(correctionReason)) {
                balance.setAbnormalReason(StrUtil.trim(correctionReason));
            }
        }
        if (balanceMapper.updateById(balance) != 1) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_VERSION_CONFLICT,
                "库存余额已变化，请刷新后重试");
        }

        appendLedger(material, delta, ledgerType, subtype, orderType, orderId, orderNo, sourceLineId,
            correctionReason, occurredAt, next);
        return balance;
    }

    /** 历史冲销：沿用历史纠错的负库存确认规则，同时新增一条不可覆盖的库存事实流水。 */
    public Balance applyCorrectionDelta(Material material, BigDecimal delta, String ledgerType, String subtype,
                                        String orderType, Long orderId, String orderNo, Long sourceLineId,
                                        boolean allowNegative, String correctionReason, Date occurredAt) {
        Balance balance = adjustBalanceForCorrection(material, delta, allowNegative, correctionReason, sourceLineId);
        appendLedger(material, delta, ledgerType, subtype, orderType, orderId, orderNo, sourceLineId,
            correctionReason, occurredAt, balance.getQuantity());
        return balance;
    }

    /**
     * 历史纠错仅调整余额缓存，不新增补偿流水；调用方随后必须就地同步原流水。
     * 可能产生负库存时，具备既有单据纠错或作废权限的用户必须显式确认，
     * 但不再附加超级管理员身份或必填原因限制。
     */
    public Balance adjustBalanceForCorrection(Material material, BigDecimal delta, boolean allowNegative,
                                                String correctionReason, Long sourceLineId) {
        Balance balance = requireBalanceForUpdate(material.getMaterialId());
        BigDecimal next = balance.getQuantity().add(delta).setScale(InventoryConstants.QUANTITY_SCALE);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            if (!allowNegative) {
                InventoryBusinessException.LineError lineError = new InventoryBusinessException.LineError(
                    sourceLineId, material.getMaterialId(),
                    InventoryConstants.ERROR_NEGATIVE_CORRECTION_CONFIRMATION_REQUIRED,
                    "本次操作会导致负库存，请确认允许负库存", balance.getQuantity().toPlainString(), null);
                throw new InventoryBusinessException(
                    InventoryConstants.ERROR_NEGATIVE_CORRECTION_CONFIRMATION_REQUIRED, 409,
                    "本次操作会导致负库存，请勾选允许负库存后确认提交", List.of(lineError));
            }
            balance.setOutboundLocked(true);
            // 作废回滚和历史纠错都允许不填写原因；明确确认后仍锁定出库，
            // 避免异常库存继续流转。
            balance.setAbnormalReason(
                StrUtil.isBlank(correctionReason) ? null : StrUtil.trim(correctionReason));
        } else {
            balance.setOutboundLocked(false);
            balance.setAbnormalReason(null);
        }
        balance.setQuantity(next);
        balance.setUpdateBy(operatorId());
        balance.setUpdateTime(new Date());
        if (balanceMapper.updateById(balance) != 1) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_VERSION_CONFLICT,
                "库存余额已变化，请刷新后重试");
        }
        return balance;
    }

    private void appendLedger(Material material, BigDecimal delta, String ledgerType, String subtype,
                              String orderType, Long orderId, String orderNo, Long sourceLineId,
                              String correctionReason, Date occurredAt, BigDecimal balanceAfter) {
        Date now = new Date();
        Ledger ledger = new Ledger();
        ledger.setLedgerId(IdWorker.getId());
        ledger.setTenantId(tenantId());
        ledger.setMaterialId(material.getMaterialId());
        ledger.setLedgerType(ledgerType);
        ledger.setBusinessSubtype(subtype);
        ledger.setQuantityDelta(delta.setScale(InventoryConstants.QUANTITY_SCALE));
        ledger.setBalanceAfter(balanceAfter);
        ledger.setRelatedOrderType(orderType);
        ledger.setRelatedOrderId(orderId);
        ledger.setRelatedOrderNo(orderNo);
        ledger.setSourceLineId(sourceLineId);
        ledger.setEffective(true);
        ledger.setVersion(0L);
        ledger.setCorrectionReason(correctionReason);
        ledger.setOperatorEmployeeId(operatorId());
        ledger.setOperatorNameSnapshot(operatorName());
        ledger.setOccurredAt(occurredAt == null ? now : occurredAt);
        ledger.setCreateTime(now);
        ledger.setUpdateTime(now);
        ledgerMapper.insert(ledger);
        recomputeLedgerBalances(material.getMaterialId());
    }

    /**
     * 按业务发生时间重算某物资全部流水的行后余额。历史改单、物理删除及补录早期单据后调用，
     * 保证事实流水自身可独立还原余额。
     */
    public void recomputeLedgerBalances(Long materialId) {
        List<Ledger> ledgers = ledgerMapper.selectList(Wrappers.<Ledger>lambdaQuery()
            .eq(Ledger::getTenantId, tenantId())
            .eq(Ledger::getMaterialId, materialId)
            .orderByAsc(Ledger::getOccurredAt)
            .orderByAsc(Ledger::getLedgerId)
            .last("FOR UPDATE"));
        BigDecimal running = InventoryQuantity.ZERO;
        Date now = new Date();
        for (Ledger ledger : ledgers) {
            if (Boolean.TRUE.equals(ledger.getEffective())) {
                running = running.add(ledger.getQuantityDelta())
                    .setScale(InventoryConstants.QUANTITY_SCALE);
            }
            if (ledger.getBalanceAfter() == null || ledger.getBalanceAfter().compareTo(running) != 0) {
                ledger.setBalanceAfter(running);
                ledger.setUpdateTime(now);
                if (ledgerMapper.updateById(ledger) != 1) {
                    throw new InventoryBusinessException(InventoryConstants.ERROR_VERSION_CONFLICT,
                        "库存流水已变化，请刷新后重试");
                }
            }
        }
    }

    public String nextOrderNo(String type) {
        String tenantId = tenantId();
        Date today = DateUtil.beginOfDay(new Date());
        BusinessSequence sequence = sequenceMapper.selectForUpdate(tenantId, type, today);
        Date now = new Date();
        if (sequence == null) {
            sequence = new BusinessSequence();
            sequence.setSequenceId(IdWorker.getId());
            sequence.setTenantId(tenantId);
            sequence.setBusinessType(type);
            sequence.setSequenceDate(today);
            sequence.setCurrentValue(1L);
            sequence.setVersion(0L);
            sequence.setCreateTime(now);
            sequence.setUpdateTime(now);
            sequenceMapper.insert(sequence);
        } else {
            sequence.setCurrentValue(sequence.getCurrentValue() + 1);
            sequence.setUpdateTime(now);
            if (sequenceMapper.updateById(sequence) != 1) {
                throw new InventoryBusinessException(InventoryConstants.ERROR_VERSION_CONFLICT,
                    "业务编号生成冲突，请重试");
            }
        }
        String prefix = switch (type) {
            case "INBOUND" -> "RK";
            case "OUTBOUND" -> "CK";
            case "RETURN" -> "TK";
            case "STOCKTAKE" -> "PD";
            case "RECEIPT" -> "LL";
            default -> "KC";
        };
        return prefix + DateUtil.format(today, "yyyyMMdd") + StrUtil.padPre(String.valueOf(sequence.getCurrentValue()), 5, '0');
    }

    public void audit(String aggregateType, Long aggregateId, String action, String reason,
                      Object before, Object after) {
        OperationLog log = new OperationLog();
        log.setOperationLogId(IdWorker.getId());
        log.setTenantId(tenantId());
        log.setAggregateType(aggregateType);
        log.setAggregateId(aggregateId);
        log.setAction(action);
        log.setReason(reason);
        log.setBeforeSnapshot(toJson(before));
        log.setAfterSnapshot(toJson(after));
        log.setOperatorEmployeeId(operatorId());
        log.setOperatorNameSnapshot(operatorName());
        log.setCreateTime(new Date());
        operationLogMapper.insert(log);
    }

    public MutationVo idempotent(String operationType, String key, Object request,
                                 Supplier<MutationVo> operation) {
        if (StrUtil.isBlank(key)) {
            throw InventoryBusinessException.rule("幂等键不能为空");
        }
        String tenantId = tenantId();
        String hash = DigestUtil.sha256Hex(toJson(request));
        Idempotency current = idempotencyMapper.selectByScope(tenantId, operationType, key);
        if (current != null) {
            return replay(current, hash);
        }
        Date now = new Date();
        Idempotency record = new Idempotency();
        record.setIdempotencyId(IdWorker.getId());
        record.setTenantId(tenantId);
        record.setOperatorEmployeeId(operatorId());
        record.setOperationType(operationType);
        record.setIdempotencyKey(key);
        record.setRequestHash(hash);
        record.setOperationStatus("PROCESSING");
        record.setCreateTime(now);
        record.setUpdateTime(now);
        try {
            idempotencyMapper.insert(record);
        } catch (DuplicateKeyException exception) {
            return replay(idempotencyMapper.selectByScope(tenantId, operationType, key), hash);
        }
        MutationVo result = operation.get();
        record.setOperationStatus("SUCCEEDED");
        record.setResponseJson(toJson(result));
        record.setUpdateTime(new Date());
        idempotencyMapper.updateById(record);
        return result;
    }

    private MutationVo replay(Idempotency record, String hash) {
        if (record == null || !StrUtil.equals(record.getRequestHash(), hash)) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_IDEMPOTENCY_CONFLICT,
                "相同幂等键对应的请求内容不同");
        }
        if (!"SUCCEEDED".equals(record.getOperationStatus())) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_IDEMPOTENCY_PROCESSING,
                "请求正在处理中，请稍后刷新");
        }
        try {
            return objectMapper.readValue(record.getResponseJson(), MutationVo.class);
        } catch (JsonProcessingException exception) {
            throw InventoryBusinessException.rule("幂等结果读取失败");
        }
    }

    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw InventoryBusinessException.rule("操作快照序列化失败");
        }
    }

    /**
     * 尝试读取已有审计快照。历史异常数据只影响对应一条展示记录，不能中断单据详情。
     */
    public <T> T readJsonOrNull(String json, Class<T> valueType) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
