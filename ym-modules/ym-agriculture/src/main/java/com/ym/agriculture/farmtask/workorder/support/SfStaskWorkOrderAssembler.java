package com.ym.agriculture.farmtask.workorder.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskClockRecord;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskDispatchVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskFlowLogVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskGreenhouseBriefVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerHistoryItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * stask 工单无 IO 视图装配器。
 *
 * <p>所有方法只消费实体和调用方预加载的索引，不访问 Mapper、登录上下文或外部服务。</p>
 */
public final class SfStaskWorkOrderAssembler {

    private SfStaskWorkOrderAssembler() {
    }

    /**
     * 将拆分工单转换为列表 VO。
     *
     * @param order            工单实体
     * @param employeeIndex    预加载员工索引
     * @param greenhouseIndex  工单 ID 到大棚摘要列表的索引
     * @return 工单列表 VO
     */
    public static SfStaskWorkOrderVo toWorkOrderVo(SfStaskWorkOrder order,
        Map<Long, SysEmployeeVo> employeeIndex,
        Map<Long, List<SfStaskGreenhouseBriefVo>> greenhouseIndex) {
        SfStaskWorkOrderVo vo = new SfStaskWorkOrderVo();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPackageId(order.getPackageId());
        vo.setPlanDate(order.getPlanDate());
        vo.setGreenhouseId(order.getGreenhouseId());
        vo.setGreenhouseNameSnapshot(order.getGreenhouseNameSnapshot());
        vo.setGreenhouses(greenhouseIndex.getOrDefault(order.getOrderId(), List.of()));
        vo.setWorkItemId(order.getWorkItemId());
        vo.setWorkItemNameSnapshot(order.getWorkItemNameSnapshot());
        vo.setLeaderId(order.getLeaderId());
        vo.setLeaderName(employeeName(employeeIndex, order.getLeaderId()));
        vo.setCreatorEmployeeId(order.getCreatorEmployeeId());
        vo.setCreatorEmployeeName(employeeName(employeeIndex, order.getCreatorEmployeeId()));
        vo.setHandlerTechnicianEmployeeId(order.getHandlerTechnicianEmployeeId());
        vo.setHandlerTechnicianEmployeeName(handlerName(order.getHandlerTechnicianEmployeeId(),
            order.getHandlerTechnicianEmployeeNameSnapshot(), employeeIndex));
        vo.setStatus(order.getStatus());
        vo.setRequiredWorkerCount(order.getRequiredWorkerCount());
        vo.setAcceptedWorkerCount(order.getAcceptedWorkerCount());
        vo.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getCreateTime()));
        return vo;
    }

    /**
     * 将任务包转换为兼容工单列表 VO。
     *
     * @param taskPackage   任务包实体
     * @param employeeIndex 预加载员工索引
     * @param greenhouses   任务包大棚摘要
     * @return 工单列表 VO
     */
    public static SfStaskWorkOrderVo toPackageVo(SfStaskTaskPackage taskPackage,
        Map<Long, SysEmployeeVo> employeeIndex, List<SfStaskGreenhouseBriefVo> greenhouses) {
        SfStaskWorkOrderVo vo = new SfStaskWorkOrderVo();
        vo.setOrderId(taskPackage.getPackageId());
        vo.setOrderNo(taskPackage.getPackageNo());
        vo.setPackageId(taskPackage.getPackageId());
        vo.setPlanDate(taskPackage.getPlanDate());
        vo.setGreenhouses(CollUtil.emptyIfNull(greenhouses));
        vo.setCreatorEmployeeId(taskPackage.getCreatorEmployeeId());
        vo.setCreatorEmployeeName(employeeName(employeeIndex, taskPackage.getCreatorEmployeeId()));
        vo.setHandlerTechnicianEmployeeId(taskPackage.getHandlerTechnicianEmployeeId());
        vo.setHandlerTechnicianEmployeeName(handlerName(taskPackage.getHandlerTechnicianEmployeeId(),
            taskPackage.getHandlerTechnicianEmployeeNameSnapshot(), employeeIndex));
        vo.setStatus(taskPackage.getStatus());
        vo.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(taskPackage.getCreateTime()));
        return vo;
    }

    /**
     * 将派工实体转换为派工 VO。
     *
     * @param dispatch      派工实体
     * @param employeeIndex 预加载员工索引
     * @return 派工 VO
     */
    public static SfStaskDispatchVo toDispatchVo(
        SfStaskDispatch dispatch, Map<Long, SysEmployeeVo> employeeIndex) {
        SfStaskDispatchVo vo = new SfStaskDispatchVo();
        vo.setDispatchId(dispatch.getDispatchId());
        vo.setWorkerId(dispatch.getWorkerId());
        SysEmployeeVo worker = employeeIndex.get(dispatch.getWorkerId());
        if (worker != null) {
            vo.setWorkerName(worker.getName());
            vo.setWorkerPhone(worker.getPhone());
        }
        vo.setStatus(dispatch.getStatus());
        vo.setRejectReason(dispatch.getRejectReason());
        vo.setInvitedAt(dispatch.getInvitedAt());
        vo.setRespondedAt(dispatch.getRespondedAt());
        return vo;
    }

    /**
     * 将流转日志实体转换为 VO。
     *
     * @param log              流转日志实体
     * @param employeeIndex    预加载员工索引
     * @param operatorRoleName 预解析操作人角色名称
     * @return 流转日志 VO
     */
    public static SfStaskFlowLogVo toFlowLogVo(SfStaskFlowLog log,
        Map<Long, SysEmployeeVo> employeeIndex, String operatorRoleName) {
        SfStaskFlowLogVo vo = new SfStaskFlowLogVo();
        vo.setLogId(log.getLogId());
        vo.setFromStatus(log.getFromStatus());
        vo.setToStatus(log.getToStatus());
        vo.setEvent(log.getEvent());
        vo.setOperatorEmployeeId(log.getOperatorEmployeeId());
        vo.setOperatorEmployeeName(employeeName(employeeIndex, log.getOperatorEmployeeId()));
        vo.setOperatorRoleCode(log.getOperatorRoleCode());
        vo.setOperatorRoleName(operatorRoleName);
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    /**
     * 转换大棚摘要。
     *
     * @param row 工单大棚实体
     * @return 大棚摘要
     */
    public static SfStaskGreenhouseBriefVo toGreenhouseBrief(SfStaskWorkOrderGreenhouse row) {
        SfStaskGreenhouseBriefVo vo = new SfStaskGreenhouseBriefVo();
        vo.setGreenhouseItemId(row.getGreenhouseItemId());
        vo.setGreenhouseId(row.getGreenhouseId());
        vo.setGreenhouseCode(row.getGreenhouseCodeSnapshot());
        vo.setGreenhouseName(row.getGreenhouseNameSnapshot());
        return vo;
    }

    /**
     * 转换大棚摘要列表。
     *
     * @param rows 工单大棚实体列表
     * @return 大棚摘要列表
     */
    public static List<SfStaskGreenhouseBriefVo> toGreenhouseBriefs(List<SfStaskWorkOrderGreenhouse> rows) {
        if (CollUtil.isEmpty(rows)) {
            return List.of();
        }
        return rows.stream().map(SfStaskWorkOrderAssembler::toGreenhouseBrief).toList();
    }

    /**
     * 按大棚 ID 去重转换摘要列表。
     *
     * @param rows 工单大棚实体列表
     * @return 去重大棚摘要列表
     */
    public static List<SfStaskGreenhouseBriefVo> toDistinctGreenhouseBriefs(
        List<SfStaskWorkOrderGreenhouse> rows) {
        if (CollUtil.isEmpty(rows)) {
            return List.of();
        }
        Map<Long, SfStaskGreenhouseBriefVo> result = new LinkedHashMap<>();
        for (SfStaskWorkOrderGreenhouse row : rows) {
            if (row.getGreenhouseId() != null) {
                result.putIfAbsent(row.getGreenhouseId(), toGreenhouseBrief(row));
            }
        }
        return new ArrayList<>(result.values());
    }

    /**
     * 解析任务包状态文案。
     *
     * @param status 工单状态
     * @return 状态文案
     */
    public static String packageStatusLabel(String status,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case StaskOrderStatus.DRAFT -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_DRAFT);
            case StaskOrderStatus.PENDING_TECH_CONFIRM -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_PENDING_TECH_CONFIRM);
            case StaskOrderStatus.TECH_REJECTED -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_TECH_REJECTED);
            default -> splitStatusLabel(status, messages);
        };
    }

    /**
     * 解析任务包主操作。
     *
     * @param status 工单状态
     * @return 主操作编码
     */
    public static String packagePrimaryAction(String status) {
        if (StaskOrderStatus.DRAFT.equals(status)) {
            return "EDIT";
        }
        if (StaskOrderStatus.TECH_REJECTED.equals(status)) {
            return "PROCESS";
        }
        return null;
    }

    /**
     * 解析拆分工单状态文案。
     *
     * @param status 工单状态
     * @return 状态文案
     */
    public static String splitStatusLabel(String status,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case StaskOrderStatus.PENDING_LEADER_ACCEPT -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_PENDING_LEADER_ACCEPT);
            case StaskOrderStatus.PENDING_LEADER_ASSIGN -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_PENDING_DISPATCH);
            case StaskOrderStatus.ASSIGN_COMPLETE -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_DISPATCH_COMPLETED);
            case StaskOrderStatus.LEADER_ARRIVED -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_ARRIVED);
            case StaskOrderStatus.PENDING_ACCEPTANCE -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_PENDING_ACCEPTANCE);
            case StaskOrderStatus.ACCEPTANCE_PASSED -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_PASSED);
            case StaskOrderStatus.ACCEPTANCE_REJECTED -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_REJECTED);
            case StaskOrderStatus.VOIDED -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_CANCELLED);
            case StaskOrderStatus.CANCELLED -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_DISPATCH_STATUS_CANCELLED);
            default -> status;
        };
    }

    /**
     * 解析创建角色文案。
     *
     * @param creatorRoleCode 创建角色编码
     * @return 创建角色文案
     */
    public static String creatorRoleLabel(String creatorRoleCode, StaskMessageResolver messages) {
        if (creatorRoleCode == null) {
            return "";
        }
        return switch (creatorRoleCode) {
            case EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN ->
                messages.message(StaskMessageKeys.LABEL_COMMON_ROLE_PRODUCTION);
            case EmployeeConstants.APP_ROLE_STASK_EXPERT ->
                messages.message(StaskMessageKeys.LABEL_COMMON_ROLE_TECHNICIAN);
            case EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER ->
                messages.message(StaskMessageKeys.LABEL_COMMON_ROLE_GROUP_LEADER);
            case EmployeeConstants.APP_ROLE_STASK_WORKER ->
                messages.message(StaskMessageKeys.LABEL_COMMON_ROLE_WORKER);
            default -> "";
        };
    }

    /**
     * 已知 stask 角色固定使用当前请求语言，未知扩展角色回退到租户配置名称。
     */
    public static String localizedRoleName(String roleCode, String configuredName,
        StaskMessageResolver messages) {
        String localized = creatorRoleLabel(roleCode, messages);
        return StrUtil.isNotBlank(localized) ? localized : StrUtil.blankToDefault(configuredName, roleCode);
    }

    /**
     * 解析工人端任务状态。
     *
     * @param dispatch 派工实体
     * @param order    工单实体
     * @return 工人端状态编码
     */
    public static String workerTaskStatus(SfStaskDispatch dispatch, SfStaskWorkOrder order) {
        return SfStaskWorkerTaskAssembler.taskStatus(dispatch, order);
    }

    /**
     * 解析工人端任务状态文案。
     *
     * @param status 工人端状态编码
     * @return 状态文案
     */
    public static String workerTaskStatusLabel(String status,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        return SfStaskWorkerTaskAssembler.taskStatusLabel(status, messages);
    }

    /**
     * 解析工人端任务主操作。
     *
     * @param status 工人端状态编码
     * @return 主操作编码
     */
    public static String workerTaskPrimaryAction(String status) {
        return SfStaskWorkerTaskAssembler.taskPrimaryAction(status);
    }

    /**
     * 解析计划日期相对文案。
     *
     * @param planDate 计划日期
     * @return 相对日期文案
     */
    public static String planDateRelativeLabel(Date planDate,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        return SfStaskWorkerTaskAssembler.planDateRelativeLabel(planDate, messages);
    }

    /**
     * 解析组长评价文案。
     *
     * @param leaderEvaluation 组长评价编码
     * @return 评价文案
     */
    public static String leaderEvaluationLabel(String leaderEvaluation,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        return SfStaskWorkerTaskAssembler.leaderEvaluationLabel(leaderEvaluation, messages);
    }

    /**
     * 解析拆分工单标题。
     *
     * @param greenhouseName 大棚名称
     * @param workItemName   农事项名称
     * @return 工单标题
     */
    public static String splitTitle(String greenhouseName, String workItemName) {
        if (StrUtil.isBlank(greenhouseName)) {
            return workItemName;
        }
        if (StrUtil.isBlank(workItemName)) {
            return greenhouseName;
        }
        return greenhouseName + " · " + workItemName;
    }

    /**
     * 将工人派工与工单装配为全部任务条目。
     */
    public static SfStaskWorkerAllTaskItemVo toWorkerAllTaskItem(SfStaskDispatch dispatch,
        SfStaskWorkOrder order, SfStaskAcceptance acceptance, Map<Long, SysEmployeeVo> employeeIndex,
        boolean reinvited, com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        return SfStaskWorkerTaskAssembler.toAllTaskItem(
            dispatch, order, acceptance, employeeIndex, reinvited, messages);
    }

    /**
     * 装配工人首页任务条目。
     */
    public static SfStaskWorkerTaskItemVo toWorkerTaskItem(SfStaskDispatch dispatch, SfStaskWorkOrder order,
        Map<Long, SysEmployeeVo> employeeIndex, boolean reinvited, String primaryAction,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        return SfStaskWorkerTaskAssembler.toHomeTaskItem(
            dispatch, order, employeeIndex, reinvited, primaryAction, messages);
    }

    /**
     * 装配工人历史任务条目。
     */
    public static SfStaskWorkerHistoryItemVo toWorkerHistoryItem(SfStaskDispatch dispatch,
        SfStaskWorkOrder order, SfStaskAcceptance acceptance, Map<Long, SysEmployeeVo> employeeIndex,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        return SfStaskWorkerTaskAssembler.toHistoryItem(dispatch, order, acceptance, employeeIndex, messages);
    }

    /**
     * 每个工人仅保留邀请时间最新的一条派工记录。
     */
    public static List<SfStaskDispatch> collapseLatestDispatchPerWorker(List<SfStaskDispatch> dispatches) {
        return SfStaskWorkerTaskAssembler.collapseLatestPerWorker(dispatches);
    }

    /**
     * 按业务事件建立最新流转日志索引。
     */
    public static Map<String, SfStaskFlowLog> indexLatestFlowLogs(List<SfStaskFlowLog> logs) {
        return SfStaskWorkOrderRecordIndex.latestFlowLogs(logs);
    }

    /**
     * 建立工单发放时间日志索引。
     */
    public static Map<Long, SfStaskFlowLog> indexIssuedAtLogs(List<SfStaskFlowLog> logs) {
        return SfStaskWorkOrderRecordIndex.issuedAtLogs(logs);
    }

    /**
     * 建立最新完工记录索引。
     */
    public static Map<Long, SfStaskCompletion> indexLatestCompletions(List<SfStaskCompletion> completions) {
        return SfStaskWorkOrderRecordIndex.latestCompletions(completions);
    }

    /**
     * 建立最新打卡记录索引。
     */
    public static Map<Long, SfStaskClockRecord> indexLatestClockRecords(List<SfStaskClockRecord> records) {
        return SfStaskWorkOrderRecordIndex.latestClockRecords(records);
    }

    /**
     * 建立最新验收记录索引。
     */
    public static Map<Long, SfStaskAcceptance> indexLatestAcceptances(List<SfStaskAcceptance> rows) {
        return SfStaskWorkOrderRecordIndex.latestAcceptances(rows);
    }

    /**
     * 按最新业务时间排序管理端工作台条目。
     */
    public static void sortManagerWorkbenchItems(List<SfStaskManagerWorkbenchItemVo> items) {
        SfStaskWorkbenchItemSorter.sortManagerItems(items);
    }

    /**
     * 按最新业务时间排序技术员工作台条目。
     */
    public static void sortTechnicianWorkbenchItems(List<SfStaskTechnicianWorkbenchItemVo> items) {
        SfStaskWorkbenchItemSorter.sortTechnicianItems(items);
    }

    /**
     * 按最新业务时间排序组长工作台条目。
     */
    public static void sortLeaderWorkbenchItems(List<SfStaskLeaderWorkbenchItemVo> items) {
        SfStaskWorkbenchItemSorter.sortLeaderItems(items);
    }

    /**
     * 按邀请时间和计划日期排序工人首页派工记录。
     */
    public static void sortWorkerHomeDispatches(
        List<SfStaskDispatch> dispatches, Map<Long, SfStaskWorkOrder> orderIndex) {
        SfStaskWorkerTaskAssembler.sortHomeDispatches(dispatches, orderIndex);
    }

    private static String employeeName(Map<Long, SysEmployeeVo> employeeIndex, Long employeeId) {
        if (employeeId == null || CollUtil.isEmpty(employeeIndex)) {
            return null;
        }
        SysEmployeeVo employee = employeeIndex.get(employeeId);
        return employee == null ? null : employee.getName();
    }

    private static String handlerName(Long employeeId, String snapshot,
        Map<Long, SysEmployeeVo> employeeIndex) {
        if (StrUtil.isNotBlank(snapshot)) {
            return snapshot;
        }
        return employeeName(employeeIndex, employeeId);
    }

}
