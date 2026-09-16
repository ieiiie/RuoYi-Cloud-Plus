package com.ym.agriculture.farmtask.workorder.service.impl;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAcceptanceBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdjustWorkersBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskListQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminPackageQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAllTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskCancelDispatchBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskClockInBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskCompleteBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskDispatchBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskLeaderAcceptBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskRejectBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskReplaceWorkerBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTechConfirmBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkOrderCreateBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkOrderPageBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkerAllTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkerHistoryQueryBo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminPackageDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminPackageListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskReadListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchTasksVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchTasksVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchTasksVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerAcceptVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerHistoryItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerHomeVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerProfileVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkbenchVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.service.ISfStaskWorkOrderService;
import com.ym.agriculture.farmtask.workorder.service.SfStaskDispatchCommandService;
import com.ym.agriculture.farmtask.workorder.service.SfStaskExecutionCommandService;
import com.ym.agriculture.farmtask.workorder.service.SfStaskPackageCommandService;
import com.ym.agriculture.farmtask.workorder.service.SfStaskWorkOrderQueryService;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeLeaderOptionVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * stask 工单兼容门面。
 *
 * <p>保持原服务接口稳定，只负责把查询、工作台及各阶段命令转发到聚焦服务。</p>
 */
@RequiredArgsConstructor
@Service
public class SfStaskWorkOrderServiceImpl implements ISfStaskWorkOrderService {

    private final SfStaskWorkOrderQueryService queryService;
    private final SfStaskWorkbenchQueryService workbenchService;
    private final SfStaskPackageCommandService packageCommandService;
    private final SfStaskDispatchCommandService dispatchCommandService;
    private final SfStaskExecutionCommandService executionCommandService;

    @Override
    public PageResult<SfStaskWorkOrderVo> queryPage(SfStaskWorkOrderPageBo bo, PageQuery pageQuery) {
        return queryService.queryPage(bo, pageQuery);
    }

    @Override
    public PageResult<SfStaskAdminTaskListVo> adminTaskList(
        SfStaskAdminTaskListQueryBo bo, PageQuery pageQuery) {
        return queryService.adminTaskList(bo, pageQuery);
    }

    @Override
    public PageResult<SfStaskAdminTaskReadListVo> adminTasks(
        SfStaskAdminTaskQueryBo bo, PageQuery pageQuery) {
        return queryService.adminTasks(bo, pageQuery);
    }

    @Override
    public PageResult<SfStaskAdminPackageListVo> adminPackages(
        SfStaskAdminPackageQueryBo bo, PageQuery pageQuery) {
        return queryService.adminPackages(bo, pageQuery);
    }

    @Override
    public SfStaskAdminTaskDetailVo adminTaskDetail(Long orderId) {
        return queryService.adminTaskDetail(orderId);
    }

    @Override
    public SfStaskAdminPackageDetailVo adminPackageDetail(Long packageId) {
        return queryService.adminPackageDetail(packageId);
    }

