package com.ym.agriculture.farmtask.workorder.support.workbench;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskAcceptanceResult;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskClockRecord;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.leaderlabor.model.entity.SfStaskLeaderLaborRecord;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskGreenhouseBriefVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 纯内存装配拆分工单的状态、按钮和事件时间，不访问 Mapper 或外部服务。
 */
@Component
@lombok.RequiredArgsConstructor
public class SfStaskSplitWorkbenchAssembler {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;

    private static final String CARD_TYPE_SPLIT = "SPLIT";
    private static final String ACTION_ACCEPT = "ACCEPT";
    private static final String ACTION_LEADER_ACCEPT = "LEADER_ACCEPT";
    private static final String ACTION_DISPATCH = "DISPATCH";
    private static final String ACTION_CLOCK_IN = "CLOCK_IN";
    private static final String ACTION_APPLY_COMPLETE = "APPLY_COMPLETE";
    private static final String ACTION_REAPPLY_ACCEPTANCE = "REAPPLY_ACCEPTANCE";

    /**
     * 装配管理员工作台拆分工单。
     */
    public List<SfStaskWorkOrderVo> managerOrders(List<SfStaskWorkOrder> orders,
        Map<Long, SysEmployeeVo> employeeIndex, SfStaskSplitWorkbenchBatchReader.SplitData data) {
        if (CollUtil.isEmpty(orders)) {
            return List.of();
        }
        List<SfStaskWorkOrderVo> result = new ArrayList<>(orders.size());
        for (SfStaskWorkOrder order : orders) {
            SfStaskWorkOrderVo vo = SfStaskWorkOrderAssembler.toWorkOrderVo(order, employeeIndex, Map.of());
            applyLabor(order, vo, data);
            fillManagerFields(order, vo, data);
            result.add(vo);
        }
        return result;
    }

