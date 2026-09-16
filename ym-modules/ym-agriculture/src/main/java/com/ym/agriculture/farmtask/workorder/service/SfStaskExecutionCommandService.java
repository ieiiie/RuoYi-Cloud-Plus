package com.ym.agriculture.farmtask.workorder.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.system.api.model.LoginUser;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.shared.i18n.StaskErrorCodes;
import com.ym.agriculture.farmtask.i18n.StaskI18nResourceRegistrar;
import com.ym.agriculture.farmtask.inventory.service.TaskMaterialLifecycleCoordinator;
import com.ym.agriculture.farmtask.clocklocation.model.vo.SfStaskClockLocationVo;
import com.ym.agriculture.farmtask.clocklocation.service.ISfStaskClockLocationService;
import com.ym.agriculture.farmtask.leaderlabor.support.StaskClockEvidenceSupport;
import com.ym.agriculture.farmtask.worker.service.ISfStaskWorkerSkillService;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAcceptanceMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskClockRecordMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskCompletionMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskFlowLogMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAcceptanceBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskClockInBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskCompleteBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskAcceptanceResult;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskDispatchStatus;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskClockRecord;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskOrderStateMachine;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * stask 工单执行阶段命令服务。
 *
 * <p>集中处理到岗打卡、提交完工和生产管理员验收，并保证状态流转、执行记录与流转日志处于同一事务。</p>
 */
@RequiredArgsConstructor
@Service
public class SfStaskExecutionCommandService {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskDispatchMapper dispatchMapper;
    private final SfStaskClockRecordMapper clockRecordMapper;
    private final SfStaskCompletionMapper completionMapper;
    private final SfStaskAcceptanceMapper acceptanceMapper;
    private final SfStaskFlowLogMapper flowLogMapper;
    private final ISfStaskClockLocationService clockLocationService;
    private final ISfStaskWorkerSkillService workerSkillService;
    private final SfStaskOrderStateMachine stateMachine;
    private final SfStaskEmployeeAccessor employeeAccessor;
    @Autowired
    private StaskI18nResourceRegistrar i18nResourceRegistrar;
    @Autowired(required = false)
    private TaskMaterialLifecycleCoordinator taskMaterialCoordinator;

