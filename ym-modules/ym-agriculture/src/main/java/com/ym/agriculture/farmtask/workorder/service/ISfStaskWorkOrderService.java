package com.ym.agriculture.farmtask.workorder.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAcceptanceBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdjustWorkersBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskListQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminPackageQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskCancelDispatchBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskClockInBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskCompleteBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskDispatchBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskLeaderAcceptBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskReplaceWorkerBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskRejectBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAllTaskQueryBo;
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
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchTasksVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchTasksVo;
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
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeLeaderOptionVo;

import java.util.List;

/**
 * stask 工单服务接口。
 */
public interface ISfStaskWorkOrderService {

    /**
     * 分页查询工单。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 工单分页
     */
    PageResult<SfStaskWorkOrderVo> queryPage(SfStaskWorkOrderPageBo bo, PageQuery pageQuery);

    /**
     * 平台后台分页查询统一农事任务列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 任务包与拆分工单统一分页列表
     */
    PageResult<SfStaskAdminTaskListVo> adminTaskList(
        SfStaskAdminTaskListQueryBo bo, PageQuery pageQuery);

    PageResult<SfStaskAdminTaskReadListVo> adminTasks(
        SfStaskAdminTaskQueryBo bo, PageQuery pageQuery);

    PageResult<SfStaskAdminPackageListVo> adminPackages(
        SfStaskAdminPackageQueryBo bo, PageQuery pageQuery);

    SfStaskAdminTaskDetailVo adminTaskDetail(Long orderId);

    SfStaskAdminPackageDetailVo adminPackageDetail(Long packageId);

    /**
     * 分页查询生产管理员全部任务列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 全部任务分页列表
     */
    PageResult<SfStaskAllTaskItemVo> managerAllTasks(SfStaskAllTaskQueryBo bo, PageQuery pageQuery);

    /**
     * 分页查询技术员全部任务列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 全部任务分页列表
     */
    PageResult<SfStaskAllTaskItemVo> technicianAllTasks(SfStaskAllTaskQueryBo bo, PageQuery pageQuery);

    /**
     * 分页查询组长全部任务列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 全部任务分页列表
     */
    PageResult<SfStaskAllTaskItemVo> leaderAllTasks(SfStaskAllTaskQueryBo bo, PageQuery pageQuery);

    /**
     * 分页查询领导视角的租户全量有效任务。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 全部任务分页列表
     */
    PageResult<SfStaskAllTaskItemVo> leaderAdminAllTasks(SfStaskAllTaskQueryBo bo, PageQuery pageQuery);

    /**
     * 分页查询工人全部任务列表。
     *
     * @deprecated 工人端已废弃，保留服务签名仅用于兼容存量调用。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 全部任务分页列表
     */
    @Deprecated
    PageResult<SfStaskWorkerAllTaskItemVo> workerAllTasks(SfStaskWorkerAllTaskQueryBo bo, PageQuery pageQuery);

    /**
     * 查询工单详情。
     *
     * @param orderId 工单ID
     * @return 工单详情
     */
    SfStaskWorkOrderDetailVo detail(Long orderId);

    /**
     * 创建任务包。
     *
     * @param bo              创建入参
     * @param creatorRoleCode 创建人角色编码
     * @return 任务包ID
     */
    Long createPackage(SfStaskWorkOrderCreateBo bo, String creatorRoleCode);

    /**
     * 更新任务包并保存为草稿。
     *
     * @param packageId       任务包ID
     * @param bo              编辑入参
     * @param creatorRoleCode 创建人角色编码
     * @return 任务包ID
     */
    Long savePackageDraft(Long packageId, SfStaskWorkOrderCreateBo bo, String creatorRoleCode);

    /**
     * 更新任务包并提交。
     *
     * @param packageId       任务包ID
     * @param bo              编辑入参
     * @param creatorRoleCode 创建人角色编码
     * @return 任务包ID
     */
    Long submitPackage(Long packageId, SfStaskWorkOrderCreateBo bo, String creatorRoleCode);

    /**
     * 技术员确认任务包。
     *
     * @param packageId 任务包ID
     * @param bo        技术说明入参
     * @return 拆分工单数量
     */
    int techConfirm(Long packageId, SfStaskTechConfirmBo bo);

    /**
     * 技术员退回任务包。
     *
     * @param packageId 任务包ID
     * @param bo        退回原因
     * @return 影响行数
     */
    int techReject(Long packageId, SfStaskRejectBo bo);

    /**
     * 撤销任务包。
     *
     * @param packageId 任务包ID
     * @param bo        撤销原因
     * @return 影响行数
     */
    int cancelPackage(Long packageId, SfStaskRejectBo bo);

