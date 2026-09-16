package com.ym.agriculture.farmtask.workorder.service.packagecmd;

import cn.hutool.core.collection.CollUtil;
import com.ym.system.api.model.LoginUser;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskRejectBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 任务包命令业务守卫。
 *
 * <p>集中处理租户、角色、创建人、可编辑状态和可终止状态校验，不执行状态变更。</p>
 */
@RequiredArgsConstructor
@Component
public class SfStaskPackageCommandGuard {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskDispatchMapper dispatchMapper;
    private final SfStaskLeaderAssignmentResolver assignmentResolver;

    /**
     * 按ID读取当前租户可见的拆分工单。
     *
     * @param orderId 工单ID
     * @return 工单
     */
    public SfStaskWorkOrder requireOrder(Long orderId) {
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
     * 按任务包ID或拆分工单ID读取当前租户可见的任务包头。
     *
     * @param id 任务包ID或工单ID
     * @return 任务包头
     */
    public SfStaskTaskPackage requirePackageHeader(Long id) {
        SfStaskTaskPackage header = loadPackageHeader(id);
        if (header == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_NOT_FOUND);
        }
        return header;
    }

    /**
     * 按任务包ID或拆分工单ID解析任务包头。
     *
     * @param id 任务包ID或工单ID
     * @return 可见任务包头，不存在时返回空
     */
    public SfStaskTaskPackage loadPackageHeader(Long id) {
        SfStaskTaskPackage header = taskPackageMapper.selectById(id);
        if (header != null) {
            return matchPackageTenant(header.getTenantId()) ? header : null;
        }
        SfStaskWorkOrder order = workOrderMapper.selectById(id);
        if (order == null || !matchPackageTenant(order.getTenantId()) || order.getPackageId() == null) {
            return null;
        }
        header = taskPackageMapper.selectById(order.getPackageId());
        return header != null && matchPackageTenant(header.getTenantId()) ? header : null;
    }

