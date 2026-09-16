package com.ym.agriculture.farmtask.workorder.service.query;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.system.api.model.LoginUser;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farmtask.assignment.dao.SfFarmWorkAssignmentMapper;
import com.ym.agriculture.farmtask.assignment.model.entity.SfFarmWorkAssignment;
import com.ym.agriculture.farmtask.leaderlabor.dao.SfStaskLeaderLaborRecordMapper;
import com.ym.agriculture.farmtask.leaderlabor.model.entity.SfStaskLeaderLaborRecord;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.TaskMaterialMapper;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.TaskMaterial;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAcceptanceMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskCompletionMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskFlowLogMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderItemMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskLeaderAssignMode;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.*;
import com.ym.agriculture.farmtask.workorder.model.vo.*;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.SfStaskRoleNameReader;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.voice.model.vo.SfStaskVoiceBroadcastVo;
import com.ym.agriculture.farmtask.voice.service.ISfStaskVoiceBroadcastService;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * stask 任务包与拆分工单详情装配器。
 *
 * <p>调用方负责入口鉴权，本组件负责详情关联数据、权限字段和展示字段装配。</p>
 */
@RequiredArgsConstructor
@Service
public class SfStaskOrderDetailBuilder {

    private static final String ACTION_ACCEPT = "ACCEPT";

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskWorkOrderItemMapper itemMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfStaskDispatchMapper dispatchMapper;
    private final SfStaskCompletionMapper completionMapper;
    private final SfStaskAcceptanceMapper acceptanceMapper;
    private final SfStaskFlowLogMapper flowLogMapper;
    private final SfFarmWorkAssignmentMapper assignmentMapper;
    private final SfStaskRoleNameReader roleNameReader;
    private final SfStaskLeaderLaborRecordMapper laborRecordMapper;

    @Autowired(required = false)
    private TaskMaterialMapper taskMaterialMapper;

    /**
     * 任务播报按工单生成；详情入口已完成角色与任务可见性校验后才会读取。
     */
    @Autowired(required = false)
    private ISfStaskVoiceBroadcastService voiceBroadcastService;

    /**
     * 装配任务包详情。
     *
     * @param taskPackage 任务包
     * @param employees 员工查询上下文
     * @param splits 已批量加载的拆分工单
     * @return 任务包详情
     */
    public SfStaskWorkOrderDetailVo buildPackageDetail(SfStaskTaskPackage taskPackage,
        SfStaskEmployeeQueryContext employees, List<SfStaskWorkOrder> splits) {
        String tenantId = taskPackage.getTenantId();
        Long packageId = taskPackage.getPackageId();
        List<SfStaskWorkOrderGreenhouse> greenhouses = greenhouseMapper.selectByPackageId(tenantId, packageId);
        List<SfStaskWorkOrderItem> items = itemMapper.selectByPackageIds(tenantId, List.of(packageId));
        Map<Long, List<SfStaskTaskMaterialVo>> taskMaterials = loadTaskMaterials(tenantId, packageId);
        List<SfStaskFlowLog> logs = flowLogMapper.selectByOrderId(tenantId, packageId);
        Map<String, SfFarmWorkAssignment> assignmentIndex = loadAssignmentIndex(tenantId, items, greenhouses);
        employees.preload(Stream.of(
                Stream.of(taskPackage.getCreatorEmployeeId(), taskPackage.getHandlerTechnicianEmployeeId()),
                items.stream().flatMap(item -> Stream.of(item.getLeaderIdSnapshot(), item.getManualLeaderId())),
                splits.stream().flatMap(split -> Stream.of(
                    split.getLeaderId(), split.getHandlerTechnicianEmployeeId())),
                assignmentIndex.values().stream().map(SfFarmWorkAssignment::getLeaderId),
                logs.stream().map(SfStaskFlowLog::getOperatorEmployeeId))
            .flatMap(Function.identity()).filter(Objects::nonNull).distinct().toList());
        Map<String, String> roleNames = roleNameReader.load(Stream.concat(
                Stream.of(taskPackage.getCreatorRoleCode()),
                logs.stream().map(SfStaskFlowLog::getOperatorRoleCode))
            .filter(Objects::nonNull).distinct().toList());

        SfStaskWorkOrderDetailVo vo = new SfStaskWorkOrderDetailVo();
        SfStaskWorkOrderVo base = SfStaskWorkOrderAssembler.toPackageVo(
            taskPackage, employees.employees(), SfStaskWorkOrderAssembler.toDistinctGreenhouseBriefs(greenhouses));
        BeanUtil.copyProperties(base, vo);
        vo.setOverallTechNote(taskPackage.getOverallTechNote());
        vo.setCreatorRoleCode(taskPackage.getCreatorRoleCode());
        vo.setCreatorRoleName(roleNameReader.resolve(roleNames, taskPackage.getCreatorRoleCode()));
        vo.setPackageItems(buildPackageItemVos(items, greenhouses, splits, assignmentIndex,
            employees.employees(), taskMaterials));
        vo.setFlowLogs(toFlowLogVos(logs, employees.employees(), roleNames));
        String roleCode = currentRole(employees);
        fillPackagePermissions(taskPackage, vo, roleCode);
        fillTechReject(vo, taskPackage, logs, employees);
        attachSplitOrders(vo, splits, employees, roleCode);
        fillExpertPackageAcceptanceHint(taskPackage, vo, splits, roleCode);
        return vo;
    }

