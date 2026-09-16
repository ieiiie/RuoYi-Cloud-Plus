package com.ym.agriculture.farmtask.workorder.service.packagecmd;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.exception.ServiceException;
import com.ym.agriculture.farmtask.inventory.service.TaskMaterialLifecycleCoordinator;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAcceptanceMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskClockRecordMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskCompletionMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskFlowLogMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderItemMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskRejectBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskClockRecord;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 任务包撤回、删除和作废命令。
 *
 * <p>该组件在事务门面已经开启的事务中完成完整终止流程，自身不声明新事务。</p>
 */
@RequiredArgsConstructor
@Component
public class SfStaskPackageTerminationCommand {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskWorkOrderItemMapper itemMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfStaskDispatchMapper dispatchMapper;
    private final SfStaskClockRecordMapper clockRecordMapper;
    private final SfStaskCompletionMapper completionMapper;
    private final SfStaskAcceptanceMapper acceptanceMapper;
    private final SfStaskFlowLogMapper flowLogMapper;
    private final SfStaskStateFlowSupport stateFlow;
    private final SfStaskPackageCommandGuard guard;

    @Autowired(required = false)
    private TaskMaterialLifecycleCoordinator taskMaterialCoordinator;

    /**
     * 撤销并作废任务包。
     *
     * @param packageId 任务包ID
     * @param bo        撤销原因
     * @return 作废记录数
     */
    public int cancelPackage(Long packageId, SfStaskRejectBo bo) {
        guard.requireReason(bo, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_DISPATCH_CANCEL_REASON_PROMPT));
        return voidPackage(packageId, bo);
    }

    /**
     * 物理删除未拆单的草稿或技术退回任务包。
     *
     * @param packageId 任务包ID
     * @return 删除行数
     */
    public int deletePackage(Long packageId) {
        SfStaskTaskPackage header = guard.requirePackageHeader(packageId);
        guard.ensurePackageCreator(header);
        if (!StaskOrderStatus.DRAFT.equals(header.getStatus())
            && !StaskOrderStatus.TECH_REJECTED.equals(header.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_DELETE_STATUS_INVALID);
        }
        String tenantId = header.getTenantId();
        Long resolvedPackageId = header.getPackageId();
        List<SfStaskWorkOrder> packageOrders = workOrderMapper.selectByPackageId(tenantId, resolvedPackageId);
        if (CollUtil.isNotEmpty(packageOrders)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_SPLIT_EXISTS_DELETE_FORBIDDEN);
        }
        List<Long> orderIds = packageOrders.stream().map(SfStaskWorkOrder::getOrderId).toList();
        if (hasExecutionRecords(tenantId, orderIds)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_BUSINESS_RECORD_EXISTS_DELETE_FORBIDDEN);
        }
        flowLogMapper.delete(Wrappers.<SfStaskFlowLog>lambdaQuery()
            .eq(SfStaskFlowLog::getTenantId, tenantId)
            .eq(SfStaskFlowLog::getPackageId, resolvedPackageId));
        if (taskMaterialCoordinator != null) {
            taskMaterialCoordinator.deletePackageSnapshots(tenantId, resolvedPackageId);
        }
        greenhouseMapper.deleteByPackageId(tenantId, resolvedPackageId);
        itemMapper.deleteByPackageId(tenantId, resolvedPackageId);
        return taskPackageMapper.deleteById(resolvedPackageId);
    }

    /**
     * 创建人将待技术确认任务包撤回草稿。
     *
     * @param packageId 任务包ID
     * @param bo        撤回原因
     * @return 固定返回1
     */
    public int withdrawPackage(Long packageId, SfStaskRejectBo bo) {
        guard.requireReason(bo, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_WITHDRAW_REASON_PROMPT));
        SfStaskTaskPackage header = guard.requirePackageHeader(packageId);
        guard.ensurePackageCreator(header);
        if (!StaskOrderStatus.PENDING_TECH_CONFIRM.equals(header.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WITHDRAW_DRAFT_STATUS_INVALID);
        }
        stateFlow.changeStatus(header, StaskOrderEvent.WITHDRAW_TO_DRAFT, bo.getReason());
        return 1;
    }

    /**
     * 技术员撤回尚未被组长接单的任务包。
     *
     * @param packageId 任务包ID或拆分工单ID
     * @param bo        撤回原因
     * @return 固定返回1
     */
    public int technicianWithdrawPackage(Long packageId, SfStaskRejectBo bo) {
        guard.requireReason(bo, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_WITHDRAW_REASON_PROMPT));
        guard.requireCurrentRole(EmployeeConstants.APP_ROLE_STASK_EXPERT, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_TECHNICIAN_WITHDRAW_ONLY));
        SfStaskTaskPackage header = guard.requirePackageHeader(packageId);
        if (!StaskOrderStatus.PENDING_LEADER_ACCEPT.equals(header.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WITHDRAW_LEADER_STATUS_INVALID);
        }
        guard.ensurePackageOrdersAllPendingLeaderAccept(header.getTenantId(), header.getPackageId());
        if (taskMaterialCoordinator != null) {
            taskMaterialCoordinator.onPackageWithdrawn(header.getTenantId(), header.getPackageId());
        }
        removeSplitOrdersForWithdraw(header.getTenantId(), header.getPackageId());
        if (StaskCreatorRole.EXPERT.equals(header.getCreatorRoleCode())) {
            guard.ensurePackageCreator(header);
            stateFlow.changeStatus(header, StaskOrderEvent.WITHDRAW_TO_DRAFT, bo.getReason());
            return 1;
        }
        if (StaskCreatorRole.PRODUCTION_ADMIN.equals(header.getCreatorRoleCode())) {
            stateFlow.changeStatus(header, StaskOrderEvent.WITHDRAW_TO_TECH_CONFIRM, bo.getReason());
            return 1;
        }
        throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WITHDRAW_FORBIDDEN);
    }

    /**
     * 创建人作废单条拆分工单。
     *
     * @param orderId 工单ID
     * @param bo      作废原因
     * @return 固定返回1
     */
    public int voidOrder(Long orderId, SfStaskRejectBo bo) {
        guard.requireReason(bo, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_CANCEL_REASON_PROMPT));
        SfStaskWorkOrder order = guard.requireOrder(orderId);
        guard.ensureOrderCreator(order);
        guard.ensureVoidOrderCreatorRole(order);
        guard.ensureCreatorOrderVoidable(order);
        guard.ensureNoDispatchRecords(order.getTenantId(), order.getOrderId());
        if (taskMaterialCoordinator != null) {
            taskMaterialCoordinator.assertCanVoidOrders(List.of(order));
        }
        stateFlow.changeStatus(order, StaskOrderEvent.VOID, bo.getReason());
        syncPackageStatusAfterOrderVoid(order.getTenantId(), order.getPackageId(), bo.getReason());
        if (taskMaterialCoordinator != null) {
            taskMaterialCoordinator.onOrdersVoided(List.of(order));
        }
        return 1;
    }

    /**
     * 生产管理员按任务ID作废拆分工单或未拆分任务包。
     *
     * @param id 任务包ID或拆分工单ID
     * @param bo 作废原因
     * @return 作废记录数
     */
    public int voidManagerTask(Long id, SfStaskRejectBo bo) {
        guard.requireReason(bo, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_CANCEL_REASON_PROMPT));
        guard.requireCurrentRole(EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_PRODUCTION_ADMIN_CANCEL_ONLY));
        SfStaskWorkOrder order = workOrderMapper.selectById(id);
        if (order != null && guard.matchPackageTenant(order.getTenantId())) {
            return voidOrder(id, bo);
        }
        SfStaskTaskPackage header = guard.requirePackageHeader(id);
        guard.ensurePackageCreator(header);
        List<SfStaskWorkOrder> packageOrders = workOrderMapper.selectByPackageId(
            header.getTenantId(), header.getPackageId());
        if (CollUtil.isNotEmpty(packageOrders)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_SPLIT_EXISTS_CANCEL_SINGLE);
        }
        return voidPackage(id, bo);
    }

    /**
     * 作废任务包及其全部尚未执行的拆分工单。
     *
     * @param packageId 任务包ID
     * @param bo        作废原因
     * @return 作废记录数
     */
    public int voidPackage(Long packageId, SfStaskRejectBo bo) {
        guard.requireReason(bo, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_CANCEL_REASON_PROMPT));
        SfStaskTaskPackage header = guard.requirePackageHeader(packageId);
        guard.ensurePackageCreator(header);
        List<SfStaskWorkOrder> packageOrders = workOrderMapper.selectByPackageId(
            header.getTenantId(), header.getPackageId());
        guard.ensureVoidablePackage(header, packageOrders);

        if (taskMaterialCoordinator != null) {
            taskMaterialCoordinator.assertCanVoidPackage(header.getTenantId(), header.getPackageId());
        }
        stateFlow.changeStatus(header, StaskOrderEvent.VOID, bo.getReason());
        if (CollUtil.isEmpty(packageOrders)) {
            if (taskMaterialCoordinator != null) {
                taskMaterialCoordinator.onPackageVoided(header.getTenantId(), header.getPackageId());
            }
            return 1;
        }
        List<SfStaskFlowLog> logs = prepareOrderVoidLogs(packageOrders, bo.getReason());
        if (!workOrderMapper.updateBatchById(packageOrders)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        }
        flowLogMapper.insertBatch(logs);
        if (taskMaterialCoordinator != null) {
            taskMaterialCoordinator.onPackageVoided(header.getTenantId(), header.getPackageId());
        }
        return packageOrders.size() + 1;
    }

    private List<SfStaskFlowLog> prepareOrderVoidLogs(List<SfStaskWorkOrder> packageOrders, String reason) {
        List<SfStaskFlowLog> logs = new ArrayList<>(packageOrders.size());
        String operatorRoleCode = guard.currentRoleCode();
        for (SfStaskWorkOrder order : packageOrders) {
            logs.add(stateFlow.prepareTransition(
                order, StaskOrderEvent.VOID, reason, operatorRoleCode));
        }
        return logs;
    }

    private boolean hasExecutionRecords(String tenantId, List<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return false;
        }
        return dispatchMapper.selectCount(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .in(SfStaskDispatch::getOrderId, orderIds)) > 0
            || clockRecordMapper.selectCount(Wrappers.<SfStaskClockRecord>lambdaQuery()
            .eq(SfStaskClockRecord::getTenantId, tenantId)
            .in(SfStaskClockRecord::getOrderId, orderIds)) > 0
            || completionMapper.selectCount(Wrappers.<SfStaskCompletion>lambdaQuery()
            .eq(SfStaskCompletion::getTenantId, tenantId)
            .in(SfStaskCompletion::getOrderId, orderIds)) > 0
            || acceptanceMapper.selectCount(Wrappers.<SfStaskAcceptance>lambdaQuery()
            .eq(SfStaskAcceptance::getTenantId, tenantId)
            .in(SfStaskAcceptance::getOrderId, orderIds)) > 0;
    }

    private void removeSplitOrdersForWithdraw(String tenantId, Long packageId) {
        List<SfStaskWorkOrder> packageOrders = workOrderMapper.selectByPackageId(tenantId, packageId);
        if (CollUtil.isEmpty(packageOrders)) {
            return;
        }
        List<Long> orderIds = packageOrders.stream()
            .map(SfStaskWorkOrder::getOrderId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        flowLogMapper.delete(Wrappers.<SfStaskFlowLog>lambdaQuery()
            .eq(SfStaskFlowLog::getTenantId, tenantId)
            .in(SfStaskFlowLog::getOrderId, orderIds));
        workOrderMapper.delete(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getPackageId, packageId));
        greenhouseMapper.clearOrderIdByPackageId(tenantId, packageId);
    }

    private void syncPackageStatusAfterOrderVoid(String tenantId, Long packageId, String reason) {
        if (packageId == null) {
            return;
        }
        SfStaskTaskPackage taskPackage = taskPackageMapper.selectById(packageId);
        if (taskPackage == null || StaskOrderStatus.VOIDED.equals(taskPackage.getStatus())) {
            return;
        }
        List<SfStaskWorkOrder> packageOrders = workOrderMapper.selectByPackageId(tenantId, packageId);
        if (CollUtil.isNotEmpty(packageOrders) && packageOrders.stream()
            .allMatch(order -> StaskOrderStatus.VOIDED.equals(order.getStatus()))) {
            stateFlow.changeStatus(taskPackage, StaskOrderEvent.VOID, reason);
        }
    }
}