    /**
     * 物理删除任务包。
     *
     * @param packageId 任务包ID
     * @return 影响行数
     */
    int deletePackage(Long packageId);

    /**
     * 创建人撤回任务包为草稿。
     *
     * @param packageId 任务包ID
     * @param bo        撤回原因
     * @return 影响行数
     */
    int withdrawPackage(Long packageId, SfStaskRejectBo bo);

    /**
     * 技术员整包撤回：技术员自建回草稿，生产管理员发起回待技术确认。
     *
     * @param packageId 任务包ID
     * @param bo        撤回原因
     * @return 影响行数
     */
    int technicianWithdrawPackage(Long packageId, SfStaskRejectBo bo);

    /**
     * 创建人作废任务包（软作废：标记 {@code VOIDED}，不物理删除）。
     *
     * @param packageId 任务包ID
     * @param bo        作废原因
     * @return 影响行数
     */
    int voidPackage(Long packageId, SfStaskRejectBo bo);

    /**
     * 作废单个拆分工单（软作废：标记 {@code VOIDED}，不物理删除）。
     * 技术员自建任务或生产管理员自建拆分工单均可调用。
     *
     * @param orderId 拆分工单ID
     * @param bo      作废原因
     * @return 影响行数
     */
    int voidOrder(Long orderId, SfStaskRejectBo bo);

    /**
     * 生产管理员作废：已拆单走单条 {@link #voidOrder}，未拆单走 {@link #voidPackage}。
     *
     * @param id 拆分工单 ID 或任务包 ID
     * @param bo 作废原因
     * @return 影响行数
     */
    int voidManagerTask(Long id, SfStaskRejectBo bo);

    /**
     * 组长接单。
     *
     * @param orderId 工单ID
     * @param bo      接单入参
     * @return 影响行数
     */
    int leaderAccept(Long orderId, SfStaskLeaderAcceptBo bo);

    /**
     * 组长派工。
     *
     * @deprecated 工人派工功能已废弃。
     *
     * @param orderId 工单ID
     * @param bo      派工入参
     * @return 新增派工数量
     */
    @Deprecated
    int dispatch(Long orderId, SfStaskDispatchBo bo);

    /**
     * 不需要工人，直接完成派工。
     *
     * @deprecated 工人派工功能已废弃。
     *
     * @param orderId 工单ID
     * @return 影响行数
     */
    @Deprecated
    int completeDispatchWithoutWorkers(Long orderId);

    /**
     * 推荐可派工工人。
     *
     * @deprecated 工人派工功能已废弃。
     *
     * @param orderId 工单ID
     * @param keyword 搜索关键字
     * @param gender  性别
     * @param ageMin  年龄下限
     * @param ageMax  年龄上限
     * @return 工人候选列表
     */
    @Deprecated
    List<SysEmployeeLeaderOptionVo> recommendWorkers(Long orderId, String keyword, String gender, Integer ageMin, Integer ageMax);

    /**
     * 调整需求工人数。
     *
     * @deprecated 接单时填写的工人数量不可再经派工流程修改。
     *
     * @param orderId 工单ID
     * @param bo      调整入参
     * @return 影响行数
     */
    @Deprecated
    int adjustRequiredWorkers(Long orderId, SfStaskAdjustWorkersBo bo);

    /**
     * 组长撤销派工。
     *
     * @deprecated 工人派工功能已废弃。
     *
     * @param orderId    工单ID
     * @param dispatchId 派工明细ID
     * @param bo         撤销原因
     * @return 影响行数
     */
    @Deprecated
    int cancelDispatch(Long orderId, Long dispatchId, SfStaskCancelDispatchBo bo);

    /**
     * 催促工人确认。
     *
     * @deprecated 工人派工功能已废弃。
     *
     * @param orderId    工单ID
     * @param dispatchId 派工明细ID
     * @return 影响行数
     */
    @Deprecated
    int remindDispatch(Long orderId, Long dispatchId);

    /**
     * 再邀请工人。
     *
     * @deprecated 工人派工功能已废弃。
     *
     * @param orderId    工单ID
     * @param dispatchId 原派工明细ID
     * @return 新增派工数量
     */
    @Deprecated
    int reinviteDispatch(Long orderId, Long dispatchId);

    /**
     * 换人工人。
     *
     * @deprecated 工人派工功能已废弃。
     *
     * @param orderId    工单ID
     * @param dispatchId 原派工明细ID
     * @param bo         新工人入参
     * @return 新增派工数量
     */
    @Deprecated
    int replaceDispatch(Long orderId, Long dispatchId, SfStaskReplaceWorkerBo bo);

