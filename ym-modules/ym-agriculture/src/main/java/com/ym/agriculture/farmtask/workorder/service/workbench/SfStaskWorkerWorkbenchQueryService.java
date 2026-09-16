package com.ym.agriculture.farmtask.workorder.service.workbench;

import cn.hutool.core.collection.CollUtil;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskDispatchStatus;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerHomeVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerProfileVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkbenchVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.workorder.support.StaskGreenhousePlantingBatchHelper;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskWorkbenchEmployeeContextFactory;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskWorkerWorkbenchAssembler;
import com.ym.agriculture.farmtask.worker.service.ISfStaskWorkerSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工人首页、档案和旧版工作台查询服务。
 */
@Service
@RequiredArgsConstructor
public class SfStaskWorkerWorkbenchQueryService {

    private static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final SfStaskDispatchMapper dispatchMapper;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final ISfStaskWorkerSkillService workerSkillService;
    private final SfStaskWorkbenchEmployeeContextFactory employeeContextFactory;
    private final SfStaskWorkerWorkbenchAssembler assembler;
    private final StaskGreenhousePlantingBatchHelper plantingBatchHelper;

    /**
     * 查询工人首页任务。
     *
     * @param tenantId  当前租户
     * @param employeeId 当前工人
     * @return 工人首页
     */
    public SfStaskWorkerHomeVo home(String tenantId, Long employeeId) {
        List<SfStaskDispatch> dispatches = CollUtil.emptyIfNull(
            dispatchMapper.selectHomeByWorkerId(tenantId, employeeId));
        if (dispatches.isEmpty()) {
            return assembler.emptyHome();
        }
        Map<Long, SfStaskWorkOrder> orders = loadOrders(tenantId, dispatches);
        List<Long> pendingOrderIds = dispatches.stream()
            .filter(row -> StaskDispatchStatus.PENDING.equals(row.getStatus()))
            .map(SfStaskDispatch::getOrderId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Set<Long> reinvitedOrderIds = pendingOrderIds.isEmpty() ? Set.of() : new HashSet<>(
            CollUtil.emptyIfNull(dispatchMapper.selectOrderIdsWithPriorInvite(
                tenantId, employeeId, pendingOrderIds)));
        SfStaskEmployeeQueryContext employees = employeeContextFactory.create();
        employees.preload(orders.values().stream()
            .map(SfStaskWorkOrder::getLeaderId).filter(Objects::nonNull).distinct().toList());
        SfStaskWorkerHomeVo home = assembler.home(
            dispatches, orders, employees.employees(), reinvitedOrderIds);

        List<SfStaskWorkerTaskItemVo> allItems = new ArrayList<>();
        allItems.addAll(CollUtil.emptyIfNull(home.getPendingInviteTasks()));
        allItems.addAll(CollUtil.emptyIfNull(home.getAcceptedTasks()));
        plantingBatchHelper.enrichWorkerTaskItems(allItems);
        return home;
    }

    /**
     * 查询工人个人工作统计。
     *
     * @param tenantId  当前租户
     * @param employeeId 当前工人
     * @return 工人档案统计
     */
    public SfStaskWorkerProfileVo profile(String tenantId, Long employeeId) {
        SfStaskWorkerProfileVo profile = new SfStaskWorkerProfileVo();
        profile.setPendingInviteCount(0L);
        profile.setAcceptedCount(0L);
        profile.setCompletedCount(0L);
        profile.setSkills(workerSkillService.queryByEmployeeId(employeeId));

        List<SfStaskDispatch> dispatches = CollUtil.emptyIfNull(
            dispatchMapper.selectAllByWorkerId(tenantId, employeeId));
        if (dispatches.isEmpty()) {
            return profile;
        }
        // Mapper 已按邀请时间倒序，兼容规则要求同一工单保留第一条派工。
        Map<Long, SfStaskDispatch> dispatchByOrder = dispatches.stream()
            .filter(row -> row.getOrderId() != null)
            .collect(Collectors.toMap(SfStaskDispatch::getOrderId, Function.identity(),
                (first, ignored) -> first, HashMap::new));
        Map<Long, SfStaskWorkOrder> orders = CollUtil.emptyIfNull(
                workOrderMapper.selectByIds(tenantId, dispatchByOrder.keySet())).stream()
            .collect(Collectors.toMap(SfStaskWorkOrder::getOrderId, Function.identity(),
                (first, ignored) -> first, HashMap::new));

        long pending = 0;
        long accepted = 0;
        long completed = 0;
        for (Map.Entry<Long, SfStaskDispatch> entry : dispatchByOrder.entrySet()) {
            SfStaskWorkOrder order = orders.get(entry.getKey());
            if (order == null) {
                continue;
            }
            String status = SfStaskWorkOrderAssembler.workerTaskStatus(entry.getValue(), order);
            if (STATUS_PENDING_CONFIRM.equals(status)) {
                pending++;
            } else if (STATUS_ACCEPTED.equals(status)) {
                accepted++;
            } else if (STATUS_COMPLETED.equals(status)) {
                completed++;
            }
        }
        profile.setPendingInviteCount(pending);
        profile.setAcceptedCount(accepted);
        profile.setCompletedCount(completed);
        return profile;
    }

    /**
     * 查询工人旧版兼容工作台。
     *
     * @param tenantId  当前租户
     * @param employeeId 当前工人
     * @return 兼容工作台
     */
    public SfStaskWorkbenchVo workbench(String tenantId, Long employeeId) {
        return assembler.legacyWorkbench(home(tenantId, employeeId));
    }

    private Map<Long, SfStaskWorkOrder> loadOrders(String tenantId, List<SfStaskDispatch> dispatches) {
        List<Long> orderIds = dispatches.stream()
            .map(SfStaskDispatch::getOrderId).filter(Objects::nonNull).distinct().toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return CollUtil.emptyIfNull(workOrderMapper.selectByIds(tenantId, orderIds)).stream()
            .collect(Collectors.toMap(SfStaskWorkOrder::getOrderId, Function.identity(),
                (first, ignored) -> first, HashMap::new));
    }
}
