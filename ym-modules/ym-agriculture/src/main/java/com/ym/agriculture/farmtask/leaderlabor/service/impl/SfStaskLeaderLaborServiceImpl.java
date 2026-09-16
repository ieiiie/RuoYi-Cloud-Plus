package com.ym.agriculture.farmtask.leaderlabor.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.clocklocation.service.ISfStaskClockLocationService;
import com.ym.agriculture.shared.i18n.StaskErrorCodes;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import com.ym.agriculture.farmtask.inventory.service.TaskMaterialLifecycleCoordinator;
import com.ym.agriculture.farmtask.leaderlabor.dao.SfStaskLeaderLaborRecordMapper;
import com.ym.agriculture.farmtask.leaderlabor.dao.SfStaskOperationIdempotencyMapper;
import com.ym.agriculture.farmtask.leaderlabor.model.bo.*;
import com.ym.agriculture.farmtask.leaderlabor.model.entity.SfStaskLeaderLaborRecord;
import com.ym.agriculture.farmtask.leaderlabor.model.entity.SfStaskOperationIdempotency;
import com.ym.agriculture.farmtask.leaderlabor.model.vo.*;
import com.ym.agriculture.farmtask.leaderlabor.service.ISfStaskLeaderLaborService;
import com.ym.agriculture.farmtask.leaderlabor.support.StaskClockEvidenceSupport;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskClockRecordMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskFlowLogMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskLeaderAcceptBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskClockRecord;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.voice.service.ISfStaskVoiceBroadcastService;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskOrderStateMachine;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Supplier;

/** 组长批量接单、打卡与计划日用工记录命令/查询服务。 */
@Service
@RequiredArgsConstructor
public class SfStaskLeaderLaborServiceImpl implements ISfStaskLeaderLaborService {

    private final StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskLeaderLaborRecordMapper laborRecordMapper;
    private final SfStaskOperationIdempotencyMapper idempotencyMapper;
    private final SfStaskClockRecordMapper clockRecordMapper;
    private final SfStaskFlowLogMapper flowLogMapper;
    private final ISfStaskClockLocationService clockLocationService;
    private final ISfStaskVoiceBroadcastService voiceBroadcastService;
    private final SfStaskOrderStateMachine stateMachine;
    private final SfStaskEmployeeAccessor employeeAccessor;

    @Autowired(required = false)
    private TaskMaterialLifecycleCoordinator taskMaterialCoordinator;

    @Override
    public List<SfStaskLaborDateOptionVo> batchAcceptDateOptions() {
        return dateOptions(StaskOrderStatus.PENDING_LEADER_ACCEPT);
    }

