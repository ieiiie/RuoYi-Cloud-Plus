package com.ym.agriculture.farmtask.employee.controller;

import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.employee.model.bo.MiniappRegisterApprovalQueryBo;
import com.ym.agriculture.farmtask.employee.model.bo.MiniappRegisterApprovalRejectBo;
import com.ym.agriculture.farmtask.employee.model.vo.MiniappRegisterApprovalPendingCountVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.agriculture.farmtask.employee.service.MiniappRegisterApprovalService;
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
 * 小程序领导注册审批接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/miniapp/employee/register-approvals")
public class MiniappRegisterApprovalController {

    private final MiniappRegisterApprovalService approvalService;

    /** 查询当前租户待审批数量。 */
    @GetMapping("/pending-count")
    public R<MiniappRegisterApprovalPendingCountVo> pendingCount() {
        approvalService.requireApprovalPermission();
        return R.ok(approvalService.pendingCount());
    }

    /** 分页查询注册审批列表。 */
    @GetMapping
    public R<PageResult<SysEmployeeVo>> list(MiniappRegisterApprovalQueryBo bo, PageQuery pageQuery) {
        approvalService.requireApprovalPermission();
        return R.ok(approvalService.queryPage(bo, pageQuery));
    }

    /** 查询注册审批详情。 */
    @GetMapping("/{employeeId}")
    public R<SysEmployeeVo> detail(@NotNull @PathVariable Long employeeId) {
        approvalService.requireApprovalPermission();
        return R.ok(approvalService.detail(employeeId));
    }

    /** 通过注册申请。 */
    @Log(title = "小程序注册审批", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{employeeId}/approve")
    public R<Void> approve(@NotNull @PathVariable Long employeeId) {
        approvalService.requireApprovalPermission();
        approvalService.approve(employeeId);
        return R.ok();
    }

    /** 驳回注册申请。 */
    @Log(title = "小程序注册审批", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{employeeId}/reject")
    public R<Void> reject(@NotNull @PathVariable Long employeeId,
        @Valid @RequestBody MiniappRegisterApprovalRejectBo bo) {
        approvalService.requireApprovalPermission();
        approvalService.reject(employeeId, bo.getReason());
        return R.ok();
    }
}