    /**
     * 为已完成工单补充最新验收结果，并按验收时间倒序排列。
     */
    public void applyCompletedAcceptance(List<SfStaskWorkOrderVo> orders,
        SfStaskSplitWorkbenchBatchReader.SplitData data) {
        if (CollUtil.isEmpty(orders)) {
            return;
        }
        for (SfStaskWorkOrderVo vo : CollUtil.emptyIfNull(orders)) {
            SfStaskAcceptance acceptance = data.acceptances().get(vo.getOrderId());
            if (acceptance == null) {
                continue;
            }
            vo.setAcceptanceResult(acceptance.getResult());
            vo.setAcceptedAt(acceptance.getAcceptedAt());
            vo.setEventTime(acceptance.getAcceptedAt());
            vo.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_TIME));
            if (StaskAcceptanceResult.PASS.equals(acceptance.getResult())) {
                vo.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_PASSED));
            } else if (StaskAcceptanceResult.REJECT.equals(acceptance.getResult())) {
                vo.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_REJECTED));
            }
        }
        orders.sort(Comparator.comparing(SfStaskWorkOrderVo::getEventTime,
            Comparator.nullsLast(Comparator.reverseOrder())));
    }

    /**
     * 装配技术员工作台拆分工单。
     */
    public List<SfStaskTechnicianWorkbenchItemVo> technicianItems(List<SfStaskWorkOrder> orders,
        Map<Long, SysEmployeeVo> employeeIndex, SfStaskSplitWorkbenchBatchReader.SplitData data,
        boolean completed) {
        return technicianItems(orders, employeeIndex, data, completed, null);
    }

    /**
     * 装配技术员工作台拆分工单，并按当前登录技术员限制验收按钮。
     *
     * @param currentEmployeeId 当前登录技术员；为空时不展示验收操作
     */
    public List<SfStaskTechnicianWorkbenchItemVo> technicianItems(List<SfStaskWorkOrder> orders,
        Map<Long, SysEmployeeVo> employeeIndex, SfStaskSplitWorkbenchBatchReader.SplitData data,
        boolean completed, Long currentEmployeeId) {
        List<SfStaskWorkOrderVo> vos = managerOrders(orders, employeeIndex, data);
        if (completed) {
            for (SfStaskWorkOrderVo vo : vos) {
                SfStaskCompletion completion = data.completions().get(vo.getOrderId());
                if (completion != null) {
                    vo.setCompletedAt(completion.getCompletedAt());
                }
            }
            applyCompletedAcceptance(vos, data);
            // 兼容技术员已完成列表：展示完成时间，验收结果仍用于状态文案。
            for (SfStaskWorkOrderVo vo : vos) {
                if (vo.getCompletedAt() != null) {
                    vo.setEventTime(vo.getCompletedAt());
                    vo.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_COMPLETION_TIME));
                }
            }
        }
        List<SfStaskTechnicianWorkbenchItemVo> items = new ArrayList<>(vos.size());
        for (SfStaskWorkOrderVo vo : vos) {
            SfStaskTechnicianWorkbenchItemVo item = new SfStaskTechnicianWorkbenchItemVo();
            BeanUtil.copyProperties(vo, item);
            item.setCardType(CARD_TYPE_SPLIT);
            item.setTitle(SfStaskWorkOrderAssembler.splitTitle(
                vo.getGreenhouseNameSnapshot(), vo.getWorkItemNameSnapshot()));
            SfStaskAcceptance acceptance = data.acceptances().get(vo.getOrderId());
            if (acceptance != null) {
                item.setAcceptanceResult(acceptance.getResult());
                item.setAcceptedAt(acceptance.getAcceptedAt());
            }
            boolean handler = Objects.equals(vo.getHandlerTechnicianEmployeeId(), currentEmployeeId);
            item.setRelatedToCurrentUser(Objects.equals(vo.getCreatorEmployeeId(), currentEmployeeId) || handler);
            item.setCanAcceptance(StaskOrderStatus.PENDING_ACCEPTANCE.equals(vo.getStatus()) && handler);
            if (StaskOrderStatus.PENDING_ACCEPTANCE.equals(vo.getStatus()) && !handler) {
                item.setReadonlyReason(messages.message(
                    com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_TECHNICIAN_NOT_HANDLER));
            }
            items.add(item);
        }
        SfStaskWorkOrderAssembler.sortTechnicianWorkbenchItems(items);
        return items;
    }

    /**
     * 装配组长工作台拆分工单。
     */
    public List<SfStaskLeaderWorkbenchItemVo> leaderItems(List<SfStaskWorkOrder> orders,
        Map<Long, SysEmployeeVo> employeeIndex, Map<String, String> roleNames,
        SfStaskSplitWorkbenchBatchReader.SplitData data) {
        if (CollUtil.isEmpty(orders)) {
            return List.of();
        }
        List<SfStaskLeaderWorkbenchItemVo> items = new ArrayList<>(orders.size());
        for (SfStaskWorkOrder order : orders) {
            SfStaskWorkOrderVo vo = SfStaskWorkOrderAssembler.toWorkOrderVo(order, employeeIndex, Map.of());
            applyLabor(order, vo, data);
            SfStaskLeaderWorkbenchItemVo item = new SfStaskLeaderWorkbenchItemVo();
            BeanUtil.copyProperties(vo, item);
            item.setCardType(CARD_TYPE_SPLIT);
            item.setTitle(SfStaskWorkOrderAssembler.splitTitle(
                order.getGreenhouseNameSnapshot(), order.getWorkItemNameSnapshot()));
            item.setCreatorRoleCode(order.getCreatorRoleCode());
            item.setCreatorRoleName(resolveRoleName(roleNames, order.getCreatorRoleCode()));
            fillLeaderFields(order, item, data);
            items.add(item);
        }
        SfStaskWorkOrderAssembler.sortLeaderWorkbenchItems(items);
        return items;
    }

    /**
     * 装配兼容工作台的普通拆分工单列表。
     */
    public List<SfStaskWorkOrderVo> legacyOrders(List<SfStaskWorkOrder> orders,
        Map<Long, SysEmployeeVo> employeeIndex,
        Map<Long, List<SfStaskGreenhouseBriefVo>> greenhouseIndex) {
        if (CollUtil.isEmpty(orders)) {
            return List.of();
        }
        return orders.stream()
            .map(order -> SfStaskWorkOrderAssembler.toWorkOrderVo(order, employeeIndex, greenhouseIndex))
            .toList();
    }

    private void fillManagerFields(SfStaskWorkOrder order, SfStaskWorkOrderVo vo,
        SfStaskSplitWorkbenchBatchReader.SplitData data) {
        String status = order.getStatus();
        vo.setStatusLabel(SfStaskWorkOrderAssembler.splitStatusLabel(status, messages));
        if (StaskOrderStatus.PENDING_ACCEPTANCE.equals(status)) {
            vo.setPrimaryAction(ACTION_ACCEPT);
            SfStaskCompletion completion = data.completions().get(order.getOrderId());
            if (completion != null) {
                vo.setCompletedAt(completion.getCompletedAt());
                vo.setEventTime(completion.getCompletedAt());
            }
            vo.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_COMPLETION_TIME));
            return;
        }
        if (StaskOrderStatus.PENDING_LEADER_ACCEPT.equals(status)) {
            fillIssuedTime(order, vo, data.issuedLogs());
            return;
        }
        if (StaskOrderStatus.ASSIGN_COMPLETE.equals(status)) {
            SfStaskFlowLog acceptedLog = SfStaskWorkbenchTimeSupport.latestFlowLog(
                data.flowLogs(), order.getOrderId(), StaskOrderEvent.LEADER_ACCEPT);
            vo.setEventTime(acceptedLog == null
                ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getCreateTime())
                : acceptedLog.getCreateTime());
            vo.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_DISPATCH_COMPLETED_TIME));
            return;
        }
        if (StaskOrderStatus.LEADER_ARRIVED.equals(status)) {
            SfStaskClockRecord clock = data.clocks().get(order.getOrderId());
            vo.setEventTime(clock == null
                ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getCreateTime())
                : clock.getClockTime());
            vo.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ARRIVAL_TIME));
            return;
        }
        if (StaskOrderStatus.VOIDED.equals(status)) {
            SfStaskFlowLog voidLog = SfStaskWorkbenchTimeSupport.latestFlowLog(
                data.flowLogs(), order.getOrderId(), StaskOrderEvent.VOID);
            vo.setEventTime(voidLog == null
                ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getUpdateTime())
                : voidLog.getCreateTime());
            vo.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_CANCELLED_TIME));
            return;
        }
        if (StaskOrderStatus.CANCELLED.equals(status)) {
            SfStaskFlowLog cancelLog = SfStaskWorkbenchTimeSupport.latestFlowLog(
                data.flowLogs(), order.getOrderId(), StaskOrderEvent.CANCEL);
            vo.setEventTime(cancelLog == null
                ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getUpdateTime())
                : cancelLog.getCreateTime());
            vo.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_REVOKED_TIME));
        }
    }

    /** 用工人数按组长与计划日关联，兼容字段与正式字段必须保持相同来源。 */
    private void applyLabor(SfStaskWorkOrder order, SfStaskWorkOrderVo vo,
        SfStaskSplitWorkbenchBatchReader.SplitData data) {
        if (order.getLeaderId() == null || order.getPlanDate() == null) return;
        String key = order.getLeaderId() + "_"
            + order.getPlanDate().toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
        SfStaskLeaderLaborRecord record = data.laborRecords().get(key);
        BigDecimal count = record == null ? null : record.getLaborCount();
        vo.setDailyLaborCount(count);
        vo.setRequiredWorkerCount(count == null ? null : count.doubleValue());
        vo.setLaborRecordVersion(record == null ? null : record.getVersion());
    }

    private void fillLeaderFields(SfStaskWorkOrder order, SfStaskLeaderWorkbenchItemVo item,
        SfStaskSplitWorkbenchBatchReader.SplitData data) {
        String status = order.getStatus();
        if (StaskOrderStatus.PENDING_LEADER_ACCEPT.equals(status)) {
            item.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_PENDING_LEADER_ACCEPT));
            item.setPrimaryAction(ACTION_LEADER_ACCEPT);
            fillIssuedTime(order, item, data.issuedLogs());
            return;
        }
        if (StaskOrderStatus.ASSIGN_COMPLETE.equals(status)) {
            item.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_DISPATCH_COMPLETED));
            item.setPrimaryAction(ACTION_CLOCK_IN);
            // 到岗资格只取决于本人组长身份与已接单状态，计划日期不再限制为当天。
            item.setClockInEnabled(true);
            SfStaskFlowLog acceptedLog = SfStaskWorkbenchTimeSupport.latestFlowLog(
                data.flowLogs(), order.getOrderId(), StaskOrderEvent.LEADER_ACCEPT);
            item.setEventTime(acceptedLog == null
                ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getCreateTime())
                : acceptedLog.getCreateTime());
            item.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_DISPATCH_COMPLETED_TIME));
            return;
        }
        if (StaskOrderStatus.LEADER_ARRIVED.equals(status)) {
            item.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_ARRIVED));
            item.setPrimaryAction(ACTION_APPLY_COMPLETE);
            SfStaskClockRecord clock = data.clocks().get(order.getOrderId());
            item.setEventTime(clock == null
                ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getCreateTime())
                : clock.getClockTime());
            item.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ARRIVAL_TIME));
            return;
        }
        if (StaskOrderStatus.PENDING_ACCEPTANCE.equals(status)) {
            item.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_PENDING_ACCEPTANCE));
            SfStaskCompletion completion = data.completions().get(order.getOrderId());
            if (completion != null) {
                item.setCompletedAt(completion.getCompletedAt());
                item.setEventTime(completion.getCompletedAt());
            }
            item.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_COMPLETION_TIME));
            return;
        }
        if (StaskOrderStatus.ACCEPTANCE_PASSED.equals(status)) {
            item.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_PASSED));
            SfStaskAcceptance acceptance = data.acceptances().get(order.getOrderId());
            if (acceptance != null) {
                item.setAcceptanceResult(acceptance.getResult());
                item.setAcceptedAt(acceptance.getAcceptedAt());
                item.setEventTime(acceptance.getAcceptedAt());
                item.setEventTimeLabel(messages.message(
                    com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_TIME));
            }
        } else if (StaskOrderStatus.ACCEPTANCE_REJECTED.equals(status)) {
            item.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_REJECTED));
            item.setPrimaryAction(ACTION_REAPPLY_ACCEPTANCE);
        } else if (StaskOrderStatus.VOIDED.equals(status)) {
            item.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_CANCELLED));
            SfStaskFlowLog voidLog = SfStaskWorkbenchTimeSupport.latestFlowLog(
                data.flowLogs(), order.getOrderId(), StaskOrderEvent.VOID);
            item.setEventTime(voidLog == null
                ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getUpdateTime())
                : voidLog.getCreateTime());
            item.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_CANCELLED_TIME));
        }
    }

    private void fillIssuedTime(SfStaskWorkOrder order, SfStaskWorkOrderVo vo,
        Map<Long, SfStaskFlowLog> issuedLogs) {
        SfStaskFlowLog issuedLog = issuedLogs.get(order.getOrderId());
        vo.setEventTime(issuedLog == null
            ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getCreateTime())
            : issuedLog.getCreateTime());
        vo.setEventTimeLabel(issuedLog == null ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_CREATED_TIME) : messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ISSUED_TIME));
        vo.setElapsedHours(SfStaskWorkbenchTimeSupport.elapsedHours(
            issuedLog == null ? null : issuedLog.getCreateTime()));
    }

    private void fillIssuedTime(SfStaskWorkOrder order, SfStaskLeaderWorkbenchItemVo item,
        Map<Long, SfStaskFlowLog> issuedLogs) {
        SfStaskFlowLog issuedLog = issuedLogs.get(order.getOrderId());
        item.setEventTime(issuedLog == null
            ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getCreateTime())
            : issuedLog.getCreateTime());
        item.setEventTimeLabel(issuedLog == null ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_CREATED_TIME) : messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ISSUED_TIME));
        item.setElapsedHours(SfStaskWorkbenchTimeSupport.elapsedHours(
            issuedLog == null ? null : issuedLog.getCreateTime()));
    }

    private String resolveRoleName(Map<String, String> roleNames, String roleCode) {
        return SfStaskWorkOrderAssembler.localizedRoleName(roleCode, roleNames.get(roleCode), messages);
    }
}
