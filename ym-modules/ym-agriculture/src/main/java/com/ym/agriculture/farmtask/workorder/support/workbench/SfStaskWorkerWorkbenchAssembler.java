package com.ym.agriculture.farmtask.workorder.support.workbench;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskDispatchStatus;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerHomeVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkbenchVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 纯内存装配工人首页与兼容工作台，不访问数据库或外部服务。
 */
@Component
@lombok.RequiredArgsConstructor
public class SfStaskWorkerWorkbenchAssembler {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;

    private static final String ACTION_ACCEPT = "ACCEPT";
    private static final String ACTION_VIEW_INSTRUCTION = "VIEW_INSTRUCTION";

    private static final Set<String> PENDING_EXCLUDED_STATUSES = Set.of(
        StaskOrderStatus.VOIDED,
        StaskOrderStatus.CANCELLED
    );

    private static final Set<String> ACCEPTED_EXCLUDED_STATUSES = Set.of(
        StaskOrderStatus.ACCEPTANCE_PASSED,
        StaskOrderStatus.ACCEPTANCE_REJECTED,
        StaskOrderStatus.VOIDED,
        StaskOrderStatus.CANCELLED
    );

    /**
     * 装配工人首页邀请和已接任务。
     */
    public SfStaskWorkerHomeVo home(List<SfStaskDispatch> dispatches, Map<Long, SfStaskWorkOrder> orderIndex,
        Map<Long, SysEmployeeVo> employeeIndex, Set<Long> reinvitedOrderIds) {
        if (CollUtil.isEmpty(dispatches)) {
            return emptyHome();
        }
        List<SfStaskDispatch> pending = filterDispatches(
            dispatches, orderIndex, StaskDispatchStatus.PENDING, PENDING_EXCLUDED_STATUSES);
        SfStaskWorkOrderAssembler.sortWorkerHomeDispatches(pending, orderIndex);
        List<SfStaskWorkerTaskItemVo> pendingItems = pending.stream()
            .map(dispatch -> SfStaskWorkOrderAssembler.toWorkerTaskItem(
                dispatch, orderIndex.get(dispatch.getOrderId()), employeeIndex,
                reinvitedOrderIds.contains(dispatch.getOrderId()), ACTION_ACCEPT, messages))
            .toList();

        List<SfStaskDispatch> accepted = filterDispatches(
            dispatches, orderIndex, StaskDispatchStatus.ACCEPTED, ACCEPTED_EXCLUDED_STATUSES);
        SfStaskWorkOrderAssembler.sortWorkerHomeDispatches(accepted, orderIndex);
        List<SfStaskWorkerTaskItemVo> acceptedItems = accepted.stream()
            .map(dispatch -> SfStaskWorkOrderAssembler.toWorkerTaskItem(
                dispatch, orderIndex.get(dispatch.getOrderId()), employeeIndex,
                false, ACTION_VIEW_INSTRUCTION, messages))
            .toList();

        SfStaskWorkerHomeVo home = new SfStaskWorkerHomeVo();
        home.setPendingInviteCount((long) pendingItems.size());
        home.setAcceptedCount((long) acceptedItems.size());
        home.setPendingReviewCount(home.getPendingInviteCount());
        home.setProcessingCount(home.getAcceptedCount());
        home.setPendingInviteTasks(pendingItems);
        home.setAcceptedTasks(acceptedItems);
        return home;
    }

    /**
     * 将工人首页转换为旧版统一工作台视图。
     */
    public SfStaskWorkbenchVo legacyWorkbench(SfStaskWorkerHomeVo home) {
        SfStaskWorkbenchVo vo = new SfStaskWorkbenchVo();
        vo.setPendingReviewCount(home.getPendingReviewCount());
        vo.setProcessingCount(home.getProcessingCount());
        vo.setPendingAcceptanceCount(0L);
        vo.setRows(Stream.concat(
                CollUtil.emptyIfNull(home.getPendingInviteTasks()).stream(),
                CollUtil.emptyIfNull(home.getAcceptedTasks()).stream())
            .map(this::legacyOrder)
            .toList());
        return vo;
    }

    /**
     * 返回字段完整初始化的空工人首页。
     */
    public SfStaskWorkerHomeVo emptyHome() {
        SfStaskWorkerHomeVo home = new SfStaskWorkerHomeVo();
        home.setPendingInviteCount(0L);
        home.setAcceptedCount(0L);
        home.setPendingReviewCount(0L);
        home.setProcessingCount(0L);
        home.setPendingInviteTasks(List.of());
        home.setAcceptedTasks(List.of());
        return home;
    }

    private static List<SfStaskDispatch> filterDispatches(List<SfStaskDispatch> dispatches,
        Map<Long, SfStaskWorkOrder> orderIndex, String dispatchStatus, Set<String> excludedOrderStatuses) {
        List<SfStaskDispatch> result = new ArrayList<>();
        for (SfStaskDispatch dispatch : dispatches) {
            if (!dispatchStatus.equals(dispatch.getStatus())) {
                continue;
            }
            SfStaskWorkOrder order = orderIndex.get(dispatch.getOrderId());
            if (order != null && !excludedOrderStatuses.contains(order.getStatus())) {
                result.add(dispatch);
            }
        }
        return result;
    }

    private SfStaskWorkOrderVo legacyOrder(SfStaskWorkerTaskItemVo item) {
        SfStaskWorkOrderVo vo = new SfStaskWorkOrderVo();
        vo.setOrderId(item.getOrderId());
        vo.setDispatchId(item.getDispatchId());
        vo.setDispatchStatus(item.getDispatchStatus());
        vo.setPlanDate(item.getPlanDate());
        vo.setLeaderId(item.getLeaderId());
        vo.setLeaderName(item.getLeaderName());
        if (StringUtils.isNotBlank(item.getTitle())) {
            String[] parts = item.getTitle().split(" · ", 2);
            vo.setGreenhouseNameSnapshot(parts[0]);
            if (parts.length > 1) {
                vo.setWorkItemNameSnapshot(parts[1]);
            }
        }
        vo.setPrimaryAction(item.getPrimaryAction());
        return vo;
    }
}