    /**
     * 装配拆分工单详情。
     *
     * @param order 拆分工单
     * @param employees 员工查询上下文
     * @return 拆分工单详情
     */
    public SfStaskWorkOrderDetailVo buildOrderDetail(SfStaskWorkOrder order,
        SfStaskEmployeeQueryContext employees) {
        return buildOrderDetail(order, employees, true, true);
    }

    /**
     * 装配后台管理端的任务详情。后台查看不应受小程序角色字段裁剪，也不读取语音播报。
     */
    public SfStaskWorkOrderDetailVo buildAdminOrderDetail(SfStaskWorkOrder order,
        SfStaskEmployeeQueryContext employees) {
        return buildOrderDetail(order, employees, false, false);
    }

    private SfStaskWorkOrderDetailVo buildOrderDetail(SfStaskWorkOrder order,
        SfStaskEmployeeQueryContext employees, boolean applyVisibility, boolean includeVoice) {
        String tenantId = order.getTenantId();
        Long orderId = order.getOrderId();
        List<SfStaskCompletion> completionHistory = completionMapper.selectByOrderIds(tenantId, List.of(orderId));
        SfStaskCompletion completion = SfStaskWorkOrderAssembler.indexLatestCompletions(completionHistory).get(orderId);
        SfStaskAcceptance acceptance = SfStaskWorkOrderAssembler.indexLatestAcceptances(
            acceptanceMapper.selectByOrderIds(tenantId, List.of(orderId))).get(orderId);
        List<SfStaskDispatch> dispatches = applyVisibility
            ? List.of() : CollUtil.emptyIfNull(dispatchMapper.selectByOrderId(tenantId, orderId));
        List<SfStaskFlowLog> logs = flowLogMapper.selectByOrderId(tenantId, orderId);
        employees.preload(Stream.of(
                Stream.of(order.getLeaderId(), order.getCreatorEmployeeId(), order.getHandlerTechnicianEmployeeId(),
                    acceptance == null ? null : acceptance.getAcceptorEmployeeId()),
                dispatches.stream().map(SfStaskDispatch::getWorkerId),
                logs.stream().map(SfStaskFlowLog::getOperatorEmployeeId))
            .flatMap(Function.identity()).filter(Objects::nonNull).distinct().toList());
        Map<String, String> roleNames = roleNameReader.load(Stream.of(
                Stream.of(order.getCreatorRoleCode(),
                    acceptance == null ? null : acceptance.getAcceptorRoleCode()),
                logs.stream().map(SfStaskFlowLog::getOperatorRoleCode))
            .flatMap(Function.identity()).filter(Objects::nonNull).distinct().toList());
        Map<Long, List<SfStaskGreenhouseBriefVo>> greenhouseIndex = loadOrderGreenhouses(
            tenantId, List.of(orderId));
        SfStaskWorkOrderDetailVo vo = new SfStaskWorkOrderDetailVo();
        BeanUtil.copyProperties(SfStaskWorkOrderAssembler.toWorkOrderVo(
            order, employees.employees(), greenhouseIndex), vo);
        applyLaborRecord(order, vo);
        vo.setManagerRequirement(order.getManagerRequirement());
        vo.setManagerPhotos(order.getManagerPhotos());
        vo.setTechInstruction(order.getTechInstruction());
        vo.setTechPhotos(order.getTechPhotos());
        vo.setOverallTechNote(order.getOverallTechNote());
        vo.setCreatorRoleCode(order.getCreatorRoleCode());
        vo.setCreatorRoleName(roleNameReader.resolve(roleNames, order.getCreatorRoleCode()));
        if (completion != null) {
            vo.setCompletionId(completion.getCompletionId());
            vo.setWorkPhotos(completion.getWorkPhotos());
            vo.setCompletionRemark(completion.getCompletionRemark());
            vo.setCompletedAt(completion.getCompletedAt());
        }
        vo.setCompletionHistory(toCompletionHistoryVos(completionHistory));
        if (acceptance != null) {
            vo.setAcceptanceId(acceptance.getAcceptanceId());
            vo.setAcceptanceResult(acceptance.getResult());
            vo.setRejectReason(acceptance.getRejectReason());
            vo.setAcceptancePhotos(acceptance.getAcceptancePhotos());
            vo.setAcceptedAt(acceptance.getAcceptedAt());
            vo.setAcceptorEmployeeId(acceptance.getAcceptorEmployeeId());
            vo.setAcceptorEmployeeName(employees.nameOf(acceptance.getAcceptorEmployeeId()));
            vo.setAcceptorRoleCode(acceptance.getAcceptorRoleCode());
            vo.setAcceptorRoleName(roleNameReader.resolve(roleNames, acceptance.getAcceptorRoleCode()));
        }
        // 小程序详情保持兼容；后台只读详情额外提供完整派工记录。
        if (applyVisibility) {
            vo.setDispatches(List.of());
            vo.setWorkWorkers(List.of());
        } else {
            List<SfStaskDispatchVo> dispatchVos = dispatches.stream()
                .map(row -> SfStaskWorkOrderAssembler.toDispatchVo(row, employees.employees()))
                .toList();
            vo.setDispatches(dispatchVos);
            vo.setWorkWorkers(dispatchVos.stream()
                .filter(row -> com.ym.agriculture.farmtask.workorder.model.constants.StaskDispatchStatus.ACCEPTED
                    .equals(row.getStatus()))
                .toList());
        }
        vo.setFlowLogs(toFlowLogVos(logs, employees.employees(), roleNames));
        if (applyVisibility) {
            fillOrderPermissions(order, vo, currentRole(employees));
            applyTechVisibility(vo, currentRole(employees));
        }
        if (includeVoice) {
            vo.setVoiceBroadcast(queryVoiceBroadcast(orderId));
        }
        return vo;
    }

