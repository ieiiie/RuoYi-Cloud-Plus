package com.ym.agriculture.farmtask.workorder.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskDispatchStatus;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerHistoryItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerTaskItemVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * stask 工人任务视图装配器。
 *
 * <p>负责工人首页、全部任务和历史任务的纯内存状态与字段转换。</p>
 */
public final class SfStaskWorkerTaskAssembler {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private SfStaskWorkerTaskAssembler() {
    }

    /**
     * 解析工人端任务状态。
     *
     * @param dispatch 派工实体
     * @param order 工单实体
     * @return 工人端状态编码
     */
    public static String taskStatus(SfStaskDispatch dispatch, SfStaskWorkOrder order) {
        if (StaskOrderStatus.VOIDED.equals(order.getStatus())
            || StaskOrderStatus.CANCELLED.equals(order.getStatus())
            || StaskDispatchStatus.CANCELLED.equals(dispatch.getStatus())) {
            return "VOIDED";
        }
        if (StaskDispatchStatus.REJECTED.equals(dispatch.getStatus())) {
            return "REJECTED";
        }
        if (StaskDispatchStatus.PENDING.equals(dispatch.getStatus())) {
            return "PENDING_CONFIRM";
        }
        if (StaskDispatchStatus.ACCEPTED.equals(dispatch.getStatus())) {
            if (StaskOrderStatus.ACCEPTANCE_PASSED.equals(order.getStatus())) {
                return "COMPLETED";
            }
            if (StaskOrderStatus.ACCEPTANCE_REJECTED.equals(order.getStatus())) {
                return "NOT_PASSED";
            }
        }
        return "ACCEPTED";
    }

