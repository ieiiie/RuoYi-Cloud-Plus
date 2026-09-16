package com.ym.agriculture.farmtask.workorder.service.packagecmd;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.core.exception.ServiceException;
import com.ym.agriculture.farmtask.assignment.model.entity.SfFarmWorkAssignment;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskFlowLogMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskLeaderAssignMode;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import com.ym.agriculture.farmtask.workorder.support.SfStaskOrderNoGenerator;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 任务包拆单器。
 *
 * <p>按农事项和最终组长将任务包批量拆成单棚工单，并批量写入工单、绑定关系与流转日志。</p>
 */
@RequiredArgsConstructor
@Component
public class SfStaskPackageSplitter {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfStaskFlowLogMapper flowLogMapper;
    private final SfStaskLeaderAssignmentResolver assignmentResolver;
    private final SfStaskStateFlowSupport stateFlow;
    private final SfStaskPackageCommandGuard guard;
    private final SfStaskOrderNoGenerator orderNoGenerator;

    /**
     * 将任务包拆分成单棚工单。
     *
     * @param header          任务包头
     * @param items           任务包农事项
     * @param overallTechNote 整体技术说明
     * @param now             统一创建时间
     * @return 新增工单数量
     */
    public int splitPackage(SfStaskTaskPackage header, List<SfStaskWorkOrderItem> items,
        String overallTechNote, Date now) {
        List<SfStaskWorkOrderGreenhouse> greenhouses = greenhouseMapper.selectByPackageId(
            header.getTenantId(), header.getPackageId());
        if (CollUtil.isEmpty(items) || CollUtil.isEmpty(greenhouses)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_DETAIL_INCOMPLETE);
        }
        SplitScope scope = buildSplitScope(header, items, greenhouses);
        SplitRows rows = buildSplitRows(header, items, overallTechNote, now, scope);
        if (CollUtil.isEmpty(rows.orders())) {
            return 0;
        }
        persistSplitRows(rows);
        return rows.orders().size();
    }

    /**
     * 按任务项的最终组长对大棚进行分组。
     *
     * @param item              任务包农事项
     * @param scopedGreenhouses 当前农事项的大棚关系
     * @param assignmentIndex   自动分工索引
     * @param leaderMap         组长信息索引
     * @return 以组长ID为键的大棚分组
     */
    public Map<Long, List<SfStaskWorkOrderGreenhouse>> groupGreenhousesByEffectiveLeader(
        SfStaskWorkOrderItem item, List<SfStaskWorkOrderGreenhouse> scopedGreenhouses,
        Map<String, SfFarmWorkAssignment> assignmentIndex, Map<Long, SysEmployeeVo> leaderMap) {
        if (CollUtil.isEmpty(scopedGreenhouses)) {
            return Map.of();
        }
        LinkedHashMap<Long, List<SfStaskWorkOrderGreenhouse>> result = new LinkedHashMap<>();
        if (item.getLeaderIdSnapshot() != null) {
            result.put(item.getLeaderIdSnapshot(), new ArrayList<>(scopedGreenhouses));
            return result;
        }
        String mode = StaskLeaderAssignMode.normalize(item.getLeaderAssignMode());
        if (StaskLeaderAssignMode.MANUAL.equals(mode)) {
            SysEmployeeVo leader = leaderMap.get(item.getManualLeaderId());
            if (!SfStaskLeaderAssignmentResolver.isValidLeader(leader)) {
                throw new ServiceException(item.getManualLeaderId() == null
                    ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_ASSIGNMENT_MANUAL_LEADER_REQUIRED) : messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_ASSIGNMENT_MANUAL_LEADER_UNAVAILABLE));
            }
            result.put(leader.getEmployeeId(), new ArrayList<>(scopedGreenhouses));
            return result;
        }
        for (SfStaskWorkOrderGreenhouse greenhouse : scopedGreenhouses) {
            SfFarmWorkAssignment assignment = assignmentIndex.get(
                SfStaskLeaderAssignmentResolver.assignmentIndexKey(
                    greenhouse.getGreenhouseId(), item.getWorkItemId()));
            Long leaderId = assignment == null ? null : assignment.getLeaderId();
            SysEmployeeVo leader = leaderMap.get(leaderId);
            if (!SfStaskLeaderAssignmentResolver.isValidLeader(leader)) {
                throw messages.exception(
                    com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_GREENHOUSE_LEADER_UNASSIGNED,
                    item.getWorkItemNameSnapshot());
            }
            result.computeIfAbsent(leaderId, key -> new ArrayList<>()).add(greenhouse);
        }
        return result;
    }

    private SplitScope buildSplitScope(SfStaskTaskPackage header, List<SfStaskWorkOrderItem> items,
        List<SfStaskWorkOrderGreenhouse> greenhouses) {
        Map<Long, List<SfStaskWorkOrderGreenhouse>> greenhousesByItem = greenhouses.stream()
            .filter(row -> row.getItemId() != null)
            .collect(Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getItemId));
        List<SfStaskWorkOrderGreenhouse> legacyGreenhouses = greenhouses.stream()
            .filter(row -> row.getItemId() == null)
            .toList();
        Map<String, SfFarmWorkAssignment> assignmentIndex = assignmentResolver.loadAssignmentIndex(
            header.getTenantId(), items, greenhouses);
        List<Long> leaderIds = Stream.concat(
                assignmentIndex.values().stream().map(SfFarmWorkAssignment::getLeaderId),
                items.stream().flatMap(item -> Stream.of(
                    item.getLeaderIdSnapshot(), item.getManualLeaderId())))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, SysEmployeeVo> leaderMap = assignmentResolver.loadEmployeeMap(leaderIds);
        return new SplitScope(greenhousesByItem, legacyGreenhouses, assignmentIndex, leaderMap);
    }

    private SplitRows buildSplitRows(SfStaskTaskPackage header, List<SfStaskWorkOrderItem> items,
        String overallTechNote, Date now, SplitScope scope) {
        List<SfStaskWorkOrder> orders = new ArrayList<>();
        List<SfStaskWorkOrderGreenhouse> bindings = new ArrayList<>();
        List<SfStaskFlowLog> logs = new ArrayList<>();
        String operatorRoleCode = guard.currentRoleCode();
        String event = StaskCreatorRole.EXPERT.equals(header.getCreatorRoleCode())
            ? StaskOrderEvent.SUBMIT_BY_TECHNICIAN : StaskOrderEvent.TECH_CONFIRM;
        boolean legacyGreenhousesConsumed = false;
        for (SfStaskWorkOrderItem item : items) {
            List<SfStaskWorkOrderGreenhouse> scopedGreenhouses = scope.greenhousesByItem().get(item.getItemId());
            if (CollUtil.isEmpty(scopedGreenhouses)) {
                scopedGreenhouses = legacyGreenhousesConsumed ? List.of() : scope.legacyGreenhouses();
                legacyGreenhousesConsumed = true;
            }
            appendItemRows(header, item, scopedGreenhouses, overallTechNote, now,
                operatorRoleCode, event, scope, orders, bindings, logs);
        }
        return new SplitRows(orders, bindings, logs);
    }

    private void appendItemRows(SfStaskTaskPackage header, SfStaskWorkOrderItem item,
        List<SfStaskWorkOrderGreenhouse> scopedGreenhouses, String overallTechNote, Date now,
        String operatorRoleCode, String event, SplitScope scope, List<SfStaskWorkOrder> orders,
        List<SfStaskWorkOrderGreenhouse> bindings, List<SfStaskFlowLog> logs) {
        Map<Long, List<SfStaskWorkOrderGreenhouse>> greenhousesByLeader = groupGreenhousesByEffectiveLeader(
            item, scopedGreenhouses, scope.assignmentIndex(), scope.leaderMap());
        for (Map.Entry<Long, List<SfStaskWorkOrderGreenhouse>> entry : greenhousesByLeader.entrySet()) {
            appendLeaderRows(header, item, entry.getKey(), entry.getValue(), overallTechNote,
                now, operatorRoleCode, event, orders, bindings, logs);
        }
    }

    private void appendLeaderRows(SfStaskTaskPackage header, SfStaskWorkOrderItem item, Long leaderId,
        List<SfStaskWorkOrderGreenhouse> greenhouses, String overallTechNote, Date now,
        String operatorRoleCode, String event, List<SfStaskWorkOrder> orders,
        List<SfStaskWorkOrderGreenhouse> bindings, List<SfStaskFlowLog> logs) {
        for (SfStaskWorkOrderGreenhouse greenhouse : greenhouses) {
            SfStaskWorkOrder order = buildSplitOrder(
                header, item, greenhouse, leaderId, overallTechNote, now);
            orders.add(order);
            bindings.add(buildBinding(header.getTenantId(), greenhouse, order));
            logs.add(stateFlow.buildFlowLog(order, null, StaskOrderStatus.PENDING_LEADER_ACCEPT,
                event, messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_SPLIT_ORDER), operatorRoleCode));
        }
    }

    private void persistSplitRows(SplitRows rows) {
        if (!workOrderMapper.insertBatch(rows.orders())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_SPLIT_ORDER_SAVE_FAILED);
        }
        if (!greenhouseMapper.updateBatchById(rows.bindings())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_SPLIT_GREENHOUSE_BIND_FAILED);
        }
        if (!flowLogMapper.insertBatch(rows.logs())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_SPLIT_FLOW_LOG_SAVE_FAILED);
        }
    }

    private SfStaskWorkOrder buildSplitOrder(SfStaskTaskPackage header, SfStaskWorkOrderItem item,
        SfStaskWorkOrderGreenhouse greenhouse, Long leaderId, String overallTechNote, Date now) {
        SfStaskWorkOrder order = new SfStaskWorkOrder();
        order.setOrderId(IdWorker.getId());
        order.setOrderNo(orderNoGenerator.next(now));
        order.setPackageId(header.getPackageId());
        order.setTenantId(header.getTenantId());
        order.setCreatorEmployeeId(header.getCreatorEmployeeId());
        order.setCreatorRoleCode(header.getCreatorRoleCode());
        order.setHandlerTechnicianEmployeeId(header.getHandlerTechnicianEmployeeId());
        order.setHandlerTechnicianEmployeeNameSnapshot(header.getHandlerTechnicianEmployeeNameSnapshot());
        order.setPlanDate(header.getPlanDate());
        order.setGreenhouseId(greenhouse.getGreenhouseId());
        order.setGreenhouseCodeSnapshot(greenhouse.getGreenhouseCodeSnapshot());
        order.setGreenhouseNameSnapshot(greenhouse.getGreenhouseNameSnapshot());
        order.setWorkItemId(item.getWorkItemId());
        order.setWorkItemNameSnapshot(item.getWorkItemNameSnapshot());
        order.setWorkItemCodeSnapshot(item.getWorkItemCodeSnapshot());
        order.setCategoryIdSnapshot(item.getCategoryIdSnapshot());
        order.setCategoryNameSnapshot(item.getCategoryNameSnapshot());
        order.setManagerRequirement(item.getManagerRequirement());
        order.setManagerPhotos(item.getManagerPhotos());
        order.setTechInstruction(item.getTechInstruction());
        order.setTechPhotos(item.getTechPhotos());
        order.setOverallTechNote(overallTechNote);
        order.setLeaderId(leaderId);
        order.setStatus(StaskOrderStatus.PENDING_LEADER_ACCEPT);
        order.setAcceptedWorkerCount(0);
        order.setVersion(0);
        order.setCreateBy(header.getCreatorEmployeeId());
        order.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        order.setUpdateBy(header.getCreatorEmployeeId());
        order.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        return order;
    }

    private static SfStaskWorkOrderGreenhouse buildBinding(String tenantId,
        SfStaskWorkOrderGreenhouse greenhouse, SfStaskWorkOrder order) {
        SfStaskWorkOrderGreenhouse binding = new SfStaskWorkOrderGreenhouse();
        binding.setGreenhouseItemId(greenhouse.getGreenhouseItemId());
        binding.setTenantId(tenantId);
        binding.setOrderId(order.getOrderId());
        return binding;
    }

    private record SplitScope(Map<Long, List<SfStaskWorkOrderGreenhouse>> greenhousesByItem,
        List<SfStaskWorkOrderGreenhouse> legacyGreenhouses,
        Map<String, SfFarmWorkAssignment> assignmentIndex,
        Map<Long, SysEmployeeVo> leaderMap) {
    }

    private record SplitRows(List<SfStaskWorkOrder> orders,
        List<SfStaskWorkOrderGreenhouse> bindings,
        List<SfStaskFlowLog> logs) {
    }
}