    /**
     * 将不可覆盖的完工资料版本转换为详情中的审计历史，保持最新版本在列表首位。
     */
    private List<SfStaskCompletionHistoryVo> toCompletionHistoryVos(List<SfStaskCompletion> completions) {
        return completions.stream().map(row -> {
            SfStaskCompletionHistoryVo vo = new SfStaskCompletionHistoryVo();
            vo.setCompletionId(row.getCompletionId());
            vo.setWorkPhotos(row.getWorkPhotos());
            vo.setCompletionRemark(row.getCompletionRemark());
            vo.setCompletedAt(row.getCompletedAt());
            return vo;
        }).toList();
    }

    private SfStaskVoiceBroadcastVo queryVoiceBroadcast(Long orderId) {
        return voiceBroadcastService == null ? null : voiceBroadcastService.queryLatestForOrder(orderId);
    }

    private List<SfStaskPackageItemVo> buildPackageItemVos(List<SfStaskWorkOrderItem> items,
        List<SfStaskWorkOrderGreenhouse> greenhouses, List<SfStaskWorkOrder> splits,
        Map<String, SfFarmWorkAssignment> assignmentIndex, Map<Long, SysEmployeeVo> employees,
        Map<Long, List<SfStaskTaskMaterialVo>> taskMaterials) {
        Map<Long, List<SfStaskWorkOrderGreenhouse>> byItem = greenhouses.stream()
            .filter(row -> row.getItemId() != null)
            .collect(Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getItemId));
        List<SfStaskWorkOrderGreenhouse> legacy = greenhouses.stream()
            .filter(row -> row.getItemId() == null).toList();
        Map<Long, SfStaskWorkOrder> splitMap = splits.stream()
            .collect(Collectors.toMap(SfStaskWorkOrder::getOrderId, Function.identity(), (a, b) -> a));
        List<SfStaskPackageItemVo> result = new ArrayList<>(items.size());
        for (SfStaskWorkOrderItem item : items) {
            List<SfStaskWorkOrderGreenhouse> scoped = byItem.getOrDefault(item.getItemId(), legacy);
            SfStaskPackageItemVo vo = new SfStaskPackageItemVo();
            BeanUtil.copyProperties(item, vo);
            List<SfStaskGreenhouseBriefVo> briefs = SfStaskWorkOrderAssembler.toGreenhouseBriefs(scoped);
            vo.setGreenhouses(briefs);
            vo.setGreenhouseIds(briefs.stream().map(SfStaskGreenhouseBriefVo::getGreenhouseId).toList());
            vo.setMaterials(taskMaterials.getOrDefault(item.getWorkItemId(), List.of()));
            vo.setLeaderAssignMode(StaskLeaderAssignMode.normalize(item.getLeaderAssignMode()));
            vo.setManualLeaderName(item.getManualLeaderNameSnapshot());
            if (StringUtils.isNotBlank(vo.getManualLeaderName())) {
                vo.setManualLeaderNameSnapshotResourceId(item.getItemId());
            }
            Long leaderId = resolveItemLeader(item, scoped, splitMap, assignmentIndex);
            vo.setLeaderId(leaderId);
            boolean leaderNameFromSnapshot = StringUtils.isNotBlank(item.getLeaderNameSnapshot())
                && Objects.equals(leaderId, item.getLeaderIdSnapshot());
            if (leaderNameFromSnapshot) {
                vo.setLeaderName(item.getLeaderNameSnapshot());
                vo.setLeaderNameSnapshotResourceId(item.getItemId());
            } else {
                vo.setLeaderName(employeeName(employees, leaderId));
                vo.setLeaderNameEmployeeResourceId(leaderId);
            }
            if (StaskLeaderAssignMode.MANUAL.equals(vo.getLeaderAssignMode())) {
                vo.setManualLeaderId(leaderId == null ? item.getManualLeaderId() : leaderId);
                if (StringUtils.isBlank(vo.getManualLeaderName())) {
                    vo.setManualLeaderName(vo.getLeaderName());
                    if (leaderNameFromSnapshot) {
                        vo.setManualLeaderNameLeaderSnapshotResourceId(item.getItemId());
                    } else {
                        vo.setManualLeaderNameEmployeeResourceId(leaderId);
                    }
                }
            }
            result.add(vo);
        }
        return result;
    }

    private Map<Long, List<SfStaskTaskMaterialVo>> loadTaskMaterials(String tenantId, Long packageId) {
        if (taskMaterialMapper == null) {
            return Map.of();
        }
        Map<Long, List<SfStaskTaskMaterialVo>> result = new LinkedHashMap<>();
        List<TaskMaterial> rows = taskMaterialMapper.selectList(Wrappers.<TaskMaterial>lambdaQuery()
            .eq(TaskMaterial::getTenantId, tenantId)
            .eq(TaskMaterial::getTaskPackageId, packageId)
            .orderByAsc(TaskMaterial::getFarmItemId).orderByAsc(TaskMaterial::getMaterialId));
        for (TaskMaterial row : rows) {
            SfStaskTaskMaterialVo vo = new SfStaskTaskMaterialVo();
            vo.setInventoryMaterialId(row.getMaterialId());
            vo.setMaterialCode(row.getMaterialCodeSnapshot());
            vo.setMaterialName(row.getMaterialNameSnapshot());
            vo.setSpecification(row.getSpecificationSnapshot());
            vo.setUnit(row.getUnitSnapshot());
            vo.setQuantity(row.getTotalQuantity());
            result.computeIfAbsent(row.getFarmItemId(), ignored -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    private static Long resolveItemLeader(SfStaskWorkOrderItem item,
        List<SfStaskWorkOrderGreenhouse> greenhouses, Map<Long, SfStaskWorkOrder> splits,
        Map<String, SfFarmWorkAssignment> assignments) {
        if (item.getLeaderIdSnapshot() != null) {
            return item.getLeaderIdSnapshot();
        }
        if (StaskLeaderAssignMode.MANUAL.equals(StaskLeaderAssignMode.normalize(item.getLeaderAssignMode()))) {
            return item.getManualLeaderId();
        }
        LinkedHashSet<Long> leaders = greenhouses.stream().map(row -> {
                SfStaskWorkOrder split = splits.get(row.getOrderId());
                if (split != null) {
                    return split.getLeaderId();
                }
                SfFarmWorkAssignment assignment = assignments.get(
                    row.getGreenhouseId() + ":" + item.getWorkItemId());
                return assignment == null ? null : assignment.getLeaderId();
            }).filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return leaders.size() == 1 ? leaders.iterator().next() : null;
    }

    private Map<String, SfFarmWorkAssignment> loadAssignmentIndex(String tenantId,
        List<SfStaskWorkOrderItem> items, List<SfStaskWorkOrderGreenhouse> greenhouses) {
        List<Long> workItemIds = items.stream().map(SfStaskWorkOrderItem::getWorkItemId)
            .filter(Objects::nonNull).distinct().toList();
        List<Long> greenhouseIds = greenhouses.stream().map(SfStaskWorkOrderGreenhouse::getGreenhouseId)
            .filter(Objects::nonNull).distinct().toList();
        if (workItemIds.isEmpty() || greenhouseIds.isEmpty()) {
            return Map.of();
        }
        return assignmentMapper.selectByGreenhousesAndWorkItems(tenantId, greenhouseIds, workItemIds).stream()
            .collect(Collectors.toMap(row -> row.getGreenhouseId() + ":" + row.getWorkItemId(),
                Function.identity(), (a, b) -> a));
    }

    private void fillPackagePermissions(SfStaskTaskPackage taskPackage,
        SfStaskWorkOrderDetailVo vo, String roleCode) {
        boolean creator = Objects.equals(taskPackage.getCreatorEmployeeId(), LoginHelper.getUserId());
        boolean draft = StaskOrderStatus.DRAFT.equals(taskPackage.getStatus());
        boolean techRejected = StaskOrderStatus.TECH_REJECTED.equals(taskPackage.getStatus());
        boolean production = StaskCreatorRole.PRODUCTION_ADMIN.equals(taskPackage.getCreatorRoleCode());
        boolean technicianCreator = StaskCreatorRole.EXPERT.equals(taskPackage.getCreatorRoleCode());
        boolean technician = EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(roleCode);
        boolean pendingTechConfirm = StaskOrderStatus.PENDING_TECH_CONFIRM.equals(taskPackage.getStatus());
        boolean editable = creator && (draft || production && techRejected);
        vo.setEditable(editable);
        vo.setCanSaveDraft(creator && draft);
        vo.setCanSubmit(creator && (draft || production && techRejected));
        vo.setCanDelete(creator && (draft || techRejected));
        vo.setCanWithdraw(!technicianCreator && creator && pendingTechConfirm);
        vo.setCanVoid(!technicianCreator && creator && isVoidable(taskPackage.getStatus()));
        boolean canReview = production && technician && pendingTechConfirm;
        vo.setRelatedToCurrentUser(creator
            || Objects.equals(taskPackage.getHandlerTechnicianEmployeeId(), LoginHelper.getUserId()));
        vo.setCanTechConfirm(canReview);
        vo.setCanTechReject(canReview);
        vo.setCanDispatch(false);
        vo.setDispatchReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_DISPATCH_BEFORE_SPLIT));
        vo.setCanAcceptance(false);
        vo.setAcceptanceEditable(false);
        vo.setAcceptanceReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_ACCEPTANCE_FORBIDDEN));
        if (editable) {
            vo.setReadonlyReason(null);
        } else if (StaskOrderStatus.VOIDED.equals(taskPackage.getStatus())) {
            vo.setReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_CANCELLED_READ_ONLY));
        } else if (!creator && (draft || techRejected || technicianCreator)) {
            vo.setReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_CREATOR_ONLY_EDIT));
        } else if (techRejected) {
            vo.setReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_PRODUCTION_CREATOR_RESUBMIT_ONLY));
        } else {
            vo.setReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_NOT_EDITABLE));
        }
    }

    private void fillOrderPermissions(SfStaskWorkOrder order,
        SfStaskWorkOrderDetailVo vo, String roleCode) {
        boolean creator = Objects.equals(order.getCreatorEmployeeId(), LoginHelper.getUserId());
        boolean productionManager = EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN.equals(roleCode);
        boolean technician = EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(roleCode);
        boolean draft = StaskOrderStatus.DRAFT.equals(order.getStatus());
        boolean techRejected = StaskOrderStatus.TECH_REJECTED.equals(order.getStatus());
        boolean productionCreator = StaskCreatorRole.PRODUCTION_ADMIN.equals(order.getCreatorRoleCode());
        boolean technicianCreator = StaskCreatorRole.EXPERT.equals(order.getCreatorRoleCode());
        boolean editable = creator && (draft || productionCreator && techRejected);
        vo.setEditable(editable);
        vo.setCanSaveDraft(creator && draft);
        vo.setCanSubmit(creator && (draft || productionCreator && techRejected));
        vo.setCanDelete(creator && (draft || techRejected));
        if (technicianCreator) {
            vo.setCanWithdraw(creator && canTechnicianWithdrawPackage(order));
            vo.setCanVoid(creator && canTechnicianVoidOrder(order));
        } else if (productionCreator) {
            vo.setCanWithdraw(technician && canTechnicianWithdrawPackage(order));
            vo.setCanVoid(productionManager && creator && canManagerVoidOrder(order));
        } else {
            vo.setCanWithdraw(false);
            vo.setCanVoid(false);
        }
        boolean canReview = productionCreator && technician
            && StaskOrderStatus.PENDING_TECH_CONFIRM.equals(order.getStatus());
        vo.setCanTechConfirm(canReview);
        vo.setCanTechReject(canReview);
        vo.setPendingWorkerCount(0L);
        vo.setRejectedWorkerCount(0L);
        vo.setCanDispatch(false);
        vo.setCanDispatchWithoutWorkers(false);
        vo.setDispatchReadonlyReason(messages.message(
            com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_FEATURE_DEPRECATED));
        boolean pendingAcceptance = StaskOrderStatus.PENDING_ACCEPTANCE.equals(order.getStatus());
        boolean handlerTechnician = technician
            && Objects.equals(order.getHandlerTechnicianEmployeeId(), LoginHelper.getUserId());
        boolean acceptanceFinished = StaskOrderStatus.ACCEPTANCE_PASSED.equals(order.getStatus())
            || StaskOrderStatus.ACCEPTANCE_REJECTED.equals(order.getStatus());
        vo.setRelatedToCurrentUser(creator || handlerTechnician);
        vo.setCanAcceptance(pendingAcceptance && (productionManager || handlerTechnician));
        vo.setAcceptanceEditable(pendingAcceptance && (productionManager || handlerTechnician));
        if (pendingAcceptance && (productionManager || handlerTechnician)) {
            vo.setAcceptanceReadonlyReason(null);
        } else if (acceptanceFinished) {
            vo.setAcceptanceReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_READ_ONLY));
        } else if (pendingAcceptance && technician) {
            vo.setAcceptanceReadonlyReason(messages.message(
                com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_TECHNICIAN_NOT_HANDLER));
        } else if (pendingAcceptance) {
            vo.setAcceptanceReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_PRODUCTION_ADMIN_ACCEPTANCE_ONLY));
        } else {
            vo.setAcceptanceReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_STATUS_NOT_ACCEPTABLE));
        }
        if (editable) {
            vo.setReadonlyReason(null);
        } else if (StaskOrderStatus.VOIDED.equals(order.getStatus())) {
            vo.setReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_CANCELLED_READ_ONLY));
        } else if (!creator && (draft || techRejected || technicianCreator)) {
            vo.setReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_CREATOR_ONLY_EDIT));
        } else if (techRejected) {
            vo.setReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_PRODUCTION_CREATOR_RESUBMIT_ONLY));
        } else {
            vo.setReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_NOT_EDITABLE));
        }
    }

    private static boolean isVoidable(String status) {
        return StaskOrderStatus.PENDING_TECH_CONFIRM.equals(status)
            || StaskOrderStatus.PENDING_LEADER_ACCEPT.equals(status)
            || StaskOrderStatus.ASSIGN_COMPLETE.equals(status);
    }

    private boolean canTechnicianWithdrawPackage(SfStaskWorkOrder order) {
        if (order.getPackageId() == null) {
            return false;
        }
        SfStaskTaskPackage taskPackage = taskPackageMapper.selectById(order.getPackageId());
        if (taskPackage == null) {
            return false;
        }
        if (StaskCreatorRole.EXPERT.equals(taskPackage.getCreatorRoleCode())) {
            if (!Objects.equals(taskPackage.getCreatorEmployeeId(), LoginHelper.getUserId())) {
                return false;
            }
        } else if (!StaskCreatorRole.PRODUCTION_ADMIN.equals(taskPackage.getCreatorRoleCode())) {
            return false;
        }
        if (!StaskOrderStatus.PENDING_LEADER_ACCEPT.equals(taskPackage.getStatus())) {
            return false;
        }
        List<SfStaskWorkOrder> packageOrders = workOrderMapper.selectByPackageId(
            order.getTenantId(), order.getPackageId());
        return CollUtil.isNotEmpty(packageOrders) && packageOrders.stream()
            .allMatch(row -> StaskOrderStatus.PENDING_LEADER_ACCEPT.equals(row.getStatus()));
    }

    private boolean canTechnicianVoidOrder(SfStaskWorkOrder order) {
        return StaskOrderStatus.isCreatorVoidableBeforeExecution(order.getStatus())
            && !hasDispatchRecords(order);
    }

    private boolean canManagerVoidOrder(SfStaskWorkOrder order) {
        if (order.getPackageId() == null) {
            return false;
        }
        SfStaskTaskPackage taskPackage = taskPackageMapper.selectById(order.getPackageId());
        if (taskPackage == null
            || !StaskCreatorRole.PRODUCTION_ADMIN.equals(taskPackage.getCreatorRoleCode())
            || !Objects.equals(taskPackage.getCreatorEmployeeId(), LoginHelper.getUserId())) {
            return false;
        }
        return StaskOrderStatus.isCreatorVoidableBeforeExecution(order.getStatus())
            && !hasDispatchRecords(order);
    }

    private boolean hasDispatchRecords(SfStaskWorkOrder order) {
        return CollUtil.isNotEmpty(dispatchMapper.selectByOrderId(order.getTenantId(), order.getOrderId()));
    }

    /**
     * 历史派工详情判断。工人派工流程已废弃，始终不再加载派工详情。
     *
     * @deprecated 保留供历史调用方兼容，不应在新代码中使用。
     */
    @Deprecated
    private boolean isDispatchPipelineDetail(SfStaskWorkOrder order) {
        return false;
    }

    private void fillExpertPackageAcceptanceHint(SfStaskTaskPackage taskPackage,
        SfStaskWorkOrderDetailVo vo, List<SfStaskWorkOrder> splits, String roleCode) {
        if (!StaskCreatorRole.EXPERT.equals(taskPackage.getCreatorRoleCode())
            || !EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN.equals(roleCode)) {
            return;
        }
        long pendingAcceptanceCount = CollUtil.emptyIfNull(splits).stream()
            .filter(order -> StaskOrderStatus.PENDING_ACCEPTANCE.equals(order.getStatus()))
            .count();
        if (pendingAcceptanceCount > 1) {
            vo.setAcceptanceReadonlyReason(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_MULTIPLE_SPLITS_PENDING_ACCEPTANCE));
        }
    }

    private void fillTechReject(SfStaskWorkOrderDetailVo vo, SfStaskTaskPackage taskPackage,
        List<SfStaskFlowLog> logs, SfStaskEmployeeQueryContext employees) {
        if (!StaskOrderStatus.TECH_REJECTED.equals(taskPackage.getStatus())) {
            return;
        }
        SfStaskFlowLog reject = SfStaskWorkOrderAssembler.indexLatestFlowLogs(logs)
            .get(taskPackage.getPackageId() + ":" + StaskOrderEvent.TECH_REJECT);
        if (reject != null) {
            vo.setTechRejectLogId(reject.getLogId());
            vo.setTechRejectReason(reject.getRemark());
            vo.setTechRejectedAt(reject.getCreateTime());
            vo.setTechRejectOperatorEmployeeId(reject.getOperatorEmployeeId());
            vo.setTechRejectOperatorEmployeeName(employees.nameOf(reject.getOperatorEmployeeId()));
        }
    }

    private void attachSplitOrders(SfStaskWorkOrderDetailVo vo, List<SfStaskWorkOrder> splits,
        SfStaskEmployeeQueryContext employees, String roleCode) {
        if (CollUtil.isEmpty(splits)) {
            return;
        }
        Map<Long, List<SfStaskGreenhouseBriefVo>> greenhouseIndex = loadOrderGreenhouses(
            splits.get(0).getTenantId(), splits.stream().map(SfStaskWorkOrder::getOrderId).toList());
        Map<String, SfStaskLeaderLaborRecord> laborRecords = loadLaborRecords(
            splits.get(0).getTenantId(), splits);
        vo.setSplitOrders(splits.stream().map(row -> {
            SfStaskWorkOrderVo split = SfStaskWorkOrderAssembler.toWorkOrderVo(
                row, employees.employees(), greenhouseIndex);
            applyLaborRecord(row, split, laborRecords);
            split.setStatusLabel(SfStaskWorkOrderAssembler.splitStatusLabel(row.getStatus(), messages));
            boolean technicianHandler = EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(roleCode)
                && Objects.equals(row.getHandlerTechnicianEmployeeId(), LoginHelper.getUserId());
            boolean canAcceptance = StaskOrderStatus.PENDING_ACCEPTANCE.equals(row.getStatus())
                && (EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN.equals(roleCode) || technicianHandler);
            split.setRelatedToCurrentUser(Objects.equals(row.getCreatorEmployeeId(), LoginHelper.getUserId())
                || technicianHandler);
            if (canAcceptance) {
                split.setPrimaryAction(ACTION_ACCEPT);
            } else {
                split.setPrimaryAction(null);
            }
            return split;
        }).toList());
    }

    /**
     * 详情中的人数统一由组长计划日用工记录派生，旧工单人数列不再作为回退来源。
     */
    private void applyLaborRecord(SfStaskWorkOrder order, SfStaskWorkOrderVo target) {
        if (order.getLeaderId() == null || order.getPlanDate() == null) {
            target.setDailyLaborCount(null);
            target.setRequiredWorkerCount(null);
            target.setLaborRecordVersion(null);
            return;
        }
        SfStaskLeaderLaborRecord record = laborRecordMapper.selectByLeaderAndPlanDate(
            order.getTenantId(), order.getLeaderId(), toPlanDate(order));
        applyLaborSnapshot(target, record);
    }

    /**
     * 任务包详情中的拆分工单一次预取全部日用工记录，避免逐条查询。
     */
    private Map<String, SfStaskLeaderLaborRecord> loadLaborRecords(
        String tenantId, List<SfStaskWorkOrder> orders) {
        List<Long> leaderIds = orders.stream().map(SfStaskWorkOrder::getLeaderId)
            .filter(Objects::nonNull).distinct().toList();
        List<LocalDate> planDates = orders.stream().filter(row -> row.getPlanDate() != null)
            .map(SfStaskOrderDetailBuilder::toPlanDate).distinct().toList();
        if (leaderIds.isEmpty() || planDates.isEmpty()) {
            return Map.of();
        }
        Map<String, SfStaskLeaderLaborRecord> result = new HashMap<>();
        for (SfStaskLeaderLaborRecord record : CollUtil.emptyIfNull(
            laborRecordMapper.selectByLeadersAndPlanDates(tenantId, leaderIds, planDates))) {
            result.put(laborKey(record.getLeaderEmployeeId(), record.getPlanDate()), record);
        }
        return result;
    }

    private void applyLaborRecord(SfStaskWorkOrder order, SfStaskWorkOrderVo target,
        Map<String, SfStaskLeaderLaborRecord> laborRecords) {
        SfStaskLeaderLaborRecord record = order.getLeaderId() == null || order.getPlanDate() == null ? null
            : laborRecords.get(laborKey(order.getLeaderId(), toPlanDate(order)));
        applyLaborSnapshot(target, record);
    }

    private static void applyLaborSnapshot(SfStaskWorkOrderVo target, SfStaskLeaderLaborRecord record) {
        BigDecimal count = record == null ? null : record.getLaborCount();
        target.setDailyLaborCount(count);
        target.setRequiredWorkerCount(count == null ? null : count.doubleValue());
        target.setLaborRecordVersion(record == null ? null : record.getVersion());
    }

    private static LocalDate toPlanDate(SfStaskWorkOrder order) {
        return order.getPlanDate().toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
    }

    private static String laborKey(Long leaderId, LocalDate planDate) {
        return leaderId + "_" + planDate;
    }

    private List<SfStaskFlowLogVo> toFlowLogVos(List<SfStaskFlowLog> logs,
        Map<Long, SysEmployeeVo> employees, Map<String, String> roleNames) {
        return CollUtil.emptyIfNull(logs).stream()
            .map(log -> SfStaskWorkOrderAssembler.toFlowLogVo(
                log, employees, roleNameReader.resolve(roleNames, log.getOperatorRoleCode())))
            .toList();
    }

    private void applyTechVisibility(SfStaskWorkOrderDetailVo vo, String roleCode) {
        if (!EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(roleCode)
            && !EmployeeConstants.APP_ROLE_STASK_WORKER.equals(roleCode)) {
            return;
        }
        if (StaskCreatorRole.PRODUCTION_ADMIN.equals(vo.getCreatorRoleCode())) {
            vo.setShowTechSupplement(StringUtils.isNotBlank(vo.getTechInstruction())
                || StringUtils.isNotBlank(vo.getTechPhotos())
                || StringUtils.isNotBlank(vo.getOverallTechNote()));
            return;
        }
        if (StaskCreatorRole.EXPERT.equals(vo.getCreatorRoleCode())) {
            vo.setShowTechSupplement(false);
            if (StringUtils.isBlank(vo.getManagerRequirement())) {
                vo.setManagerRequirement(StringUtils.isNotBlank(vo.getTechInstruction())
                    ? vo.getTechInstruction() : vo.getOverallTechNote());
            }
            if (StringUtils.isBlank(vo.getManagerPhotos())) {
                vo.setManagerPhotos(vo.getTechPhotos());
            }
            vo.setTechInstruction(null);
            vo.setTechPhotos(null);
            vo.setOverallTechNote(null);
        }
    }

    private Map<Long, List<SfStaskGreenhouseBriefVo>> loadOrderGreenhouses(
        String tenantId, Collection<Long> orderIds) {
        Map<Long, List<SfStaskGreenhouseBriefVo>> result = new HashMap<>();
        if (CollUtil.isEmpty(orderIds)) {
            return result;
        }
        greenhouseMapper.selectByOrderIds(tenantId, orderIds).stream()
            .collect(Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getOrderId))
            .forEach((orderId, rows) -> result.put(
                orderId, SfStaskWorkOrderAssembler.toDistinctGreenhouseBriefs(rows)));
        return result;
    }

    private String currentRole(SfStaskEmployeeQueryContext employees) {
        Long userId = LoginHelper.getUserId();
        employees.preload(userId == null ? List.of() : List.of(userId));
        SysEmployeeVo employee = employees.find(userId);
        if (employee != null && StringUtils.isNotBlank(employee.getAppRoleCode())) {
            return employee.getAppRoleCode();
        }
        LoginUser loginUser = currentLoginUserOrNull();
        Set<String> roles = loginUser == null ? Set.of() : loginUser.getRolePermission();
        return CollUtil.isEmpty(roles) ? null : roles.iterator().next();
    }

    private static LoginUser currentLoginUserOrNull() {
        try {
            return LoginHelper.getLoginUser();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String employeeName(Map<Long, SysEmployeeVo> employees, Long employeeId) {
        SysEmployeeVo employee = employeeId == null ? null : employees.get(employeeId);
        return employee == null ? null : employee.getName();
    }
}
