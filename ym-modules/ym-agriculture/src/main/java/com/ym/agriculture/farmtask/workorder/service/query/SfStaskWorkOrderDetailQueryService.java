package com.ym.agriculture.farmtask.workorder.service.query;

import cn.hutool.core.collection.CollUtil;
import com.ym.system.api.model.LoginUser;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderDetailVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.SfStaskMaterialReceiptDetailEnricher;
import com.ym.agriculture.farmtask.workorder.support.StaskGreenhousePlantingBatchHelper;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * stask 任务包与拆分工单详情查询入口。
 */
@RequiredArgsConstructor
@Service
public class SfStaskWorkOrderDetailQueryService {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final SfStaskOrderDetailBuilder detailBuilder;
    private final StaskGreenhousePlantingBatchHelper plantingBatchHelper;

    @Autowired(required = false)
    private SfStaskMaterialReceiptDetailEnricher materialReceiptDetailEnricher;

    /**
     * 查询任务包或拆分工单详情。
     *
     * @param id 任务包或工单 ID
     * @return 工单详情
     */
    public SfStaskWorkOrderDetailVo detail(Long id) {
        SfStaskEmployeeQueryContext employees = employeeContext();
        SfStaskTaskPackage taskPackage = taskPackageMapper.selectById(id);
        if (taskPackage != null) {
            ensureTenant(taskPackage.getTenantId(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_NOT_FOUND));
            List<SfStaskWorkOrder> selected = workOrderMapper.selectByPackageId(
                taskPackage.getTenantId(), taskPackage.getPackageId());
            List<SfStaskWorkOrder> splits = selected == null ? List.of() : selected;
            ensurePackageVisible(taskPackage, employees, splits);
            SfStaskWorkOrder redirected = resolveManagerPackageRedirect(taskPackage, employees, splits);
            if (redirected != null) {
                ensureOrderVisible(redirected, employees);
                return enrichOrderDetail(redirected, detailBuilder.buildOrderDetail(redirected, employees));
            }
            return enrichPackageDetail(taskPackage, detailBuilder.buildPackageDetail(taskPackage, employees, splits));
        }
        SfStaskWorkOrder order = requireOrder(id);
        ensureOrderVisible(order, employees);
        return enrichOrderDetail(order, detailBuilder.buildOrderDetail(order, employees));
    }

    private void ensureOrderVisible(SfStaskWorkOrder order, SfStaskEmployeeQueryContext employees) {
        String role = currentRole(employees);
        if (EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(role)
            && !Objects.equals(order.getLeaderId(), LoginHelper.getUserId())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_TASK_NOT_FOUND_OR_FORBIDDEN);
        }
        if (EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(role)
            && StaskOrderStatus.DRAFT.equals(order.getStatus())
            && !Objects.equals(order.getCreatorEmployeeId(), LoginHelper.getUserId())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_TASK_NOT_FOUND_OR_FORBIDDEN);
        }
    }

    private void ensurePackageVisible(SfStaskTaskPackage taskPackage, SfStaskEmployeeQueryContext employees,
        List<SfStaskWorkOrder> splits) {
        String role = currentRole(employees);
        if (EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(role)
            || EmployeeConstants.APP_ROLE_STASK_WORKER.equals(role)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_TASK_NOT_FOUND_OR_FORBIDDEN);
        }
        boolean creator = Objects.equals(taskPackage.getCreatorEmployeeId(), LoginHelper.getUserId());
        if (EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(role) && !creator
            && StaskOrderStatus.DRAFT.equals(taskPackage.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_TASK_NOT_FOUND_OR_FORBIDDEN);
        }
        if (EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN.equals(role) && !creator
            && StaskOrderStatus.DRAFT.equals(taskPackage.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_TASK_NOT_FOUND_OR_FORBIDDEN);
        }
    }

    private SfStaskWorkOrder resolveManagerPackageRedirect(SfStaskTaskPackage taskPackage,
        SfStaskEmployeeQueryContext employees, List<SfStaskWorkOrder> splits) {
        if (!EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN.equals(currentRole(employees))
            || !StaskCreatorRole.EXPERT.equals(taskPackage.getCreatorRoleCode())
            || Objects.equals(taskPackage.getCreatorEmployeeId(), LoginHelper.getUserId())) {
            return null;
        }
        List<SfStaskWorkOrder> pendingAcceptance = splits.stream()
            .filter(order -> StaskOrderStatus.PENDING_ACCEPTANCE.equals(order.getStatus()))
            .toList();
        return pendingAcceptance.size() == 1 ? pendingAcceptance.get(0) : null;
    }

    private SfStaskWorkOrderDetailVo enrichOrderDetail(SfStaskWorkOrder order, SfStaskWorkOrderDetailVo detail) {
        plantingBatchHelper.enrichWorkOrderDetailVo(detail);
        if (materialReceiptDetailEnricher != null) {
            materialReceiptDetailEnricher.enrichOrder(order, detail);
        }
        return detail;
    }

    private SfStaskWorkOrderDetailVo enrichPackageDetail(SfStaskTaskPackage taskPackage,
        SfStaskWorkOrderDetailVo detail) {
        plantingBatchHelper.enrichWorkOrderDetailVo(detail);
        if (materialReceiptDetailEnricher != null) {
            materialReceiptDetailEnricher.enrichPackage(taskPackage, detail);
        }
        return detail;
    }

    private SfStaskWorkOrder requireOrder(Long orderId) {
        SfStaskWorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND);
        }
        ensureTenant(order.getTenantId(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_WORKORDER_NOT_FOUND));
        return order;
    }

    private static void ensureTenant(String entityTenantId, String message) {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isNotBlank(tenantId) && !Objects.equals(entityTenantId, tenantId)) {
            throw new ServiceException(message);
        }
    }

    private SfStaskEmployeeQueryContext employeeContext() {
        return new SfStaskEmployeeQueryContext(ids -> {
            List<SysEmployeeVo> rows = employeeAccessor.queryBasicByIds(ids);
            return rows != null ? rows : employeeAccessor.queryByIds(ids);
        });
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
}
