package com.ym.agriculture.farmtask.workorder.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskRejectBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskListQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminPackageQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkOrderPageBo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminPackageDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminPackageListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskReadListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkbenchVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.service.ISfStaskWorkOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * stask Web 工单看板接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/stask/work-order")
public class SfStaskWorkOrderController extends BaseController {

    private final ISfStaskWorkOrderService workOrderService;

    /**
     * 分页查询工单。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 工单分页
     */
    @GetMapping("/list")
    public R<PageResult<SfStaskWorkOrderVo>> list(SfStaskWorkOrderPageBo bo, PageQuery pageQuery) {
        return R.ok(workOrderService.queryPage(bo, pageQuery));
    }

    /**
     * 分页查询平台后台统一农事任务列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 任务包与拆分工单统一分页结果
     */
    @SaCheckPermission("smartfarming:staskTask:list")
    @GetMapping("/admin-list")
    public R<PageResult<SfStaskAdminTaskListVo>> adminList(
        SfStaskAdminTaskListQueryBo bo, PageQuery pageQuery) {
        return R.ok(workOrderService.adminTaskList(bo, pageQuery));
    }

    /** 后台具体任务只读分页。 */
    @SaCheckPermission("smartfarming:staskTask:list")
    @GetMapping("/admin/tasks")
    public R<PageResult<SfStaskAdminTaskReadListVo>> adminTasks(
        SfStaskAdminTaskQueryBo bo, PageQuery pageQuery) {
        return R.ok(workOrderService.adminTasks(bo, pageQuery));
    }

    /** 后台任务包只读分页。 */
    @SaCheckPermission("smartfarming:staskTask:list")
    @GetMapping("/admin/packages")
    public R<PageResult<SfStaskAdminPackageListVo>> adminPackages(
        SfStaskAdminPackageQueryBo bo, PageQuery pageQuery) {
        return R.ok(workOrderService.adminPackages(bo, pageQuery));
    }

    /** 后台具体任务只读详情。 */
    @SaCheckPermission("smartfarming:staskTask:list")
    @GetMapping("/admin/tasks/{orderId}")
    public R<SfStaskAdminTaskDetailVo> adminTaskDetail(@NotNull @PathVariable Long orderId) {
        return R.ok(workOrderService.adminTaskDetail(orderId));
    }

    /** 后台任务包只读详情。 */
    @SaCheckPermission("smartfarming:staskTask:list")
    @GetMapping("/admin/packages/{packageId}")
    public R<SfStaskAdminPackageDetailVo> adminPackageDetail(@NotNull @PathVariable Long packageId) {
        return R.ok(workOrderService.adminPackageDetail(packageId));
    }

    /**
     * 查询工单详情。
     *
     * @param orderId 工单ID
     * @return 工单详情
     */
    @GetMapping("/{orderId}")
    public R<SfStaskWorkOrderDetailVo> detail(@NotNull @PathVariable Long orderId) {
        return R.ok(workOrderService.detail(orderId));
    }

    /**
     * 查询基础统计。
     *
     * @return 基础统计
     */
    @GetMapping("/statistics")
    public R<SfStaskWorkbenchVo> statistics() {
        return R.ok(workOrderService.workbench(null));
    }

    /**
     * 撤销任务包。
     *
     * @param packageId 任务包ID
     * @param bo        撤销原因
     * @return 操作结果
     */
    @PostMapping("/{packageId}/cancel")
    public R<Void> cancel(@NotNull @PathVariable Long packageId, @Valid @RequestBody SfStaskRejectBo bo) {
        return toAjax(workOrderService.cancelPackage(packageId, bo));
    }
}
