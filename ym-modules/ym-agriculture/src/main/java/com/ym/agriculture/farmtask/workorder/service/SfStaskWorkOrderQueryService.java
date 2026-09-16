package com.ym.agriculture.farmtask.workorder.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAllTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskListQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminPackageQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkerAllTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkerHistoryQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkOrderPageBo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminPackageDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminPackageListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskReadListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerHistoryItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.service.query.SfStaskAllTaskQueryService;
import com.ym.agriculture.farmtask.workorder.service.query.SfStaskAdminTaskListQueryService;
import com.ym.agriculture.farmtask.workorder.service.query.SfStaskAdminReadQueryService;
import com.ym.agriculture.farmtask.workorder.service.query.SfStaskWorkOrderDetailQueryService;
import com.ym.agriculture.farmtask.workorder.service.query.SfStaskWorkOrderPageQueryService;
import com.ym.agriculture.farmtask.workorder.service.query.SfStaskWorkerTaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * stask 工单查询兼容门面。
 *
 * <p>保持原查询入口稳定，只负责把分页、全部任务、详情与工人任务查询转发到聚焦服务。</p>
 */
@RequiredArgsConstructor
@Service
public class SfStaskWorkOrderQueryService {

    private final SfStaskWorkOrderPageQueryService pageQueryService;
    private final SfStaskAdminTaskListQueryService adminTaskListQueryService;
    private final SfStaskAdminReadQueryService adminReadQueryService;
    private final SfStaskAllTaskQueryService allTaskQueryService;
    private final SfStaskWorkOrderDetailQueryService detailQueryService;
    private final SfStaskWorkerTaskQueryService workerTaskQueryService;

    /**
     * 分页查询任务包与拆分工单。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 任务包与拆分工单分页结果
     */
    public PageResult<SfStaskWorkOrderVo> queryPage(SfStaskWorkOrderPageBo bo, PageQuery pageQuery) {
        return pageQueryService.queryPage(bo, pageQuery);
    }

    /**
     * 分页查询平台后台统一农事任务列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 任务包与拆分工单统一分页结果
     */
    public PageResult<SfStaskAdminTaskListVo> adminTaskList(
        SfStaskAdminTaskListQueryBo bo, PageQuery pageQuery) {
        return adminTaskListQueryService.queryPage(bo, pageQuery);
    }

    public PageResult<SfStaskAdminTaskReadListVo> adminTasks(
        SfStaskAdminTaskQueryBo bo, PageQuery pageQuery) {
        return adminReadQueryService.queryTasks(bo, pageQuery);
    }

    public PageResult<SfStaskAdminPackageListVo> adminPackages(
        SfStaskAdminPackageQueryBo bo, PageQuery pageQuery) {
        return adminReadQueryService.queryPackages(bo, pageQuery);
    }

    public SfStaskAdminTaskDetailVo adminTaskDetail(Long orderId) {
        return adminReadQueryService.taskDetail(orderId);
    }

    public SfStaskAdminPackageDetailVo adminPackageDetail(Long packageId) {
        return adminReadQueryService.packageDetail(packageId);
    }

    /**
     * 分页查询生产管理员全部任务。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 生产管理员全部任务分页结果
     */
    public PageResult<SfStaskAllTaskItemVo> managerAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        return allTaskQueryService.managerAllTasks(bo, pageQuery);
    }

    /**
     * 分页查询技术员全部任务。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 技术员全部任务分页结果
     */
    public PageResult<SfStaskAllTaskItemVo> technicianAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        return allTaskQueryService.technicianAllTasks(bo, pageQuery);
    }

    /**
     * 分页查询组长全部任务。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 组长全部任务分页结果
     */
    public PageResult<SfStaskAllTaskItemVo> leaderAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        return allTaskQueryService.leaderAllTasks(bo, pageQuery);
    }

    /**
     * 分页查询领导视角的租户全量有效任务。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 领导全部任务分页结果
     */
    public PageResult<SfStaskAllTaskItemVo> leaderAdminAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        return allTaskQueryService.leaderAdminAllTasks(bo, pageQuery);
    }

    /**
     * 分页查询工人全部任务。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 工人全部任务分页结果
     */
    public PageResult<SfStaskWorkerAllTaskItemVo> workerAllTasks(
        SfStaskWorkerAllTaskQueryBo bo, PageQuery pageQuery) {
        return workerTaskQueryService.workerAllTasks(bo, pageQuery);
    }

    /**
     * 查询任务包或拆分工单详情。
     *
     * @param id 任务包或拆分工单 ID
     * @return 任务包或拆分工单详情
     */
    public SfStaskWorkOrderDetailVo detail(Long id) {
        return detailQueryService.detail(id);
    }

    /**
     * 查询工人本人的派工详情。
     *
     * @param dispatchId 派工 ID
     * @return 工人派工详情
     */
    public SfStaskWorkOrderDetailVo workerDetail(Long dispatchId) {
        return workerTaskQueryService.workerDetail(dispatchId);
    }

    /**
     * 分页查询工人验收通过的历史任务。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 工人历史任务分页结果
     */
    public PageResult<SfStaskWorkerHistoryItemVo> workerHistory(
        SfStaskWorkerHistoryQueryBo bo, PageQuery pageQuery) {
        return workerTaskQueryService.workerHistory(bo, pageQuery);
    }
}