    /**
     * 校验任务包处于生产管理员待技术确认状态。
     *
     * @param header 任务包头
     */
    public void ensurePendingManagerPackage(SfStaskTaskPackage header) {
        if (!StaskOrderStatus.PENDING_TECH_CONFIRM.equals(header.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_NOT_PENDING_TECH_CONFIRM);
        }
        if (!StaskCreatorRole.PRODUCTION_ADMIN.equals(header.getCreatorRoleCode())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_ONLY_PRODUCTION_SUBMISSION_REVIEWABLE);
        }
    }

    /**
     * 校验当前登录员工为任务包创建人。
     *
     * @param header 任务包头
     */
    public void ensurePackageCreator(SfStaskTaskPackage header) {
        if (!Objects.equals(header.getCreatorEmployeeId(), LoginHelper.getUserId())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_CREATOR_ONLY_EDIT);
        }
    }

    /**
     * 校验当前角色和状态允许保存或提交任务包。
     *
     * @param header          任务包头
     * @param creatorRoleCode 创建角色
     * @param submit          是否提交
     */
    public void ensurePackageEditable(SfStaskTaskPackage header, String creatorRoleCode, boolean submit) {
        if (!Objects.equals(header.getCreatorRoleCode(), creatorRoleCode)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_ROLE_CANNOT_EDIT_THIS);
        }
        if (StaskCreatorRole.EXPERT.equals(creatorRoleCode)) {
            if (!StaskOrderStatus.DRAFT.equals(header.getStatus())) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_TECHNICIAN_OWN_DRAFT_ONLY);
            }
            return;
        }
        if (StaskCreatorRole.PRODUCTION_ADMIN.equals(creatorRoleCode)) {
            if (StaskOrderStatus.DRAFT.equals(header.getStatus())) {
                return;
            }
            if (submit && StaskOrderStatus.TECH_REJECTED.equals(header.getStatus())) {
                return;
            }
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_STATUS_NOT_EDITABLE);
        }
        throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_ROLE_NOT_EDITABLE);
    }

    /**
     * 校验任务包及全部拆分工单可以作废。
     *
     * @param taskPackage 任务包头
     * @param orders      拆分工单
     */
    public void ensureVoidablePackage(SfStaskTaskPackage taskPackage, List<SfStaskWorkOrder> orders) {
        ensureVoidableStatus(taskPackage.getStatus());
        orders.forEach(order -> ensureVoidableStatus(order.getStatus()));
    }

    /**
     * 校验当前登录员工为拆分工单创建人。
     *
     * @param order 工单
     */
    public void ensureOrderCreator(SfStaskWorkOrder order) {
        if (!Objects.equals(order.getCreatorEmployeeId(), LoginHelper.getUserId())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_CREATOR_ONLY_OPERATE);
        }
    }

    /**
     * 校验拆分工单创建角色与当前角色允许单工单作废。
     *
     * @param order 工单
     */
    public void ensureVoidOrderCreatorRole(SfStaskWorkOrder order) {
        if (StaskCreatorRole.EXPERT.equals(order.getCreatorRoleCode())) {
            requireCurrentRole(EmployeeConstants.APP_ROLE_STASK_EXPERT, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_TECHNICIAN_CANCEL_OWN_ONLY));
            ensureExpertCreator(order);
            return;
        }
        if (StaskCreatorRole.PRODUCTION_ADMIN.equals(order.getCreatorRoleCode())) {
            requireCurrentRole(EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_PRODUCTION_CANCEL_ONLY));
            ensureProductionAdminVoidOrder(order);
            return;
        }
        throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_TASK_NOT_CANCELLABLE);
    }

    /**
     * 校验创建人可作废的拆分工单当前仍未开始执行。
     *
     * @param order 工单
     */
    public void ensureCreatorOrderVoidable(SfStaskWorkOrder order) {
        if (!StaskOrderStatus.isCreatorVoidableBeforeExecution(order.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_STATUS_NOT_CANCELLABLE);
        }
    }

    /**
     * 校验拆分工单尚无派工记录。
     *
     * @param tenantId 租户ID
     * @param orderId  工单ID
     */
    public void ensureNoDispatchRecords(String tenantId, Long orderId) {
        if (CollUtil.isNotEmpty(dispatchMapper.selectByOrderId(tenantId, orderId))) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_DISPATCH_EXISTS_BEFORE_CANCEL);
        }
    }

    /**
     * 校验任务包所有拆分工单仍待组长接单。
     *
     * @param tenantId  租户ID
     * @param packageId 任务包ID
     */
    public void ensurePackageOrdersAllPendingLeaderAccept(String tenantId, Long packageId) {
        List<SfStaskWorkOrder> packageOrders = workOrderMapper.selectByPackageId(tenantId, packageId);
        boolean allPending = CollUtil.isNotEmpty(packageOrders) && packageOrders.stream()
            .allMatch(order -> StaskOrderStatus.PENDING_LEADER_ACCEPT.equals(order.getStatus()));
        if (!allPending) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_LEADER_ACCEPTED_WITHDRAW_FORBIDDEN);
        }
    }

    /**
     * 校验当前登录员工拥有指定应用角色。
     *
     * @param roleCode 要求的角色码
     * @param message  校验失败消息
     */
    public void requireCurrentRole(String roleCode, String message) {
        String appRoleCode = currentEmployeeAppRoleCode();
        if (StringUtils.isNotBlank(appRoleCode) && Objects.equals(appRoleCode, roleCode)) {
            return;
        }
        LoginUser loginUser = currentLoginUserOrNull();
        Set<String> roles = loginUser != null ? loginUser.getRolePermission() : Set.of();
        if (CollUtil.isEmpty(roles) || !roles.contains(roleCode)) {
            throw new ServiceException(message);
        }
    }

    /**
     * 解析当前操作人的应用角色，员工角色为空时回退到登录角色集合首项。
     *
     * @return 当前操作角色码
     */
    public String currentRoleCode() {
        String appRoleCode = currentEmployeeAppRoleCode();
        if (StringUtils.isNotBlank(appRoleCode)) {
            return appRoleCode;
        }
        LoginUser loginUser = currentLoginUserOrNull();
        Set<String> roles = loginUser != null ? loginUser.getRolePermission() : Set.of();
        return CollUtil.isNotEmpty(roles) ? roles.iterator().next() : null;
    }

    /**
     * 校验并返回当前租户ID。
     *
     * @return 当前租户ID
     */
    public String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }

    /**
     * 校验请求包含非空原因。
     *
     * @param bo      原因请求
     * @param message 缺失时错误消息
     */
    public void requireReason(SfStaskRejectBo bo, String message) {
        if (bo == null || StringUtils.isBlank(bo.getReason())) {
            throw new ServiceException(message);
        }
    }

    /**
     * 判断实体租户是否在当前上下文可见。
     *
     * @param entityTenantId 实体租户ID
     * @return 是否可见
     */
    public boolean matchPackageTenant(String entityTenantId) {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) || Objects.equals(entityTenantId, tenantId);
    }

    private void ensureVoidableStatus(String status) {
        if (StaskOrderStatus.DRAFT.equals(status)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_DRAFT_DELETE_DIRECTLY);
        }
        if (StaskOrderStatus.TECH_REJECTED.equals(status)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_TECH_REJECTED_CANCEL_FORBIDDEN);
        }
        if (!isVoidableStatus(status)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_STATUS_NOT_CANCELLABLE);
        }
    }

    private static boolean isVoidableStatus(String status) {
        return StaskOrderStatus.PENDING_TECH_CONFIRM.equals(status)
            || StaskOrderStatus.PENDING_LEADER_ACCEPT.equals(status)
            || StaskOrderStatus.ASSIGN_COMPLETE.equals(status);
    }

    private void ensureExpertCreator(SfStaskWorkOrder order) {
        if (!StaskCreatorRole.EXPERT.equals(order.getCreatorRoleCode())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_TECHNICIAN_CREATED_SINGLE_CANCEL_ONLY);
        }
    }

    private void ensureProductionAdminVoidOrder(SfStaskWorkOrder order) {
        if (order.getPackageId() == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_PRODUCTION_CREATED_CANCEL_ONLY);
        }
        SfStaskTaskPackage taskPackage = taskPackageMapper.selectById(order.getPackageId());
        if (taskPackage == null
            || !StaskCreatorRole.PRODUCTION_ADMIN.equals(taskPackage.getCreatorRoleCode())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_PRODUCTION_CREATED_CANCEL_ONLY);
        }
    }

    private String currentEmployeeAppRoleCode() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return null;
        }
        Map<Long, SysEmployeeVo> employeeMap = assignmentResolver.loadEmployeeMap(List.of(userId));
        SysEmployeeVo employee = employeeMap.get(userId);
        return employee == null ? null : employee.getAppRoleCode();
    }

    private static LoginUser currentLoginUserOrNull() {
        try {
            return LoginHelper.getLoginUser();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
