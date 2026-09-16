package com.ym.agriculture.farmtask.workorder.service.impl;

import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchTasksVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchTasksVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchTasksVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerHomeVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerProfileVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkbenchVo;
import com.ym.agriculture.farmtask.workorder.service.workbench.SfStaskLeaderWorkbenchQueryService;
import com.ym.agriculture.farmtask.workorder.service.workbench.SfStaskLegacyWorkbenchQueryService;
import com.ym.agriculture.farmtask.workorder.service.workbench.SfStaskManagerWorkbenchQueryService;
import com.ym.agriculture.farmtask.workorder.service.workbench.SfStaskTechnicianWorkbenchQueryService;
import com.ym.agriculture.farmtask.workorder.service.workbench.SfStaskWorkerWorkbenchQueryService;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * stask 工作台查询兼容门面。
 *
 * <p>本类只解析当前租户和员工身份并路由到角色查询服务，不持有 Mapper。</p>
 */
@Service
@RequiredArgsConstructor
public class SfStaskWorkbenchQueryService {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskManagerWorkbenchQueryService managerService;
    private final SfStaskTechnicianWorkbenchQueryService technicianService;
    private final SfStaskLeaderWorkbenchQueryService leaderService;
    private final SfStaskWorkerWorkbenchQueryService workerService;
    private final SfStaskLegacyWorkbenchQueryService legacyService;

    /**
     * 查询指定业务角色的兼容工作台视图。
     *
     * @param roleCode 当前业务角色编码
     * @return 兼容工作台视图
     */
    public SfStaskWorkbenchVo workbench(String roleCode) {
        QueryIdentity identity = currentIdentity();
        if (EmployeeConstants.APP_ROLE_STASK_WORKER.equals(roleCode)) {
            return workerService.workbench(identity.tenantId(), identity.employeeId());
        }
        if (EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(roleCode)) {
            return technicianService.workbench(identity.tenantId(), identity.employeeId());
        }
        if (EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN.equals(roleCode)) {
            return managerService.workbench(identity.tenantId(), identity.employeeId());
        }
        // 组长旧版入口与未知角色一直使用 employee-visible 通用查询，保持兼容语义。
        return legacyService.workbench(identity.tenantId(), identity.employeeId());
    }

    /**
     * 查询生产管理员工作台统计。
     *
     * @return 生产管理员工作台统计
     */
    public SfStaskManagerWorkbenchSummaryVo managerWorkbenchSummary() {
        QueryIdentity identity = currentIdentity();
        return managerService.summary(identity.tenantId(), identity.employeeId());
    }

    /**
     * 查询生产管理员指定分栏任务。
     *
     * @param tab 分栏编码
     * @return 分栏任务列表
     */
    public SfStaskManagerWorkbenchTasksVo managerWorkbenchTasks(String tab) {
        QueryIdentity identity = currentIdentity();
        return managerService.tasks(identity.tenantId(), identity.employeeId(), tab);
    }

    /**
     * 查询技术员工作台统计。
     *
     * @return 技术员工作台统计
     */
    public SfStaskTechnicianWorkbenchSummaryVo technicianWorkbenchSummary() {
        QueryIdentity identity = currentIdentity();
        return technicianService.summary(identity.tenantId(), identity.employeeId());
    }

    /**
     * 查询技术员指定分栏任务。
     *
     * @param tab 分栏编码
     * @return 分栏任务列表
     */
    public SfStaskTechnicianWorkbenchTasksVo technicianWorkbenchTasks(String tab) {
        QueryIdentity identity = currentIdentity();
        return technicianService.tasks(identity.tenantId(), identity.employeeId(), tab);
    }

    /**
     * 查询组长工作台统计。
     *
     * @return 组长工作台统计
     */
    public SfStaskLeaderWorkbenchSummaryVo leaderWorkbenchSummary() {
        QueryIdentity identity = currentIdentity();
        return leaderService.summary(identity.tenantId(), identity.employeeId());
    }

    /**
     * 查询组长指定分栏任务。
     *
     * @param tab 分栏编码
     * @return 分栏任务列表
     */
    public SfStaskLeaderWorkbenchTasksVo leaderWorkbenchTasks(String tab) {
        QueryIdentity identity = currentIdentity();
        return leaderService.tasks(identity.tenantId(), identity.employeeId(), tab);
    }

    /**
     * 查询工人首页任务。
     *
     * @return 工人首页任务
     */
    public SfStaskWorkerHomeVo workerHome() {
        QueryIdentity identity = currentIdentity();
        return workerService.home(identity.tenantId(), identity.employeeId());
    }

    /**
     * 查询工人个人工作统计。
     *
     * @return 工人个人工作统计
     */
    public SfStaskWorkerProfileVo workerProfile() {
        QueryIdentity identity = currentIdentity();
        return workerService.profile(identity.tenantId(), identity.employeeId());
    }

    private QueryIdentity currentIdentity() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return new QueryIdentity(tenantId, LoginHelper.getUserId());
    }

    private record QueryIdentity(String tenantId, Long employeeId) {
    }
}