    /**
     * 工人接受邀请。
     *
     * @deprecated 工人端已废弃。
     *
     * @param dispatchId 派工明细ID
     * @return 接收结果
     */
    @Deprecated
    SfStaskWorkerAcceptVo workerAccept(Long dispatchId);

    /**
     * 工人拒绝邀请。
     *
     * @deprecated 工人端已废弃。
     *
     * @param dispatchId 派工明细ID
     * @param bo         拒绝原因
     * @return 影响行数
     */
    @Deprecated
    int workerReject(Long dispatchId, SfStaskRejectBo bo);

    /**
     * 组长到岗打卡。
     *
     * @param orderId 工单ID
     * @param bo      打卡入参
     * @return 影响行数
     */
    int clockIn(Long orderId, SfStaskClockInBo bo);

    /**
     * 组长提交完工。
     *
     * @param orderId 工单ID
     * @param bo      完工入参
     * @return 影响行数
     */
    int complete(Long orderId, SfStaskCompleteBo bo);

    /**
     * 以新的完工资料版本重新申请验收。
     *
     * @param orderId 工单ID
     * @param bo      新的完工资料
     * @return 影响行数
     */
    int reapplyAcceptance(Long orderId, SfStaskCompleteBo bo);

    /**
     * 提交验收。
     *
     * @param orderId 工单ID
     * @param bo      验收入参
     * @return 影响行数
     */
    int acceptance(Long orderId, SfStaskAcceptanceBo bo);

    /**
     * 由任务经手技术员提交验收。
     *
     * @param orderId 工单ID
     * @param bo      验收入参
     * @return 影响行数
     */
    int technicianAcceptance(Long orderId, SfStaskAcceptanceBo bo);

    /**
     * 查询小程序工作台。
     *
     * @param roleCode 角色编码
     * @return 工作台
     */
    SfStaskWorkbenchVo workbench(String roleCode);

    /**
     * 查询生产管理员工作台统计（三栏计数）。
     *
     * @return 统计视图
     */
    SfStaskManagerWorkbenchSummaryVo managerWorkbenchSummary();

    /**
     * 查询生产管理员工作台列表（按 tab 返回单一 items）。
     *
     * @param tab pending-待处理，processing-进行中，completed_today-已完成(今日)
     * @return 列表视图
     */
    SfStaskManagerWorkbenchTasksVo managerWorkbenchTasks(String tab);

    /**
     * 查询技术员工作台统计（三栏计数）。
     *
     * @return 统计视图
     */
    SfStaskTechnicianWorkbenchSummaryVo technicianWorkbenchSummary();

    /**
     * 查询技术员工作台列表（按 tab 返回单一 items）。
     *
     * @param tab pending-待处理，processing-进行中，completed_today-已完成(今日)
     * @return 列表视图
     */
    SfStaskTechnicianWorkbenchTasksVo technicianWorkbenchTasks(String tab);

    /**
     * 查询组长工作台统计。
     *
     * @return 统计视图
     */
    SfStaskLeaderWorkbenchSummaryVo leaderWorkbenchSummary();

    /**
     * 查询组长工作台列表（按 tab 返回单一 items）。
     *
     * @param tab pending_accept-待接单，in_progress-进行中
     * @return 列表视图
     */
    SfStaskLeaderWorkbenchTasksVo leaderWorkbenchTasks(String tab);

    /**
     * 查询工人本人派工详情。
     *
     * @deprecated 工人端已废弃。
     *
     * @param dispatchId 派工明细ID
     * @return 工人只读任务详情
     */
    @Deprecated
    SfStaskWorkOrderDetailVo workerDetail(Long dispatchId);

    /**
     * 查询工人首页任务（待接受 + 已接受双分区）。
     *
     * @deprecated 工人端已废弃。
     *
     * @return 工人首页视图
     */
    @Deprecated
    SfStaskWorkerHomeVo workerHome();

    /**
     * 查询工人「我的」页统计（待接收、已接受、已完成三栏计数 + 技能从事次数）。
     *
     * @deprecated 工人端已废弃。
     *
     * @return 工人个人统计视图
     */
    @Deprecated
    SfStaskWorkerProfileVo workerProfile();

    /**
     * 分页查询工人历史任务（验收通过）。
     *
     * @deprecated 工人端已废弃。
     *
     * @param bo        筛选条件
     * @param pageQuery 分页参数
     * @return 历史任务分页列表
     */
    @Deprecated
    PageResult<SfStaskWorkerHistoryItemVo> workerHistory(SfStaskWorkerHistoryQueryBo bo, PageQuery pageQuery);
}