    /**
     * 记录任务组长到岗打卡并将工单流转为已到达。
     *
     * @param orderId 工单 ID
     * @param bo      打卡信息
     * @return 固定返回 1，表示打卡成功
     */
    @Transactional(rollbackFor = Exception.class)
    public int clockIn(Long orderId, SfStaskClockInBo bo) {
        if (bo == null || StringUtils.isBlank(bo.getClockType())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_CLOCK_TYPE_REQUIRED);
        }
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        if (!StaskOrderStatus.ASSIGN_COMPLETE.equals(order.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_CLOCK_IN_STATUS_INVALID);
        }
        Date now = new Date();
        SfStaskClockLocationVo location = clockLocationService.getByTenantId(order.getTenantId());
        StaskClockEvidenceSupport.ClockEvidence evidence = StaskClockEvidenceSupport.validate(
            bo.getClockType(), bo.getLongitude(), bo.getLatitude(), bo.getProofPhotos(), location, messages);
        SfStaskClockRecord record = new SfStaskClockRecord();
        record.setClockId(IdWorker.getId());
        record.setTenantId(order.getTenantId());
        record.setOrderId(orderId);
        record.setLeaderId(order.getLeaderId());
        record.setClockType(evidence.clockType());
        record.setLongitude(evidence.longitude());
        record.setLatitude(evidence.latitude());
        record.setDistanceMeters(evidence.distanceMeters());
        record.setProofPhotos(evidence.proofPhotos());
        record.setClockTime(now);
        record.setCreateTime(now);
        clockRecordMapper.insert(record);
        changeStatus(order, StaskOrderEvent.CLOCK_IN, messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_EXECUTION_LEADER_CLOCK_IN));
        if (taskMaterialCoordinator != null) {
            taskMaterialCoordinator.markArrivedForOrders(List.of(order));
        }
        register(order);
        return 1;
    }

    /**
     * 保存任务组长提交的完工资料并将工单流转为待验收。
     *
     * @param orderId 工单 ID
     * @param bo      完工信息
     * @return 固定返回 1，表示提交成功
     */
    @Transactional(rollbackFor = Exception.class)
    public int complete(Long orderId, SfStaskCompleteBo bo) {
        if (bo == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_COMPLETION_REQUIRED);
        }
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        if (!StaskOrderStatus.LEADER_ARRIVED.equals(order.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_ACCEPTANCE_STATUS_INVALID);
        }
        if (completionMapper.selectLatestByOrderId(order.getTenantId(), orderId) != null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_COMPLETION_ALREADY_SUBMITTED);
        }
        Date now = new Date();
        completionMapper.insert(newCompletion(order, bo, now));
        changeStatus(order, StaskOrderEvent.COMPLETE, messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_EXECUTION_LEADER_SUBMIT_COMPLETION));
        register(order);
        return 1;
    }

    /**
     * 将验收不通过的工单以新的完工资料版本重新置为待验收。
     *
     * @param orderId 工单 ID
     * @param bo      新的完工资料
     * @return 固定返回 1，表示重新申请成功
     */
    @Transactional(rollbackFor = Exception.class)
    public int reapplyAcceptance(Long orderId, SfStaskCompleteBo bo) {
        if (bo == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_COMPLETION_REQUIRED);
        }
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        if (!StaskOrderStatus.ACCEPTANCE_REJECTED.equals(order.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_REAPPLY_ACCEPTANCE_STATUS_INVALID);
        }
        completionMapper.insert(newCompletion(order, bo, new Date()));
        changeStatus(order, StaskOrderEvent.REAPPLY_ACCEPTANCE,
            messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_EXECUTION_LEADER_REAPPLY_ACCEPTANCE));
        register(order);
        return 1;
    }

    /**
     * 构造一条不可覆盖的完工资料版本，用于首次提交及重新申请验收。
     */
    private SfStaskCompletion newCompletion(SfStaskWorkOrder order, SfStaskCompleteBo bo, Date completedAt) {
        SfStaskCompletion completion = new SfStaskCompletion();
        completion.setCompletionId(IdWorker.getId());
        completion.setTenantId(order.getTenantId());
        completion.setOrderId(order.getOrderId());
        completion.setLeaderId(order.getLeaderId());
        completion.setWorkPhotos(normalizePhotoJson(bo.getWorkPhotos(), true,
            messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_WORKORDER_OPERATION_PHOTO)));
        completion.setCompletionRemark(bo.getCompletionRemark());
        completion.setCompletedAt(completedAt);
        completion.setCreateTime(completedAt);
        return completion;
    }

    /**
     * 由生产管理员提交验收结论。
     *
     * @param orderId 工单 ID
     * @param bo      验收信息
     * @return 固定返回 1，表示验收成功
     */
    @Transactional(rollbackFor = Exception.class)
    public int acceptance(Long orderId, SfStaskAcceptanceBo bo) {
        return acceptance(orderId, bo, EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN);
    }

    /**
     * 由经手技术员提交验收结论。
     *
     * @param orderId 工单 ID
     * @param bo      验收信息
     * @return 固定返回 1，表示验收成功
     */
    @Transactional(rollbackFor = Exception.class)
    public int technicianAcceptance(Long orderId, SfStaskAcceptanceBo bo) {
        return acceptance(orderId, bo, EmployeeConstants.APP_ROLE_STASK_EXPERT);
    }

    private int acceptance(Long orderId, SfStaskAcceptanceBo bo, String acceptorRoleCode) {
        if (bo == null || StringUtils.isBlank(bo.getResult())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_ACCEPTANCE_RESULT_REQUIRED);
        }
        SfStaskWorkOrder order = requireOrder(orderId);
        if (!hasCurrentRole(acceptorRoleCode)) {
            throw messages.stableException(StaskErrorCodes.ACTION_FORBIDDEN,
                com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_WORKORDER_PRODUCTION_ADMIN_ACCEPTANCE_ONLY);
        }
        if (EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(acceptorRoleCode)
            && !Objects.equals(order.getHandlerTechnicianEmployeeId(), LoginHelper.getUserId())) {
            throw messages.stableException(StaskErrorCodes.TECHNICIAN_NOT_HANDLER,
                com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_TECHNICIAN_NOT_HANDLER);
        }
        if (!StaskOrderStatus.PENDING_ACCEPTANCE.equals(order.getStatus())) {
            if (acceptanceMapper.selectLatestByOrderId(order.getTenantId(), orderId) != null) {
                throw messages.stableException(StaskErrorCodes.TASK_STATUS_CHANGED,
                    com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
            }
            throw messages.stableException(StaskErrorCodes.NOT_PENDING_ACCEPTANCE,
                com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_NOT_PENDING_ACCEPTANCE);
        }
        validateAcceptance(bo);

        Date now = new Date();
        SfStaskAcceptance acceptance = new SfStaskAcceptance();
        acceptance.setAcceptanceId(IdWorker.getId());
        acceptance.setTenantId(order.getTenantId());
        acceptance.setOrderId(orderId);
        acceptance.setAcceptorEmployeeId(LoginHelper.getUserId());
        acceptance.setAcceptorRoleCode(acceptorRoleCode);
        acceptance.setResult(bo.getResult());
        acceptance.setRejectReason(bo.getRejectReason());
        acceptance.setAcceptancePhotos(normalizePhotoJson(bo.getAcceptancePhotos(), false, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_WORKORDER_ACCEPTANCE_PHOTO)));
        acceptance.setAcceptedAt(now);
        acceptance.setCreateTime(now);
        changeStatus(order, StaskAcceptanceResult.PASS.equals(bo.getResult())
            ? StaskOrderEvent.ACCEPTANCE_PASS : StaskOrderEvent.ACCEPTANCE_REJECT, messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_EXECUTION_SUBMIT_ACCEPTANCE));
        acceptanceMapper.insert(acceptance);
        if (StaskAcceptanceResult.PASS.equals(bo.getResult())) {
            recordWorkerSkillCompletion(order, now);
        }
        register(order);
        return 1;
    }

    private void validateAcceptance(SfStaskAcceptanceBo bo) {
        if (!StaskAcceptanceResult.PASS.equals(bo.getResult())
            && !StaskAcceptanceResult.REJECT.equals(bo.getResult())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_ACCEPTANCE_RESULT_INVALID);
        }
        if (StaskAcceptanceResult.REJECT.equals(bo.getResult()) && StringUtils.isBlank(bo.getRejectReason())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_REJECT_REASON_REQUIRED);
        }
    }

    /**
     * 验收通过后一次查询已接受工人，并批量累计对应农事项目从事次数。
     */
    private void recordWorkerSkillCompletion(SfStaskWorkOrder order, Date completedAt) {
        if (order == null || order.getWorkItemId() == null) {
            return;
        }
        List<Long> workerIds = dispatchMapper.selectAcceptedByOrderId(order.getTenantId(), order.getOrderId()).stream()
            .map(SfStaskDispatch::getWorkerId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(workerIds)) {
            return;
        }
        workerSkillService.recordWorkCompletions(
            order.getTenantId(), workerIds, order.getWorkItemId(), completedAt);
    }

    private SfStaskWorkOrder requireOrder(Long orderId) {
        SfStaskWorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND);
        }
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isNotBlank(tenantId) && !Objects.equals(order.getTenantId(), tenantId)) {
            throw messages.stableException(StaskErrorCodes.CROSS_TENANT_FORBIDDEN,
                com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND);
        }
        return order;
    }

    private void register(SfStaskWorkOrder order) {
        if (i18nResourceRegistrar != null) {
            i18nResourceRegistrar.registerOrder(order.getTenantId(), order.getOrderId());
        }
    }

    private void ensureLeader(SfStaskWorkOrder order) {
        if (!Objects.equals(order.getLeaderId(), LoginHelper.getUserId())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_EXECUTION_LEADER_ONLY);
        }
    }

    private void changeStatus(SfStaskWorkOrder order, String event, String remark) {
        String oldStatus = order.getStatus();
        String newStatus = stateMachine.transit(oldStatus, event);
        order.setStatus(newStatus);
        ensureOptimisticUpdated(workOrderMapper.updateById(order));
        insertFlowLog(order, oldStatus, newStatus, event, remark);
    }

    private void insertFlowLog(SfStaskWorkOrder order, String fromStatus, String toStatus, String event, String remark) {
        SfStaskFlowLog log = new SfStaskFlowLog();
        log.setLogId(IdWorker.getId());
        log.setTenantId(order.getTenantId());
        log.setOrderId(order.getOrderId());
        log.setPackageId(order.getPackageId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setEvent(event);
        log.setOperatorEmployeeId(LoginHelper.getUserId());
        log.setOperatorRoleCode(currentRoleCode());
        log.setRemark(remark);
        log.setCreateTime(new Date());
        flowLogMapper.insert(log);
    }

    private void ensureOptimisticUpdated(int rows) {
        if (rows <= 0) {
            throw messages.stableException(StaskErrorCodes.TASK_STATUS_CHANGED,
                com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        }
    }

    private String normalizePhotoJson(String photoJson, boolean required, String fieldName) {
        if (StringUtils.isBlank(photoJson)) {
            if (required) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_UPLOAD_FIELD_REQUIRED,
                    fieldName);
            }
            return null;
        }
        try {
            if (JSON.parseArray(photoJson).isEmpty() && required) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_UPLOAD_FIELD_REQUIRED,
                    fieldName);
            }
            return photoJson;
        } catch (JSONException e) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_FIELD_JSON_ARRAY,
                fieldName);
        }
    }

    private void requireCurrentRole(String roleCode, String message) {
        if (!hasCurrentRole(roleCode)) {
            throw new ServiceException(message);
        }
    }

    private boolean hasCurrentRole(String roleCode) {
        String appRoleCode = currentEmployeeAppRoleCode();
        if (StringUtils.isNotBlank(appRoleCode)) {
            return Objects.equals(appRoleCode, roleCode);
        }
        LoginUser loginUser = currentLoginUserOrNull();
        Set<String> roles = loginUser != null ? loginUser.getRolePermission() : Set.of();
        return CollUtil.isNotEmpty(roles) && roles.contains(roleCode);
    }

    private String currentRoleCode() {
        String appRoleCode = currentEmployeeAppRoleCode();
        if (StringUtils.isNotBlank(appRoleCode)) {
            return appRoleCode;
        }
        LoginUser loginUser = currentLoginUserOrNull();
        Set<String> roles = loginUser != null ? loginUser.getRolePermission() : Set.of();
        return CollUtil.isNotEmpty(roles) ? roles.iterator().next() : null;
    }

    private String currentEmployeeAppRoleCode() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return null;
        }
        List<SysEmployeeVo> employees = employeeAccessor.queryBasicByIds(List.of(userId));
        if (employees == null) {
            employees = employeeAccessor.queryByIds(List.of(userId));
        }
        if (CollUtil.isEmpty(employees)) {
            return null;
        }
        return employees.get(0).getAppRoleCode();
    }

    private static LoginUser currentLoginUserOrNull() {
        try {
            return LoginHelper.getLoginUser();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
