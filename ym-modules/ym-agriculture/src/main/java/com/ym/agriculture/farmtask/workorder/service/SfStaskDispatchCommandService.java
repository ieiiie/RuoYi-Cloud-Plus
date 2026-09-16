package com.ym.agriculture.farmtask.workorder.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.system.api.model.LoginUser;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.BilingualContent;
import com.ym.agriculture.shared.i18n.StaskBilingualMessageFormatter;
import com.ym.agriculture.farmtask.i18n.StaskI18nResourceRegistrar;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.farmtask.worker.model.entity.SfStaskWorkerSkill;
import com.ym.agriculture.farmtask.worker.service.ISfStaskWorkerSkillService;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskFlowLogMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdjustWorkersBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskCancelDispatchBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskDispatchBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskLeaderAcceptBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskRejectBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskReplaceWorkerBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskDispatchStatus;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerAcceptVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskNotifyService;
import com.ym.agriculture.farmtask.workorder.support.SfStaskOrderStateMachine;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.voice.model.vo.SfStaskVoiceBroadcastVo;
import com.ym.agriculture.farmtask.voice.service.ISfStaskVoiceBroadcastService;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeLeaderOptionVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.agriculture.farmtask.employee.service.IEmployeeAppRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * stask 工单派工命令服务。
 *
 * <p>集中处理组长接单、工人邀请、派工调整和工人响应，确保派工明细、工单状态及流转日志处于同一事务。</p>
 */
@RequiredArgsConstructor
@Service
public class SfStaskDispatchCommandService {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskDispatchMapper dispatchMapper;
    private final SfStaskFlowLogMapper flowLogMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final IEmployeeAppRoleService employeeAppRoleService;
    private final ISfStaskWorkerSkillService workerSkillService;
    private final SfStaskOrderStateMachine stateMachine;
    private final SfStaskNotifyService notifyService;
    private final StaskBilingualMessageFormatter bilingualMessageFormatter;
    @Autowired
    private StaskI18nResourceRegistrar i18nResourceRegistrar;

    @Autowired(required = false)
    private ISfStaskVoiceBroadcastService voiceBroadcastService;