    @Override
    public SfStaskBatchAcceptTasksVo batchAcceptTasks(LocalDate planDate) {
        SfStaskBatchAcceptTasksVo result = new SfStaskBatchAcceptTasksVo();
        result.setPlanDate(planDate);
        result.setRows(tasks(planDate, StaskOrderStatus.PENDING_LEADER_ACCEPT));
        SfStaskLeaderLaborRecord record = laborRecordMapper.selectByLeaderAndPlanDate(tenantId(), currentLeaderId(), planDate);
        if (record != null) {
            result.setLaborRecordId(record.getLaborRecordId());
            result.setDailyLaborCount(record.getLaborCount());
            result.setLaborRecordVersion(record.getVersion());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfStaskBatchMutationVo batchAccept(SfStaskBatchAcceptBo bo) {
        return idempotent("BATCH_ACCEPT", bo.getIdempotencyKey(), bo, () -> acceptOrders(bo.getPlanDate(), bo.getOrderIds(),
            bo.getDailyLaborCount(), bo.getLaborRecordVersion(), "BATCH_ACCEPT"));
    }

    @Override
    public List<SfStaskLaborDateOptionVo> batchClockInDateOptions() {
        return dateOptions(StaskOrderStatus.ASSIGN_COMPLETE);
    }

    @Override
    public List<SfStaskBatchTaskVo> batchClockInTasks(LocalDate planDate) {
        return tasks(planDate, StaskOrderStatus.ASSIGN_COMPLETE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfStaskBatchMutationVo batchClockIn(SfStaskBatchClockInBo bo) {
        return idempotent("BATCH_CLOCK_IN", bo.getIdempotencyKey(), bo, () -> {
            List<SfStaskWorkOrder> orders = lockAndValidate(bo.getOrderIds(), bo.getPlanDate(), StaskOrderStatus.ASSIGN_COMPLETE);
            StaskClockEvidenceSupport.ClockEvidence evidence = StaskClockEvidenceSupport.validate(bo.getClockType(),
                bo.getLongitude(), bo.getLatitude(), bo.getProofPhotos(),
                clockLocationService.getByTenantId(tenantId()), messages);
            Date now = new Date();
            for (SfStaskWorkOrder order : orders) {
                SfStaskClockRecord record = new SfStaskClockRecord();
                record.setClockId(IdWorker.getId()); record.setTenantId(order.getTenantId()); record.setOrderId(order.getOrderId());
                record.setLeaderId(order.getLeaderId()); record.setClockType(evidence.clockType());
                record.setLongitude(evidence.longitude()); record.setLatitude(evidence.latitude());
                record.setDistanceMeters(evidence.distanceMeters()); record.setProofPhotos(evidence.proofPhotos());
                record.setClockTime(now); record.setCreateTime(now); clockRecordMapper.insert(record);
                transit(order, StaskOrderEvent.CLOCK_IN, "组长批量到岗打卡");
            }
            if (taskMaterialCoordinator != null) {
                taskMaterialCoordinator.markArrivedForOrders(orders);
            }
            SfStaskBatchMutationVo result = result(orders);
            result.setArrivedAt(now);
            return result;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int singleAccept(Long orderId, SfStaskLeaderAcceptBo bo) {
        BigDecimal daily = bo.getDailyLaborCount();
        if (daily == null && bo.getRequiredWorkerCount() != null) daily = BigDecimal.valueOf(bo.getRequiredWorkerCount());
        if (daily == null || daily.signum() < 0 || (bo.getRequiredWorkerCount() != null
            && daily.compareTo(BigDecimal.valueOf(bo.getRequiredWorkerCount())) != 0)) {
            throw messages.stableException(StaskErrorCodes.BATCH_TASK_INVALID, StaskMessageKeys.VALIDATION_REQUIRED_WORKER_COUNT_REQUIRED);
        }
        SfStaskWorkOrder source = workOrderMapper.selectById(orderId);
        if (source == null || source.getPlanDate() == null) throw messages.exception(StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND);
        acceptOrders(orderPlanDate(source), List.of(orderId), daily, bo.getLaborRecordVersion(), "SINGLE_ACCEPT");
        return 1;
    }

    private SfStaskBatchMutationVo acceptOrders(LocalDate planDate, List<Long> orderIds, BigDecimal dailyLaborCount,
        Long laborRecordVersion, String source) {
        List<SfStaskWorkOrder> orders = lockAndValidate(orderIds, planDate, StaskOrderStatus.PENDING_LEADER_ACCEPT);
        SfStaskLeaderLaborRecord record = saveLabor(planDate, dailyLaborCount, laborRecordVersion, source);
        for (SfStaskWorkOrder order : orders) {
            transit(order, StaskOrderEvent.LEADER_ACCEPT, "组长接单");
            voiceBroadcastService.preGenerateForOrder(order.getOrderId());
        }
        if (taskMaterialCoordinator != null) {
            taskMaterialCoordinator.ensureReceiptsForAcceptedOrders(orders);
        }
        SfStaskBatchMutationVo result = result(orders);
        result.setLaborRecordId(record.getLaborRecordId()); result.setPlanDate(planDate);
        result.setDailyLaborCount(record.getLaborCount()); result.setVersion(record.getVersion());
        return result;
    }

    private SfStaskLeaderLaborRecord saveLabor(LocalDate planDate, BigDecimal count, Long version, String source) {
        Long leaderId = currentLeaderId();
        SfStaskLeaderLaborRecord record = laborRecordMapper.selectByLeaderAndPlanDateForUpdate(tenantId(), leaderId, planDate);
        Date now = new Date();
        if (record == null) {
            if (version != null) throw messages.stableException(StaskErrorCodes.LABOR_RECORD_VERSION_CONFLICT,
                StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
            record = new SfStaskLeaderLaborRecord(); record.setLaborRecordId(IdWorker.getId()); record.setTenantId(tenantId());
            record.setLeaderEmployeeId(leaderId); record.setLeaderNameSnapshot(currentLeaderName()); record.setPlanDate(planDate);
            record.setLaborCount(count); record.setVersion(0L); record.setLastSource(source); record.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now)); record.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
            laborRecordMapper.insert(record);
            return record;
        }
        if (!Objects.equals(record.getVersion(), version)) throw messages.stableException(StaskErrorCodes.LABOR_RECORD_VERSION_CONFLICT,
            StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        record.setLaborCount(count); record.setLastSource(source); record.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        if (laborRecordMapper.updateById(record) <= 0) throw messages.stableException(StaskErrorCodes.LABOR_RECORD_VERSION_CONFLICT,
            StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        return record;
    }

    private List<SfStaskWorkOrder> lockAndValidate(List<Long> input, LocalDate planDate, String requiredStatus) {
        if (input == null || input.isEmpty() || input.stream().anyMatch(Objects::isNull) || new HashSet<>(input).size() != input.size())
            throw messages.stableException(StaskErrorCodes.BATCH_TASK_INVALID, StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND);
        List<Long> ids = input.stream().sorted().toList();
        List<SfStaskWorkOrder> orders = workOrderMapper.selectByIdsForUpdate(tenantId(), ids);
        if (orders.size() != ids.size()) throw messages.stableException(StaskErrorCodes.BATCH_TASK_INVALID, StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND);
        Long leaderId = currentLeaderId();
        for (SfStaskWorkOrder order : orders) {
            if (!Objects.equals(order.getLeaderId(), leaderId)) throw messages.stableException(StaskErrorCodes.LABOR_RECORD_FORBIDDEN,
                StaskMessageKeys.ERROR_EXECUTION_LEADER_ONLY);
            if (!Objects.equals(orderPlanDate(order), planDate) || !Objects.equals(order.getStatus(), requiredStatus))
                throw messages.stableException(StaskErrorCodes.BATCH_TASK_STATUS_CHANGED, StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        }
        return orders;
    }

    private void transit(SfStaskWorkOrder order, String event, String remark) {
        String before = order.getStatus(); String after = stateMachine.transit(before, event); order.setStatus(after);
        if (workOrderMapper.updateById(order) <= 0) throw messages.stableException(StaskErrorCodes.BATCH_TASK_STATUS_CHANGED,
            StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        SfStaskFlowLog log = new SfStaskFlowLog(); log.setLogId(IdWorker.getId()); log.setTenantId(order.getTenantId());
        log.setOrderId(order.getOrderId()); log.setPackageId(order.getPackageId()); log.setFromStatus(before); log.setToStatus(after);
        log.setEvent(event); log.setOperatorEmployeeId(LoginHelper.getUserId()); log.setOperatorRoleCode(EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER);
        log.setRemark(remark); log.setCreateTime(new Date()); flowLogMapper.insert(log);
    }

    private List<SfStaskLaborDateOptionVo> dateOptions(String status) {
        Map<LocalDate, Long> grouped = workOrderMapper.selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
                .eq(SfStaskWorkOrder::getTenantId, tenantId()).eq(SfStaskWorkOrder::getLeaderId, currentLeaderId())
                .eq(SfStaskWorkOrder::getStatus, status).isNotNull(SfStaskWorkOrder::getPlanDate))
            .stream().collect(java.util.stream.Collectors.groupingBy(this::orderPlanDate, TreeMap::new, java.util.stream.Collectors.counting()));
        return grouped.entrySet().stream().map(entry -> { SfStaskLaborDateOptionVo vo = new SfStaskLaborDateOptionVo(); vo.setPlanDate(entry.getKey()); vo.setTaskCount(entry.getValue()); return vo; }).toList();
    }

    private List<SfStaskBatchTaskVo> tasks(LocalDate planDate, String status) {
        if (planDate == null) return List.of();
        List<SfStaskWorkOrder> orders = workOrderMapper.selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId()).eq(SfStaskWorkOrder::getLeaderId, currentLeaderId())
            .ge(SfStaskWorkOrder::getPlanDate, planStart(planDate)).lt(SfStaskWorkOrder::getPlanDate, planEnd(planDate))
            .eq(SfStaskWorkOrder::getStatus, status).orderByAsc(SfStaskWorkOrder::getOrderId));
        SfStaskLeaderLaborRecord labor = laborRecordMapper.selectByLeaderAndPlanDate(tenantId(), currentLeaderId(), planDate);
        return orders.stream().map(order -> { SfStaskBatchTaskVo vo = toBatchTask(order); if (labor != null) vo.setDailyLaborCount(labor.getLaborCount()); return vo; }).toList();
    }

    private <T> T idempotent(String operation, String key, Object request, Supplier<T> action) {
        String hash = DigestUtil.sha256Hex(JSON.toJSONString(request)); SfStaskOperationIdempotency old = idempotencyMapper.selectByScope(tenantId(), LoginHelper.getUserId(), operation, key);
        if (old != null) {
            if (!Objects.equals(old.getRequestHash(), hash)) throw messages.stableException(StaskErrorCodes.IDEMPOTENCY_CONFLICT, StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
            if ("SUCCEEDED".equals(old.getOperationStatus())) return JSON.parseObject(old.getResponseJson(), (Class<T>) SfStaskBatchMutationVo.class);
            throw messages.stableException(StaskErrorCodes.IDEMPOTENCY_PROCESSING, StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        }
        SfStaskOperationIdempotency entity = new SfStaskOperationIdempotency(); entity.setOperationId(IdWorker.getId()); entity.setTenantId(tenantId());
        entity.setEmployeeId(LoginHelper.getUserId()); entity.setOperationType(operation); entity.setIdempotencyKey(key); entity.setRequestHash(hash); entity.setOperationStatus("PROCESSING"); entity.setCreateTime(new Date()); entity.setUpdateTime(new Date()); idempotencyMapper.insert(entity);
        T response = action.get(); entity.setOperationStatus("SUCCEEDED"); entity.setResponseJson(JSON.toJSONString(response)); entity.setUpdateTime(new Date()); idempotencyMapper.updateById(entity); return response;
    }

    private SfStaskBatchMutationVo result(List<SfStaskWorkOrder> orders) { SfStaskBatchMutationVo result = new SfStaskBatchMutationVo(); result.setCount(orders.size()); result.setOrderIds(orders.stream().map(SfStaskWorkOrder::getOrderId).toList()); return result; }
    private String tenantId() { return TenantHelper.getTenantId(); }
    private Long currentLeaderId() { Long id = LoginHelper.getUserId(); if (id == null || !employeeAccessor.hasAppRole(id, EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER)) throw messages.stableException(StaskErrorCodes.LABOR_RECORD_FORBIDDEN, StaskMessageKeys.ERROR_EXECUTION_LEADER_ONLY); return id; }
    private String currentLeaderName() { List<SysEmployeeVo> employees = employeeAccessor.queryBasicByIds(List.of(currentLeaderId())); return employees == null || employees.isEmpty() ? null : employees.get(0).getName(); }

    @Override
    public PageResult<SfStaskLaborRecordVo> page(SfStaskLaborRecordQueryBo bo, PageQuery pageQuery) {
        boolean own = isGroupLeader();
        if (!own) requireReadonlyLaborRole();
        if (bo == null) bo = new SfStaskLaborRecordQueryBo();
        List<SfStaskLeaderLaborRecord> records = laborRecordMapper.selectList(Wrappers.<SfStaskLeaderLaborRecord>lambdaQuery()
            .eq(SfStaskLeaderLaborRecord::getTenantId, tenantId())
            .eq(own, SfStaskLeaderLaborRecord::getLeaderEmployeeId, LoginHelper.getUserId())
            .eq(!own && bo.getLeaderEmployeeId() != null, SfStaskLeaderLaborRecord::getLeaderEmployeeId, bo.getLeaderEmployeeId())
            .ge(bo.getPlanDateStart() != null, SfStaskLeaderLaborRecord::getPlanDate, bo.getPlanDateStart())
            .le(bo.getPlanDateEnd() != null, SfStaskLeaderLaborRecord::getPlanDate, bo.getPlanDateEnd())
            .orderByDesc(SfStaskLeaderLaborRecord::getPlanDate).orderByAsc(SfStaskLeaderLaborRecord::getLeaderEmployeeId));
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(records.stream().map(record -> toVo(record, own)).toList(), pageQuery.build());
    }

    @Override
    public List<SfStaskLaborLeaderOptionVo> leaders() {
        requireReadonlyLaborRole();
        return laborRecordMapper.selectList(Wrappers.<SfStaskLeaderLaborRecord>lambdaQuery().eq(SfStaskLeaderLaborRecord::getTenantId, tenantId())
                .select(SfStaskLeaderLaborRecord::getLeaderEmployeeId, SfStaskLeaderLaborRecord::getLeaderNameSnapshot)
                .groupBy(SfStaskLeaderLaborRecord::getLeaderEmployeeId, SfStaskLeaderLaborRecord::getLeaderNameSnapshot))
            .stream().map(record -> { SfStaskLaborLeaderOptionVo vo = new SfStaskLaborLeaderOptionVo(); vo.setLeaderEmployeeId(record.getLeaderEmployeeId()); vo.setLeaderEmployeeName(record.getLeaderNameSnapshot()); return vo; }).toList();
    }

    @Override
    public SfStaskLaborRecordDetailVo detail(Long laborRecordId) {
        SfStaskLeaderLaborRecord record = requireRecord(laborRecordId);
        boolean own = isGroupLeader();
        if (!own) requireReadonlyLaborRole();
        if (own && !Objects.equals(record.getLeaderEmployeeId(), LoginHelper.getUserId())) forbidden();
        SfStaskLaborRecordDetailVo detail = new SfStaskLaborRecordDetailVo(); BeanUtil.copyProperties(toVo(record, own), detail);
        detail.setTasks(tasksForLeader(record.getLeaderEmployeeId(), record.getPlanDate())); return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfStaskLaborRecordVo update(Long laborRecordId, SfStaskLaborRecordUpdateBo bo) {
        SfStaskLeaderLaborRecord record = requireRecord(laborRecordId);
        requireEditableLaborRecord(record);
        if (!Objects.equals(record.getVersion(), bo.getVersion())) throw messages.stableException(StaskErrorCodes.LABOR_RECORD_VERSION_CONFLICT,
            StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        record.setLaborCount(bo.getDailyLaborCount()); record.setLastSource("MANUAL_EDIT"); record.setUpdateTime(java.time.LocalDateTime.now());
        if (laborRecordMapper.updateById(record) <= 0) throw messages.stableException(StaskErrorCodes.LABOR_RECORD_VERSION_CONFLICT,
            StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        return toVo(record, true);
    }

    private SfStaskLeaderLaborRecord requireRecord(Long laborRecordId) { SfStaskLeaderLaborRecord record = laborRecordMapper.selectById(laborRecordId); if (record == null || !Objects.equals(record.getTenantId(), tenantId())) throw messages.exception(StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND); return record; }
    private SfStaskLaborRecordVo toVo(SfStaskLeaderLaborRecord record, boolean own) {
        SfStaskLaborRecordVo vo = new SfStaskLaborRecordVo();
        vo.setLaborRecordId(record.getLaborRecordId());
        vo.setLeaderEmployeeId(record.getLeaderEmployeeId());
        vo.setLeaderEmployeeName(record.getLeaderNameSnapshot());
        vo.setPlanDate(record.getPlanDate());
        vo.setDailyLaborCount(record.getLaborCount());
        vo.setVersion(record.getVersion());
        vo.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(record.getUpdateTime()));
        boolean hasUnfinishedTask = own && hasUnfinishedTask(record);
        vo.setEditable(hasUnfinishedTask);
        if (!hasUnfinishedTask) {
            vo.setReadonlyReason(readonlyReason(own));
        }
        vo.setTaskCount((long) tasksForLeader(record.getLeaderEmployeeId(), record.getPlanDate()).size());
        return vo;
    }
    private List<SfStaskBatchTaskVo> tasksForLeader(Long leaderId, LocalDate date) { Set<String> statuses = Set.of(StaskOrderStatus.PENDING_LEADER_ASSIGN, StaskOrderStatus.ASSIGN_COMPLETE, StaskOrderStatus.LEADER_ARRIVED, StaskOrderStatus.PENDING_ACCEPTANCE, StaskOrderStatus.ACCEPTANCE_PASSED, StaskOrderStatus.ACCEPTANCE_REJECTED); return workOrderMapper.selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery().eq(SfStaskWorkOrder::getTenantId, tenantId()).eq(SfStaskWorkOrder::getLeaderId, leaderId).ge(SfStaskWorkOrder::getPlanDate, planStart(date)).lt(SfStaskWorkOrder::getPlanDate, planEnd(date)).in(SfStaskWorkOrder::getStatus, statuses)).stream().map(this::toBatchTask).toList(); }
    private SfStaskBatchTaskVo toBatchTask(SfStaskWorkOrder order) { SfStaskBatchTaskVo vo = BeanUtil.copyProperties(order, SfStaskBatchTaskVo.class); vo.setPlanDate(orderPlanDate(order)); vo.setGreenhouseName(order.getGreenhouseNameSnapshot()); vo.setWorkItemName(order.getWorkItemNameSnapshot()); vo.setStatusLabel(SfStaskWorkOrderAssembler.splitStatusLabel(order.getStatus(), messages)); return vo; }
    private boolean hasUnfinishedTask(SfStaskLeaderLaborRecord record) { return workOrderMapper.selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery().eq(SfStaskWorkOrder::getTenantId, tenantId()).eq(SfStaskWorkOrder::getLeaderId, record.getLeaderEmployeeId()).ge(SfStaskWorkOrder::getPlanDate, planStart(record.getPlanDate())).lt(SfStaskWorkOrder::getPlanDate, planEnd(record.getPlanDate())).in(SfStaskWorkOrder::getStatus, StaskOrderStatus.PENDING_LEADER_ASSIGN, StaskOrderStatus.ASSIGN_COMPLETE, StaskOrderStatus.LEADER_ARRIVED, StaskOrderStatus.PENDING_ACCEPTANCE, StaskOrderStatus.ACCEPTANCE_REJECTED)) > 0; }
    private void requireEditableLaborRecord(SfStaskLeaderLaborRecord record) {
        if (!isGroupLeader() || !Objects.equals(record.getLeaderEmployeeId(), LoginHelper.getUserId())) {
            forbidden();
        }
        if (!hasUnfinishedTask(record)) {
            throw messages.stableException(StaskErrorCodes.LABOR_RECORD_FORBIDDEN,
                StaskMessageKeys.LABEL_LABOR_RECORD_TASKS_COMPLETED_READ_ONLY);
        }
    }

    private String readonlyReason(boolean own) {
        return messages.message(own
            ? StaskMessageKeys.LABEL_LABOR_RECORD_TASKS_COMPLETED_READ_ONLY
            : StaskMessageKeys.ERROR_EXECUTION_LEADER_ONLY);
    }
    private boolean isGroupLeader() { return employeeAccessor.hasAppRole(LoginHelper.getUserId(), EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER); }
    private void requireReadonlyLaborRole() { Long id = LoginHelper.getUserId(); if (!(employeeAccessor.hasAppRole(id, EmployeeConstants.APP_ROLE_STASK_EXPERT) || employeeAccessor.hasAppRole(id, EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN) || employeeAccessor.hasAppRole(id, EmployeeConstants.APP_ROLE_STASK_LEADER))) forbidden(); }
    private void forbidden() { throw messages.stableException(StaskErrorCodes.LABOR_RECORD_FORBIDDEN, StaskMessageKeys.ERROR_EXECUTION_LEADER_ONLY); }
    private Date planStart(LocalDate date) { return Date.from(date.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant()); }
    private Date planEnd(LocalDate date) { return Date.from(date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant()); }
    private LocalDate orderPlanDate(SfStaskWorkOrder order) { return order.getPlanDate().toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate(); }
}