    @Override
    public PageResult<SfStaskAllTaskItemVo> managerAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        return queryService.managerAllTasks(bo, pageQuery);
    }

    @Override
    public PageResult<SfStaskAllTaskItemVo> technicianAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        return queryService.technicianAllTasks(bo, pageQuery);
    }

    @Override
    public PageResult<SfStaskAllTaskItemVo> leaderAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        return queryService.leaderAllTasks(bo, pageQuery);
    }

    @Override
    public PageResult<SfStaskAllTaskItemVo> leaderAdminAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        return queryService.leaderAdminAllTasks(bo, pageQuery);
    }

    @Override
    public PageResult<SfStaskWorkerAllTaskItemVo> workerAllTasks(
        SfStaskWorkerAllTaskQueryBo bo, PageQuery pageQuery) {
        return queryService.workerAllTasks(bo, pageQuery);
    }

    @Override
    public SfStaskWorkOrderDetailVo detail(Long orderId) {
        return queryService.detail(orderId);
    }

    @Override
    public Long createPackage(SfStaskWorkOrderCreateBo bo, String creatorRoleCode) {
        return packageCommandService.createPackage(bo, creatorRoleCode);
    }

    @Override
    public Long savePackageDraft(Long packageId, SfStaskWorkOrderCreateBo bo, String creatorRoleCode) {
        return packageCommandService.savePackageDraft(packageId, bo, creatorRoleCode);
    }

    @Override
    public Long submitPackage(Long packageId, SfStaskWorkOrderCreateBo bo, String creatorRoleCode) {
        return packageCommandService.submitPackage(packageId, bo, creatorRoleCode);
    }

    @Override
    public int techConfirm(Long packageId, SfStaskTechConfirmBo bo) {
        return packageCommandService.techConfirm(packageId, bo);
    }

    @Override
    public int techReject(Long packageId, SfStaskRejectBo bo) {
        return packageCommandService.techReject(packageId, bo);
    }

    @Override
    public int cancelPackage(Long packageId, SfStaskRejectBo bo) {
        return packageCommandService.cancelPackage(packageId, bo);
    }

    @Override
    public int deletePackage(Long packageId) {
        return packageCommandService.deletePackage(packageId);
    }

    @Override
    public int withdrawPackage(Long packageId, SfStaskRejectBo bo) {
        return packageCommandService.withdrawPackage(packageId, bo);
    }

    @Override
    public int technicianWithdrawPackage(Long packageId, SfStaskRejectBo bo) {
        return packageCommandService.technicianWithdrawPackage(packageId, bo);
    }

    @Override
    public int voidPackage(Long packageId, SfStaskRejectBo bo) {
        return packageCommandService.voidPackage(packageId, bo);
    }

    @Override
    public int voidOrder(Long orderId, SfStaskRejectBo bo) {
        return packageCommandService.voidOrder(orderId, bo);
    }

    @Override
    public int voidManagerTask(Long id, SfStaskRejectBo bo) {
        return packageCommandService.voidManagerTask(id, bo);
    }

    @Override
    public int leaderAccept(Long orderId, SfStaskLeaderAcceptBo bo) {
        return dispatchCommandService.leaderAccept(orderId, bo);
    }

    @Override
    public int dispatch(Long orderId, SfStaskDispatchBo bo) {
        return dispatchCommandService.dispatch(orderId, bo);
    }

    @Override
    public int completeDispatchWithoutWorkers(Long orderId) {
        return dispatchCommandService.completeDispatchWithoutWorkers(orderId);
    }

    @Override
    public List<SysEmployeeLeaderOptionVo> recommendWorkers(
        Long orderId, String keyword, String gender, Integer ageMin, Integer ageMax) {
        return dispatchCommandService.recommendWorkers(orderId, keyword, gender, ageMin, ageMax);
    }

    @Override
    public int adjustRequiredWorkers(Long orderId, SfStaskAdjustWorkersBo bo) {
        return dispatchCommandService.adjustRequiredWorkers(orderId, bo);
    }

    @Override
    public int cancelDispatch(Long orderId, Long dispatchId, SfStaskCancelDispatchBo bo) {
        return dispatchCommandService.cancelDispatch(orderId, dispatchId, bo);
    }

    @Override
    public int remindDispatch(Long orderId, Long dispatchId) {
        return dispatchCommandService.remindDispatch(orderId, dispatchId);
    }

    @Override
    public int reinviteDispatch(Long orderId, Long dispatchId) {
        return dispatchCommandService.reinviteDispatch(orderId, dispatchId);
    }

    @Override
    public int replaceDispatch(Long orderId, Long dispatchId, SfStaskReplaceWorkerBo bo) {
        return dispatchCommandService.replaceDispatch(orderId, dispatchId, bo);
    }

    @Override
    public SfStaskWorkerAcceptVo workerAccept(Long dispatchId) {
        return dispatchCommandService.workerAccept(dispatchId);
    }

    @Override
    public int workerReject(Long dispatchId, SfStaskRejectBo bo) {
        return dispatchCommandService.workerReject(dispatchId, bo);
    }

    @Override
    public int clockIn(Long orderId, SfStaskClockInBo bo) {
        return executionCommandService.clockIn(orderId, bo);
    }

    @Override
    public int complete(Long orderId, SfStaskCompleteBo bo) {
        return executionCommandService.complete(orderId, bo);
    }

    @Override
    public int reapplyAcceptance(Long orderId, SfStaskCompleteBo bo) {
        return executionCommandService.reapplyAcceptance(orderId, bo);
    }

    @Override
    public int acceptance(Long orderId, SfStaskAcceptanceBo bo) {
        return executionCommandService.acceptance(orderId, bo);
    }

    @Override
    public int technicianAcceptance(Long orderId, SfStaskAcceptanceBo bo) {
        return executionCommandService.technicianAcceptance(orderId, bo);
    }

    @Override
    public SfStaskWorkbenchVo workbench(String roleCode) {
        return workbenchService.workbench(roleCode);
    }

    @Override
    public SfStaskManagerWorkbenchSummaryVo managerWorkbenchSummary() {
        return workbenchService.managerWorkbenchSummary();
    }

    @Override
    public SfStaskManagerWorkbenchTasksVo managerWorkbenchTasks(String tab) {
        return workbenchService.managerWorkbenchTasks(tab);
    }

    @Override
    public SfStaskTechnicianWorkbenchSummaryVo technicianWorkbenchSummary() {
        return workbenchService.technicianWorkbenchSummary();
    }

    @Override
    public SfStaskTechnicianWorkbenchTasksVo technicianWorkbenchTasks(String tab) {
        return workbenchService.technicianWorkbenchTasks(tab);
    }

    @Override
    public SfStaskLeaderWorkbenchSummaryVo leaderWorkbenchSummary() {
        return workbenchService.leaderWorkbenchSummary();
    }

    @Override
    public SfStaskLeaderWorkbenchTasksVo leaderWorkbenchTasks(String tab) {
        return workbenchService.leaderWorkbenchTasks(tab);
    }

    @Override
    public SfStaskWorkOrderDetailVo workerDetail(Long dispatchId) {
        return queryService.workerDetail(dispatchId);
    }

    @Override
    public SfStaskWorkerHomeVo workerHome() {
        return workbenchService.workerHome();
    }

    @Override
    public SfStaskWorkerProfileVo workerProfile() {
        return workbenchService.workerProfile();
    }

    @Override
    public PageResult<SfStaskWorkerHistoryItemVo> workerHistory(
        SfStaskWorkerHistoryQueryBo bo, PageQuery pageQuery) {
        return queryService.workerHistory(bo, pageQuery);
    }
}