    /**
     * 任务组长接单并设置工人数量。
     *
     * @param orderId 工单 ID
     * @param bo      接单信息
     * @return 固定返回 1，表示接单成功
     */
    @Transactional(rollbackFor = Exception.class)
    public int leaderAccept(Long orderId, SfStaskLeaderAcceptBo bo) {
        if (bo == null || bo.getRequiredWorkerCount() == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_REQUIRED_WORKER_COUNT_REQUIRED);
        }
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        order.setRequiredWorkerCount(bo.getRequiredWorkerCount());
        updateOrderWithLock(order);
        changeStatus(order, StaskOrderEvent.LEADER_ACCEPT,
            messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_DISPATCH_LEADER_ACCEPT));
        // 接单即派工完成；保留既有任务语音播报生成，不依赖工人接受派工。
        preGenerateVoiceBroadcast(order.getOrderId());
        register(order);
        return 1;
    }

    /**
     * 批量邀请工人参与指定工单。
     *
     * @param orderId 工单 ID
     * @param bo      派工信息
     * @return 新增派工记录数量
     */
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public int dispatch(Long orderId, SfStaskDispatchBo bo) {
        throwWorkerFeatureDeprecated();
        if (bo == null || CollUtil.isEmpty(bo.getWorkerIds())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_WORKER_REQUIRED);
        }
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        if (!StaskOrderStatus.PENDING_LEADER_ASSIGN.equals(order.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_ORDER_STATUS_INVALID);
        }
        List<Long> workerIds = distinctIds(bo.getWorkerIds(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_DISPATCH_WORKER_REQUIRED));
        ensureDispatchInviteCapacity(order, workerIds.size());
        ensureWorkers(workerIds);
        ensureNoActiveInvites(order, workerIds, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_DISPATCH_WORKER_ALREADY_INVITED));

        Date now = new Date();
        Long operatorId = LoginHelper.getUserId();
        List<SfStaskDispatch> dispatches = workerIds.stream()
            .map(workerId -> newPendingDispatch(order, workerId, now, operatorId))
            .toList();
        int rows = dispatchMapper.insertBatch(dispatches) ? dispatches.size() : 0;
        register(order);
        return rows;
    }

    /**
     * 将待派工工单设置为无需工人并完成派工。
     *
     * @param orderId 工单 ID
     * @return 固定返回 1，表示操作成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public int completeDispatchWithoutWorkers(Long orderId) {
        throwWorkerFeatureDeprecated();
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        if (!StaskOrderStatus.PENDING_LEADER_ASSIGN.equals(order.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_ORDER_STATUS_INVALID);
        }
        long selectedCount = dispatchMapper.countByStatuses(order.getTenantId(), orderId,
            List.of(StaskDispatchStatus.PENDING, StaskDispatchStatus.ACCEPTED));
        if (selectedCount > 0) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_WORKERS_EXIST_BEFORE_NO_WORKER);
        }
        order.setRequiredWorkerCount(0D);
        order.setAcceptedWorkerCount(0);
        order.setUpdateBy(LoginHelper.getUserId());
        order.setUpdateTime(java.time.LocalDateTime.now());
        updateOrderWithLock(order);
        changeStatus(order, StaskOrderEvent.DISPATCH_READY, messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_DISPATCH_NO_WORKER_COMPLETE));
        register(order);
        return 1;
    }

    /**
     * 查询与工单农事项匹配的可选工人并按技能排序。
     *
     * @param orderId 工单 ID
     * @param keyword 姓名或手机号关键字
     * @param gender  性别筛选值
     * @param ageMin  最小年龄（周岁）
     * @param ageMax  最大年龄（周岁）
     * @return 推荐工人列表
     */
    @Deprecated
    public List<SysEmployeeLeaderOptionVo> recommendWorkers(
        Long orderId, String keyword, String gender, Integer ageMin, Integer ageMax) {
        throwWorkerFeatureDeprecated();
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        Set<Long> selectedWorkerIds = dispatchMapper.selectByOrderId(order.getTenantId(), orderId).stream()
            .filter(row -> !StaskDispatchStatus.CANCELLED.equals(row.getStatus()))
            .map(SfStaskDispatch::getWorkerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<SysEmployeeLeaderOptionVo> workers = employeeAccessor
            .queryOptionsByRole(EmployeeConstants.APP_ROLE_STASK_WORKER, keyword, selectedWorkerIds)
            .stream()
            .filter(worker -> StringUtils.isBlank(gender) || Objects.equals(worker.getGender(), gender))
            .filter(worker -> matchAge(worker, ageMin, ageMax))
            .toList();
        Map<Long, SfStaskWorkerSkill> skillMap = workerSkillService.queryBestSkillMap(order.getTenantId(),
            workers.stream().map(SysEmployeeLeaderOptionVo::getEmployeeId).toList(), order.getWorkItemId());
        Map<String, String> roleNames = employeeAppRoleService.getRoleNameMap(workers.stream()
            .map(SysEmployeeLeaderOptionVo::getAppRoleCode)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList());
        roleNames = roleNames == null ? Map.of() : roleNames;
        for (SysEmployeeLeaderOptionVo worker : workers) {
            worker.setAppRoleName(SfStaskWorkOrderAssembler.localizedRoleName(
                worker.getAppRoleCode(), roleNames.get(worker.getAppRoleCode()), messages));
            worker.setAge(calcAge(worker.getBirthDate()));
            SfStaskWorkerSkill skill = skillMap.get(worker.getEmployeeId());
            if (skill != null) {
                worker.setSkillLevel(skill.getSkillLevel());
                worker.setWorkCount(skill.getWorkCount());
                worker.setLastWorkDate(skill.getLastWorkDate());
                worker.setAverageScore(skill.getAverageScore());
            }
        }
        return workers.stream()
            .sorted(Comparator.comparingInt((SysEmployeeLeaderOptionVo worker) -> skillLevelRank(worker.getSkillLevel()))
                .thenComparing(worker -> worker.getWorkCount() == null ? 0 : worker.getWorkCount(),
                    Comparator.reverseOrder())
                .thenComparing(SysEmployeeLeaderOptionVo::getLastWorkDate,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SysEmployeeLeaderOptionVo::getName, Comparator.nullsLast(String::compareTo)))
            .toList();
    }

    /**
     * 调整工单需求工人数并按当前已接受人数同步派工状态。
     *
     * @param orderId 工单 ID
     * @param bo      调整信息
     * @return 更新行数
     */
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public int adjustRequiredWorkers(Long orderId, SfStaskAdjustWorkersBo bo) {
        throwWorkerFeatureDeprecated();
        if (bo == null || bo.getRequiredWorkerCount() == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_REQUIRED_WORKER_COUNT_REQUIRED);
        }
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        if (!StaskOrderStatus.PENDING_LEADER_ASSIGN.equals(order.getStatus())
            && !StaskOrderStatus.ASSIGN_COMPLETE.equals(order.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_REQUIRED_WORKER_COUNT_IMMUTABLE);
        }
        long selectedCount = dispatchMapper.countByStatuses(order.getTenantId(), orderId,
            List.of(StaskDispatchStatus.PENDING, StaskDispatchStatus.ACCEPTED));
        if (Double.compare(bo.getRequiredWorkerCount(), selectedCount) < 0) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_REQUIRED_WORKER_COUNT_BELOW_ACTIVE);
        }
        order.setRequiredWorkerCount(bo.getRequiredWorkerCount());
        order.setUpdateBy(LoginHelper.getUserId());
        order.setUpdateTime(java.time.LocalDateTime.now());
        int rows = updateOrderWithLock(order);
        reopenOrCompleteDispatch(order);
        register(order);
        return rows;
    }

    /**
     * 撤销待确认或已接受的派工记录。
     *
     * @param orderId    工单 ID
     * @param dispatchId 派工记录 ID
     * @param bo         撤销信息
     * @return 更新行数
     */
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public int cancelDispatch(Long orderId, Long dispatchId, SfStaskCancelDispatchBo bo) {
        throwWorkerFeatureDeprecated();
        if (bo == null || StringUtils.isBlank(bo.getReason())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_CANCEL_REASON_REQUIRED);
        }
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        if (!StaskOrderStatus.PENDING_LEADER_ASSIGN.equals(order.getStatus())
            && !StaskOrderStatus.ASSIGN_COMPLETE.equals(order.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_ORDER_NOT_CANCELLABLE);
        }
        SfStaskDispatch dispatch = requireOrderDispatch(order, dispatchId);
        if (!StaskDispatchStatus.PENDING.equals(dispatch.getStatus())
            && !StaskDispatchStatus.ACCEPTED.equals(dispatch.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_RECORD_NOT_CANCELLABLE);
        }
        String oldStatus = dispatch.getStatus();
        dispatch.setStatus(StaskDispatchStatus.CANCELLED);
        dispatch.setRejectReason(bo.getReason());
        dispatch.setCancelledAt(new Date());
        dispatch.setUpdateBy(LoginHelper.getUserId());
        dispatch.setUpdateTime(java.time.LocalDateTime.now());
        int rows = updateDispatchWithLock(dispatch);
        syncAcceptedWorkerCount(order);
        reopenOrCompleteDispatch(order);
        notifyService.notify(
            StaskDispatchStatus.ACCEPTED.equals(oldStatus) ? "WORKER_NEGOTIATE_CANCEL" : "WORKER_INVITE_CANCEL",
            dispatch.getWorkerId(), bilingualWithDispatchReason(dispatch, bo.getReason()));
        register(order);
        return rows;
    }

    /**
     * 催促工人处理待确认邀请。
     *
     * @param orderId    工单 ID
     * @param dispatchId 派工记录 ID
     * @return 固定返回 1，表示提醒已触发
     */
    @Deprecated
    public int remindDispatch(Long orderId, Long dispatchId) {
        throwWorkerFeatureDeprecated();
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        if (!StaskOrderStatus.PENDING_LEADER_ASSIGN.equals(order.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_ORDER_STATUS_INVALID);
        }
        SfStaskDispatch dispatch = requireOrderDispatch(order, dispatchId);
        if (!StaskDispatchStatus.PENDING.equals(dispatch.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_ONLY_PENDING_CAN_REMIND);
        }
        notifyService.notify("WORKER_INVITE_REMIND", dispatch.getWorkerId(),
            bilingual(StaskMessageKeys.NOTIFY_WORKER_REMIND));
        return 1;
    }

    /**
     * 对已拒绝或已撤销的工人再次发起邀请。
     *
     * @param orderId    工单 ID
     * @param dispatchId 原派工记录 ID
     * @return 新增派工记录数
     */
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public int reinviteDispatch(Long orderId, Long dispatchId) {
        throwWorkerFeatureDeprecated();
        SfStaskWorkOrder order = requireAssignableOrder(orderId);
        SfStaskDispatch dispatch = requireOrderDispatch(order, dispatchId);
        if (!StaskDispatchStatus.REJECTED.equals(dispatch.getStatus())
            && !StaskDispatchStatus.CANCELLED.equals(dispatch.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_ONLY_REJECTED_CANCELLED_REINVITE);
        }
        int rows = createPendingDispatch(order, dispatch.getWorkerId(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_DISPATCH_WORKER_ALREADY_INVITED));
        register(order);
        return rows;
    }

    /**
     * 将已拒绝或已撤销的邀请替换为新工人。
     *
     * @param orderId    工单 ID
     * @param dispatchId 原派工记录 ID
     * @param bo         换人信息
     * @return 新增派工记录数
     */
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public int replaceDispatch(Long orderId, Long dispatchId, SfStaskReplaceWorkerBo bo) {
        throwWorkerFeatureDeprecated();
        if (bo == null || bo.getNewWorkerId() == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_NEW_WORKER_REQUIRED);
        }
        SfStaskWorkOrder order = requireAssignableOrder(orderId);
        SfStaskDispatch dispatch = requireOrderDispatch(order, dispatchId);
        if (!StaskDispatchStatus.REJECTED.equals(dispatch.getStatus())
            && !StaskDispatchStatus.CANCELLED.equals(dispatch.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_ONLY_REJECTED_CANCELLED_REPLACE);
        }
        int rows = createPendingDispatch(order, bo.getNewWorkerId(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_DISPATCH_NEW_WORKER_ALREADY_INVITED));
        register(order);
        return rows;
    }

    /**
     * 工人接受派工邀请并同步工单已接受人数。
     *
     * @param dispatchId 派工记录 ID
     * @return 接受结果及语音播报信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public SfStaskWorkerAcceptVo workerAccept(Long dispatchId) {
        throwWorkerFeatureDeprecated();
        SfStaskDispatch dispatch = requireDispatch(dispatchId);
        SfStaskWorkOrder order = requireOrder(dispatch.getOrderId());
        Double requiredWorkerCount = order.getRequiredWorkerCount();
        if (requiredWorkerCount != null && Double.compare(requiredWorkerCount, 0D) > 0) {
            long acceptedCount = dispatchMapper.countAccepted(order.getTenantId(), order.getOrderId());
            if (Double.compare(acceptedCount, requiredWorkerCount) >= 0) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_CAPACITY_FULL);
            }
        }
        dispatch.setStatus(StaskDispatchStatus.ACCEPTED);
        dispatch.setRespondedAt(new Date());
        int rows = updateDispatchWithLock(dispatch);
        long acceptedCount = dispatchMapper.countAccepted(order.getTenantId(), order.getOrderId());
        order.setAcceptedWorkerCount((int) acceptedCount);
        updateOrderWithLock(order);
        if (order.getRequiredWorkerCount() != null
            && Double.compare(acceptedCount, order.getRequiredWorkerCount()) >= 0
            && StaskOrderStatus.PENDING_LEADER_ASSIGN.equals(order.getStatus())) {
            changeStatus(order, StaskOrderEvent.DISPATCH_READY, messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_DISPATCH_WORKER_COUNT_MET));
        }
        SfStaskWorkerAcceptVo vo = new SfStaskWorkerAcceptVo();
        vo.setDispatchId(dispatch.getDispatchId());
        vo.setOrderId(order.getOrderId());
        vo.setAccepted(rows > 0);
        vo.setVoiceBroadcast(latestVoiceBroadcast(order.getOrderId()));
        register(order);
        return vo;
    }

    /**
     * 工人拒绝派工邀请。
     *
     * @param dispatchId 派工记录 ID
     * @param bo         拒绝信息
     * @return 更新行数
     */
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public int workerReject(Long dispatchId, SfStaskRejectBo bo) {
        throwWorkerFeatureDeprecated();
        if (bo == null || StringUtils.isBlank(bo.getReason())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_REJECT_REASON_REQUIRED);
        }
        SfStaskDispatch dispatch = requireDispatch(dispatchId);
        dispatch.setStatus(StaskDispatchStatus.REJECTED);
        dispatch.setRejectReason(bo.getReason());
        dispatch.setRespondedAt(new Date());
        int rows = updateDispatchWithLock(dispatch);
        if (i18nResourceRegistrar != null) {
            i18nResourceRegistrar.registerOrder(dispatch.getTenantId(), dispatch.getOrderId());
        }
        return rows;
    }

    private SfStaskWorkOrder requireOrder(Long orderId) {
        SfStaskWorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND);
        }
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isNotBlank(tenantId) && !Objects.equals(order.getTenantId(), tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 保留旧派工服务签名，防止非 HTTP 调用绕过已废弃的工人派工功能。
     */
    private void throwWorkerFeatureDeprecated() {
        throw messages.exception(StaskMessageKeys.ERROR_DISPATCH_FEATURE_DEPRECATED);
    }

    private void register(SfStaskWorkOrder order) {
        if (i18nResourceRegistrar != null) {
            i18nResourceRegistrar.registerOrder(order.getTenantId(), order.getOrderId());
        }
    }

    private BilingualContent bilingual(String key, Object... args) {
        return bilingualMessageFormatter.format(key, args);
    }

    private BilingualContent bilingualWithDispatchReason(SfStaskDispatch dispatch, String reason) {
        return bilingualMessageFormatter.formatWithResource(dispatch.getTenantId(),
            StaskMessageKeys.NOTIFY_WORKER_CANCELLED, new I18nTextSource(I18nResourceType.STASK_DISPATCH,
                dispatch.getDispatchId(), "rejectReason", reason));
    }

    private SfStaskWorkOrder requireAssignableOrder(Long orderId) {
        SfStaskWorkOrder order = requireOrder(orderId);
        ensureLeader(order);
        if (!StaskOrderStatus.PENDING_LEADER_ASSIGN.equals(order.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_ORDER_STATUS_INVALID);
        }
        return order;
    }

    private SfStaskDispatch requireOrderDispatch(SfStaskWorkOrder order, Long dispatchId) {
        SfStaskDispatch dispatch = dispatchMapper.selectByOrderAndDispatchId(
            order.getTenantId(), order.getOrderId(), dispatchId);
        if (dispatch == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_RECORD_NOT_FOUND);
        }
        return dispatch;
    }

    private SfStaskDispatch requireDispatch(Long dispatchId) {
        SfStaskDispatch dispatch = dispatchMapper.selectById(dispatchId);
        if (dispatch == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_RECORD_NOT_FOUND);
        }
        if (!Objects.equals(dispatch.getWorkerId(), LoginHelper.getUserId())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_TASK_NOT_FOUND_OR_FORBIDDEN);
        }
        if (!StaskDispatchStatus.PENDING.equals(dispatch.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_RECORD_NOT_RESPONSIVE);
        }
        return dispatch;
    }

    private int createPendingDispatch(SfStaskWorkOrder order, Long workerId, String duplicateMessage) {
        ensureWorker(workerId);
        if (dispatchMapper.existsActiveInvite(order.getTenantId(), order.getOrderId(), workerId)) {
            throw new ServiceException(duplicateMessage);
        }
        ensureDispatchInviteCapacity(order, 1);
        SfStaskDispatch dispatch = newPendingDispatch(order, workerId, new Date(), LoginHelper.getUserId());
        return dispatchMapper.insert(dispatch);
    }

    private static SfStaskDispatch newPendingDispatch(
        SfStaskWorkOrder order, Long workerId, Date now, Long operatorId) {
        SfStaskDispatch dispatch = new SfStaskDispatch();
        dispatch.setDispatchId(IdWorker.getId());
        dispatch.setTenantId(order.getTenantId());
        dispatch.setOrderId(order.getOrderId());
        dispatch.setLeaderId(order.getLeaderId());
        dispatch.setWorkerId(workerId);
        dispatch.setStatus(StaskDispatchStatus.PENDING);
        dispatch.setInvitedAt(now);
        dispatch.setVersion(0);
        dispatch.setCreateBy(operatorId);
        dispatch.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        dispatch.setUpdateBy(operatorId);
        dispatch.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        return dispatch;
    }

    private void ensureWorker(Long workerId) {
        ensureWorkers(List.of(workerId));
    }

    private void ensureWorkers(Collection<Long> workerIds) {
        List<SysEmployeeVo> employees = employeeAccessor.queryBasicByIds(workerIds);
        if (employees == null) {
            employees = employeeAccessor.queryByIds(workerIds);
        }
        Map<Long, SysEmployeeVo> employeeMap = CollUtil.emptyIfNull(employees).stream()
            .filter(employee -> employee != null && employee.getEmployeeId() != null)
            .collect(Collectors.toMap(SysEmployeeVo::getEmployeeId, Function.identity(), (first, ignored) -> first));
        for (Long workerId : workerIds) {
            SysEmployeeVo employee = employeeMap.get(workerId);
            if (employee == null || !EmployeeConstants.APP_ROLE_STASK_WORKER.equals(employee.getAppRoleCode())) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_WORKER_INVALID);
            }
        }
    }

    private void ensureNoActiveInvites(
        SfStaskWorkOrder order, Collection<Long> workerIds, String duplicateMessage) {
        Set<Long> activeWorkerIds = dispatchMapper.selectByOrderId(order.getTenantId(), order.getOrderId()).stream()
            .filter(row -> StaskDispatchStatus.PENDING.equals(row.getStatus())
                || StaskDispatchStatus.ACCEPTED.equals(row.getStatus()))
            .map(SfStaskDispatch::getWorkerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (workerIds.stream().anyMatch(activeWorkerIds::contains)) {
            throw new ServiceException(duplicateMessage);
        }
    }

    /**
     * 校验派工邀请名额：已邀请（待确认 + 已接受）加上本次新增不得超过需求工人数。
     */
    private void ensureDispatchInviteCapacity(SfStaskWorkOrder order, int additionalInvites) {
        if (additionalInvites <= 0) {
            return;
        }
        Double requiredWorkerCount = order.getRequiredWorkerCount();
        if (requiredWorkerCount == null || Double.compare(requiredWorkerCount, 0D) <= 0) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_WORKER_INVITE_NOT_NEEDED);
        }
        long invitedCount = dispatchMapper.countByStatuses(order.getTenantId(), order.getOrderId(),
            List.of(StaskDispatchStatus.PENDING, StaskDispatchStatus.ACCEPTED));
        if (Double.compare(invitedCount + additionalInvites, requiredWorkerCount) > 0) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_INVITE_COUNT_EXCEEDS_REQUIRED);
        }
    }

    private void syncAcceptedWorkerCount(SfStaskWorkOrder order) {
        long acceptedCount = dispatchMapper.countAccepted(order.getTenantId(), order.getOrderId());
        order.setAcceptedWorkerCount((int) acceptedCount);
        order.setUpdateBy(LoginHelper.getUserId());
        order.setUpdateTime(java.time.LocalDateTime.now());
        updateOrderWithLock(order);
    }

    private void reopenOrCompleteDispatch(SfStaskWorkOrder order) {
        long acceptedCount = dispatchMapper.countAccepted(order.getTenantId(), order.getOrderId());
        if (order.getRequiredWorkerCount() == null) {
            return;
        }
        if (StaskOrderStatus.PENDING_LEADER_ASSIGN.equals(order.getStatus())
            && Double.compare(acceptedCount, order.getRequiredWorkerCount()) >= 0) {
            changeStatus(order, StaskOrderEvent.DISPATCH_READY, messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_DISPATCH_WORKER_COUNT_MET));
            return;
        }
        if (StaskOrderStatus.ASSIGN_COMPLETE.equals(order.getStatus())
            && Double.compare(acceptedCount, order.getRequiredWorkerCount()) < 0) {
            changeStatus(order, StaskOrderEvent.DISPATCH_REOPEN, messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_DISPATCH_WORKER_COUNT_INSUFFICIENT));
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
        updateOrderWithLock(order);
        insertFlowLog(order, oldStatus, newStatus, event, remark);
    }

    private int updateOrderWithLock(SfStaskWorkOrder order) {
        return ensureOptimisticUpdated(workOrderMapper.updateById(order));
    }

    private int updateDispatchWithLock(SfStaskDispatch dispatch) {
        return ensureOptimisticUpdated(dispatchMapper.updateById(dispatch));
    }

    private int ensureOptimisticUpdated(int rows) {
        if (rows <= 0) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        }
        return rows;
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

    private static List<Long> distinctIds(List<Long> ids, String emptyMessage) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException(emptyMessage);
        }
        List<Long> result = ids.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        if (CollUtil.isEmpty(result)) {
            throw new ServiceException(emptyMessage);
        }
        return result;
    }

    private static boolean matchAge(SysEmployeeLeaderOptionVo worker, Integer ageMin, Integer ageMax) {
        Integer age = calcAge(worker.getBirthDate());
        if (age == null) {
            return ageMin == null && ageMax == null;
        }
        if (ageMin != null && age < ageMin) {
            return false;
        }
        return ageMax == null || age <= ageMax;
    }

    private static Integer calcAge(Date birthDate) {
        if (birthDate == null) {
            return null;
        }
        LocalDate birth = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate now = LocalDate.now();
        int age = now.getYear() - birth.getYear();
        if (birth.plusYears(age).isAfter(now)) {
            age--;
        }
        return age;
    }

    private static int skillLevelRank(String skillLevel) {
        if ("ADVANCED".equals(skillLevel)) {
            return 0;
        }
        if ("MEDIUM".equals(skillLevel)) {
            return 1;
        }
        if ("JUNIOR".equals(skillLevel)) {
            return 2;
        }
        return 9;
    }

    private void preGenerateVoiceBroadcast(Long orderId) {
        if (voiceBroadcastService != null) {
            voiceBroadcastService.preGenerateForOrder(orderId);
        }
    }

    private SfStaskVoiceBroadcastVo latestVoiceBroadcast(Long orderId) {
        return voiceBroadcastService == null ? null : voiceBroadcastService.queryLatestForOrder(orderId);
    }
}