    /**
     * 解析工人端任务状态文案。
     *
     * @param status 工人端状态编码
     * @return 状态文案
     */
    public static String taskStatusLabel(String status,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "PENDING_CONFIRM" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DISPATCH_STATUS_PENDING);
            case "ACCEPTED" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DISPATCH_STATUS_ACCEPTED);
            case "REJECTED" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DISPATCH_STATUS_REJECTED);
            case "COMPLETED" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_COMPLETED);
            case "NOT_PASSED" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_REJECTED);
            case "VOIDED" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_TASK_CANCELLED);
            default -> status;
        };
    }

    /**
     * 解析工人端任务主操作。
     *
     * @param status 工人端状态编码
     * @return 主操作编码
     */
    public static String taskPrimaryAction(String status) {
        if ("PENDING_CONFIRM".equals(status)) {
            return "ACCEPT";
        }
        if ("ACCEPTED".equals(status)) {
            return "VIEW_INSTRUCTION";
        }
        return null;
    }

    /**
     * 解析计划日期相对文案。
     *
     * @param planDate 计划日期
     * @return 相对日期文案
     */
    public static String planDateRelativeLabel(Date planDate,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        LocalDate target = toLocalDate(planDate);
        if (target == null) {
            return null;
        }
        LocalDate today = LocalDate.now(CHINA_ZONE);
        int yearDiff = target.getYear() - today.getYear();
        if (yearDiff != 0) {
            return messages.message(yearDiff > 0
                ? com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_YEARS_LATER
                : com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_YEARS_AGO, Math.abs(yearDiff));
        }
        int monthDiff = target.getMonthValue() - today.getMonthValue();
        if (monthDiff != 0) {
            return messages.message(monthDiff > 0
                ? com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_MONTHS_LATER
                : com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_MONTHS_AGO, Math.abs(monthDiff));
        }
        long dayDiff = ChronoUnit.DAYS.between(today, target);
        return switch ((int) dayDiff) {
            case 2 -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_DAY_AFTER_TOMORROW);
            case 1 -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_TOMORROW);
            case 0 -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_TODAY);
            case -1 -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_YESTERDAY);
            case -2 -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_DAY_BEFORE_YESTERDAY);
            default -> messages.message(dayDiff >= 3
                ? com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_DAYS_LATER
                : com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DATE_DAYS_AGO, Math.abs(dayDiff));
        };
    }

    /**
     * 解析组长评价文案。
     *
     * @param leaderEvaluation 组长评价编码
     * @return 评价文案
     */
    public static String leaderEvaluationLabel(String leaderEvaluation,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        if (StrUtil.isBlank(leaderEvaluation)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_EVALUATION_PENDING);
        }
        return switch (leaderEvaluation) {
            case "EXCELLENT" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_EVALUATION_EXCELLENT);
            case "QUALIFIED" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_EVALUATION_QUALIFIED);
            case "REWORK" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_EVALUATION_REWORK);
            default -> leaderEvaluation;
        };
    }

    /**
     * 装配工人全部任务条目。
     *
     * @param dispatch 派工实体
     * @param order 工单实体
     * @param acceptance 验收记录
     * @param employeeIndex 员工索引
     * @param reinvited 是否再次邀请
     * @return 工人全部任务条目
     */
    public static SfStaskWorkerAllTaskItemVo toAllTaskItem(SfStaskDispatch dispatch,
        SfStaskWorkOrder order, SfStaskAcceptance acceptance, Map<Long, SysEmployeeVo> employeeIndex,
        boolean reinvited, com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        if (dispatch == null || order == null) {
            return null;
        }
        String uiStatus = taskStatus(dispatch, order);
        SfStaskWorkerAllTaskItemVo item = new SfStaskWorkerAllTaskItemVo();
        item.setDispatchId(dispatch.getDispatchId());
        item.setOrderId(order.getOrderId());
        item.setTitle(splitTitle(
            order.getGreenhouseNameSnapshot(), order.getWorkItemNameSnapshot()));
        item.setGreenhouseNameSnapshot(order.getGreenhouseNameSnapshot());
        item.setWorkItemNameSnapshot(order.getWorkItemNameSnapshot());
        item.setLeaderId(order.getLeaderId());
        item.setLeaderName(employeeName(employeeIndex, order.getLeaderId()));
        item.setGreenhouseId(order.getGreenhouseId());
        item.setWorkItemId(order.getWorkItemId());
        item.setPlanDate(order.getPlanDate());
        item.setPlanDateLabel(planDateRelativeLabel(order.getPlanDate(), messages));
        item.setStatus(uiStatus);
        item.setStatusLabel(taskStatusLabel(uiStatus, messages));
        item.setDispatchStatus(dispatch.getStatus());
        item.setOrderStatus(order.getStatus());
        item.setPrimaryAction(taskPrimaryAction(uiStatus));
        item.setReinvited(reinvited);
        item.setRespondedAt(dispatch.getRespondedAt());
        item.setInvitedAt(dispatch.getInvitedAt());
        item.setCompletedAt(acceptance == null ? null : acceptance.getAcceptedAt());
        item.setLeaderEvaluation(dispatch.getLeaderEvaluation());
        item.setLeaderEvaluationLabel(leaderEvaluationLabel(dispatch.getLeaderEvaluation(), messages));
        return item;
    }

    /**
     * 装配工人首页任务条目。
     */
    public static SfStaskWorkerTaskItemVo toHomeTaskItem(SfStaskDispatch dispatch, SfStaskWorkOrder order,
        Map<Long, SysEmployeeVo> employeeIndex, boolean reinvited, String primaryAction,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        SfStaskWorkerTaskItemVo item = new SfStaskWorkerTaskItemVo();
        item.setDispatchId(dispatch.getDispatchId());
        item.setOrderId(order.getOrderId());
        item.setTitle(splitTitle(
            order.getGreenhouseNameSnapshot(), order.getWorkItemNameSnapshot()));
        item.setGreenhouseNameSnapshot(order.getGreenhouseNameSnapshot());
        item.setWorkItemNameSnapshot(order.getWorkItemNameSnapshot());
        item.setLeaderId(order.getLeaderId());
        item.setLeaderName(employeeName(employeeIndex, order.getLeaderId()));
        item.setGreenhouseId(order.getGreenhouseId());
        item.setPlanDate(order.getPlanDate());
        item.setPlanDateLabel(planDateRelativeLabel(order.getPlanDate(), messages));
        item.setDispatchStatus(dispatch.getStatus());
        item.setPrimaryAction(primaryAction);
        item.setReinvited(reinvited);
        return item;
    }

    /**
     * 装配工人历史任务条目。
     */
    public static SfStaskWorkerHistoryItemVo toHistoryItem(SfStaskDispatch dispatch,
        SfStaskWorkOrder order, SfStaskAcceptance acceptance, Map<Long, SysEmployeeVo> employeeIndex,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        if (dispatch == null || order == null) {
            return null;
        }
        SfStaskWorkerHistoryItemVo item = new SfStaskWorkerHistoryItemVo();
        item.setDispatchId(dispatch.getDispatchId());
        item.setOrderId(order.getOrderId());
        item.setTitle(splitTitle(
            order.getGreenhouseNameSnapshot(), order.getWorkItemNameSnapshot()));
        item.setGreenhouseNameSnapshot(order.getGreenhouseNameSnapshot());
        item.setWorkItemNameSnapshot(order.getWorkItemNameSnapshot());
        item.setLeaderId(order.getLeaderId());
        item.setLeaderName(employeeName(employeeIndex, order.getLeaderId()));
        item.setPlanDate(order.getPlanDate());
        item.setPlanDateLabel(planDateRelativeLabel(order.getPlanDate(), messages));
        item.setCompletedAt(acceptance == null ? null : acceptance.getAcceptedAt());
        item.setLeaderEvaluation(dispatch.getLeaderEvaluation());
        item.setLeaderEvaluationLabel(leaderEvaluationLabel(dispatch.getLeaderEvaluation(), messages));
        return item;
    }

    /**
     * 每个工人仅保留邀请时间最新的一条派工记录。
     *
     * @param dispatches 派工记录
     * @return 按工人折叠后的派工记录
     */
    public static List<SfStaskDispatch> collapseLatestPerWorker(List<SfStaskDispatch> dispatches) {
        if (CollUtil.isEmpty(dispatches)) {
            return List.of();
        }
        List<SfStaskDispatch> withoutWorker = new ArrayList<>();
        Map<Long, SfStaskDispatch> latestByWorker = new LinkedHashMap<>();
        for (SfStaskDispatch dispatch : dispatches) {
            if (dispatch.getWorkerId() == null) {
                withoutWorker.add(dispatch);
                continue;
            }
            latestByWorker.merge(dispatch.getWorkerId(), dispatch, SfStaskWorkerTaskAssembler::pickLaterDispatch);
        }
        List<SfStaskDispatch> result = new ArrayList<>(latestByWorker.values());
        result.addAll(withoutWorker);
        return result;
    }

    /**
     * 按邀请时间和计划日期排序工人首页派工记录。
     *
     * @param dispatches 派工记录
     * @param orderIndex 工单索引
     */
    public static void sortHomeDispatches(List<SfStaskDispatch> dispatches,
        Map<Long, SfStaskWorkOrder> orderIndex) {
        if (CollUtil.isEmpty(dispatches)) {
            return;
        }
        dispatches.sort(Comparator
            .comparing(SfStaskDispatch::getInvitedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(row -> {
                SfStaskWorkOrder order = orderIndex.get(row.getOrderId());
                return order == null ? null : order.getPlanDate();
            }, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    private static String employeeName(Map<Long, SysEmployeeVo> employeeIndex, Long employeeId) {
        if (employeeId == null || CollUtil.isEmpty(employeeIndex)) {
            return null;
        }
        SysEmployeeVo employee = employeeIndex.get(employeeId);
        return employee == null ? null : employee.getName();
    }

    private static String splitTitle(String greenhouseName, String workItemName) {
        if (StrUtil.isBlank(greenhouseName)) {
            return workItemName;
        }
        if (StrUtil.isBlank(workItemName)) {
            return greenhouseName;
        }
        return greenhouseName + " · " + workItemName;
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(CHINA_ZONE).toLocalDate();
    }

    private static SfStaskDispatch pickLaterDispatch(SfStaskDispatch current, SfStaskDispatch candidate) {
        return compareDispatchInvitedAt(candidate, current) > 0 ? candidate : current;
    }

    private static int compareDispatchInvitedAt(SfStaskDispatch first, SfStaskDispatch second) {
        Date firstInvitedAt = first.getInvitedAt();
        Date secondInvitedAt = second.getInvitedAt();
        if (firstInvitedAt == null && secondInvitedAt == null) {
            return Long.compare(ObjectUtil.defaultIfNull(first.getDispatchId(), 0L),
                ObjectUtil.defaultIfNull(second.getDispatchId(), 0L));
        }
        if (firstInvitedAt == null) {
            return -1;
        }
        if (secondInvitedAt == null) {
            return 1;
        }
        int invitedCompare = firstInvitedAt.compareTo(secondInvitedAt);
        return invitedCompare != 0 ? invitedCompare : Long.compare(
            ObjectUtil.defaultIfNull(first.getDispatchId(), 0L),
            ObjectUtil.defaultIfNull(second.getDispatchId(), 0L));
    }
}
