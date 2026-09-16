package com.ym.agriculture.farmtask.workorder.service.query;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.voice.model.vo.SfStaskVoiceBroadcastVo;
import com.ym.agriculture.farmtask.voice.service.ISfStaskVoiceBroadcastService;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAcceptanceMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkerAllTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkerHistoryQueryBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerHistoryItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderDetailVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.workorder.support.StaskGreenhousePlantingBatchHelper;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * stask 工人全部任务、本人详情与历史任务查询。
 */
@RequiredArgsConstructor
@Service
public class SfStaskWorkerTaskQueryService {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<String> WORKER_TASK_STATUSES = List.of(
        "PENDING_CONFIRM", "ACCEPTED", "REJECTED", "COMPLETED", "NOT_PASSED", "VOIDED");

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskDispatchMapper dispatchMapper;
    private final SfStaskAcceptanceMapper acceptanceMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final StaskGreenhousePlantingBatchHelper plantingBatchHelper;
    private final SfStaskOrderDetailBuilder detailBuilder;

    @Autowired(required = false)
    private ISfStaskVoiceBroadcastService voiceBroadcastService;

    /**
     * 分页查询工人全部任务。
     */
    public PageResult<SfStaskWorkerAllTaskItemVo> workerAllTasks(
        SfStaskWorkerAllTaskQueryBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        Long employeeId = LoginHelper.getUserId();
        List<SfStaskDispatch> dispatches = dispatchMapper.selectAllByWorkerId(tenantId, employeeId);
        if (CollUtil.isEmpty(dispatches)) {
            return com.ym.agriculture.shared.common.AgriculturePageResults.build(List.of(), pageQuery.build());
        }
        Map<Long, SfStaskDispatch> dispatchByOrder = dispatches.stream()
            .filter(row -> row.getOrderId() != null)
            .collect(Collectors.toMap(SfStaskDispatch::getOrderId, Function.identity(), (a, b) -> a));
        Map<Long, SfStaskWorkOrder> orderMap = workOrderMapper.selectByIds(tenantId, dispatchByOrder.keySet()).stream()
            .filter(row -> matchWorkerFilters(row, bo))
            .collect(Collectors.toMap(SfStaskWorkOrder::getOrderId, Function.identity(), (a, b) -> a));
        SfStaskEmployeeQueryContext employees = employeeContext();
        employees.preload(orderMap.values().stream().map(SfStaskWorkOrder::getLeaderId)
            .filter(Objects::nonNull).distinct().toList());
        Map<Long, SfStaskAcceptance> acceptances = SfStaskWorkOrderAssembler.indexLatestAcceptances(
            acceptanceMapper.selectByOrderIds(tenantId, orderMap.keySet()));
        Set<Long> reinvited = new HashSet<>(
            dispatchMapper.selectOrderIdsWithPriorInvite(tenantId, employeeId, orderMap.keySet()));
        List<String> statuses = normalizeWorkerStatuses(bo);
        List<SfStaskWorkerAllTaskItemVo> items = dispatchByOrder.entrySet().stream()
            .filter(entry -> orderMap.containsKey(entry.getKey()))
            .map(entry -> SfStaskWorkOrderAssembler.toWorkerAllTaskItem(entry.getValue(),
                orderMap.get(entry.getKey()), acceptances.get(entry.getKey()), employees.employees(),
                reinvited.contains(entry.getKey()), messages))
            .filter(Objects::nonNull)
            .filter(item -> statuses.isEmpty() || statuses.contains(item.getStatus()))
            .sorted(Comparator.comparing(SfStaskWorkerAllTaskItemVo::getPlanDate,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
        plantingBatchHelper.enrichWorkerAllTaskItems(items);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(items, pageQuery.build());
    }

    /**
     * 查询工人本人的派工详情。
     */
    public SfStaskWorkOrderDetailVo workerDetail(Long dispatchId) {
        SfStaskDispatch dispatch = dispatchMapper.selectById(dispatchId);
        if (dispatch == null || !Objects.equals(dispatch.getWorkerId(), LoginHelper.getUserId())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_TASK_NOT_FOUND_OR_FORBIDDEN);
        }
        SfStaskEmployeeQueryContext employees = employeeContext();
        SfStaskWorkOrderDetailVo vo = detailBuilder.buildOrderDetail(requireOrder(dispatch.getOrderId()), employees);
        vo.setStatus(null);
        vo.setRequiredWorkerCount(null);
        vo.setAcceptedWorkerCount(null);
        vo.setDispatches(null);
        vo.setWorkWorkers(null);
        vo.setFlowLogs(null);
        vo.setEditable(false);
        vo.setCanSaveDraft(false);
        vo.setCanSubmit(false);
        vo.setCanDelete(false);
        vo.setReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_SELF_TASK_ONLY));
        vo.setVoiceBroadcast(latestVoiceBroadcast(vo.getOrderId()));
        plantingBatchHelper.enrichWorkOrderDetailVo(vo);
        return vo;
    }

    /**
     * 分页查询工人验收通过的历史任务。
     */
    public PageResult<SfStaskWorkerHistoryItemVo> workerHistory(
        SfStaskWorkerHistoryQueryBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        List<SfStaskDispatch> dispatches = dispatchMapper.selectAcceptedByWorkerId(
            tenantId, LoginHelper.getUserId());
        if (CollUtil.isEmpty(dispatches)) {
            return com.ym.agriculture.shared.common.AgriculturePageResults.build(List.of(), pageQuery.build());
        }
        Map<Long, SfStaskDispatch> dispatchByOrder = dispatches.stream()
            .collect(Collectors.toMap(SfStaskDispatch::getOrderId, Function.identity(), (a, b) -> a));
        List<SfStaskWorkOrder> orders = workOrderMapper.selectByIds(tenantId, dispatchByOrder.keySet()).stream()
            .filter(row -> StaskOrderStatus.ACCEPTANCE_PASSED.equals(row.getStatus()))
            .filter(row -> matchHistoryFilters(row, bo))
            .toList();
        List<Long> orderIds = orders.stream().map(SfStaskWorkOrder::getOrderId).toList();
        Map<Long, SfStaskAcceptance> acceptances = SfStaskWorkOrderAssembler.indexLatestAcceptances(
            acceptanceMapper.selectByOrderIds(tenantId, orderIds));
        SfStaskEmployeeQueryContext employees = employeeContext();
        employees.preload(orders.stream().map(SfStaskWorkOrder::getLeaderId)
            .filter(Objects::nonNull).distinct().toList());
        List<SfStaskWorkerHistoryItemVo> items = orders.stream()
            .sorted(Comparator.comparing(row -> {
                SfStaskAcceptance acceptance = acceptances.get(row.getOrderId());
                return acceptance == null ? null : acceptance.getAcceptedAt();
            }, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(order -> SfStaskWorkOrderAssembler.toWorkerHistoryItem(dispatchByOrder.get(order.getOrderId()),
                order, acceptances.get(order.getOrderId()), employees.employees(), messages))
            .filter(Objects::nonNull)
            .toList();
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(items, pageQuery.build());
    }

    private SfStaskWorkOrder requireOrder(Long orderId) {
        SfStaskWorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND);
        }
        ensureTenant(order.getTenantId(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_NOT_FOUND));
        return order;
    }

    private static void ensureTenant(String entityTenantId, String message) {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isNotBlank(tenantId) && !Objects.equals(entityTenantId, tenantId)) {
            throw new ServiceException(message);
        }
    }

    private static boolean matchHistoryFilters(SfStaskWorkOrder order, SfStaskWorkerHistoryQueryBo bo) {
        if (bo == null) {
            return true;
        }
        if (bo.getWorkItemId() != null && !Objects.equals(bo.getWorkItemId(), order.getWorkItemId())) {
            return false;
        }
        return matchDate(order.getPlanDate(), bo.getPlanStartDate(), bo.getPlanEndDate());
    }

    private static boolean matchWorkerFilters(SfStaskWorkOrder order, SfStaskWorkerAllTaskQueryBo bo) {
        if (order == null) {
            return false;
        }
        if (bo == null) {
            return true;
        }
        if (CollUtil.isNotEmpty(bo.getGreenhouseIds())
            && !bo.getGreenhouseIds().contains(order.getGreenhouseId())) {
            return false;
        }
        if (CollUtil.isNotEmpty(bo.getWorkItemIds())
            && !bo.getWorkItemIds().contains(order.getWorkItemId())) {
            return false;
        }
        return matchDate(order.getPlanDate(), bo.getPlanStartDate(), bo.getPlanEndDate());
    }

    private static boolean matchDate(Date planDateValue, Date startValue, Date endValue) {
        LocalDate planDate = toLocalDate(planDateValue);
        if (planDate == null) {
            return startValue == null && endValue == null;
        }
        LocalDate start = toLocalDate(startValue);
        LocalDate end = toLocalDate(endValue);
        return (start == null || !planDate.isBefore(start)) && (end == null || !planDate.isAfter(end));
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(CHINA_ZONE).toLocalDate();
    }

    private List<String> normalizeWorkerStatuses(SfStaskWorkerAllTaskQueryBo bo) {
        if (bo == null || CollUtil.isEmpty(bo.getStatuses())) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String status : bo.getStatuses()) {
            if (StringUtils.isBlank(status)) {
                continue;
            }
            String normalized = status.trim().toUpperCase();
            if (!WORKER_TASK_STATUSES.contains(normalized)) {
                throw messages.exception(
                    com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_INVALID_WORKER_STATUS_FILTER, status);
            }
            result.add(normalized);
        }
        return new java.util.ArrayList<>(result);
    }

    private SfStaskEmployeeQueryContext employeeContext() {
        return new SfStaskEmployeeQueryContext(ids -> {
            List<SysEmployeeVo> rows = employeeAccessor.queryBasicByIds(ids);
            return rows != null ? rows : employeeAccessor.queryByIds(ids);
        });
    }

    private SfStaskVoiceBroadcastVo latestVoiceBroadcast(Long orderId) {
        return voiceBroadcastService == null ? null : voiceBroadcastService.queryLatestForOrder(orderId);
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }
}
