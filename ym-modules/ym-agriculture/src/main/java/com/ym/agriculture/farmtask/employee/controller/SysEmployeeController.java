package com.ym.agriculture.farmtask.employee.controller;

import com.ym.common.core.domain.R;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeBo;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeReviewBatchBo;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeUpdateBo;
import com.ym.agriculture.farmtask.employee.model.vo.EmployeeAppRoleVo;
import com.ym.agriculture.farmtask.employee.model.vo.EmployeeCreateResultVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeProfileVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.agriculture.farmtask.employee.service.IEmployeeAppRoleService;
import com.ym.agriculture.farmtask.employee.service.EmployeeMiniappApprovalPermissionService;
import com.ym.system.api.domain.vo.RemoteRoleVo;
import com.ym.agriculture.farmtask.employee.service.ISysEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 人员管理控制器。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/employee")
public class SysEmployeeController extends BaseController {

    private final ISysEmployeeService employeeService;

    private final IEmployeeAppRoleService employeeAppRoleService;

    private final EmployeeMiniappApprovalPermissionService miniappApprovalPermissionService;

    /**
     * 查询已配置的人员应用角色列表。
     *
     * @return 应用角色列表
     */
    @GetMapping("/app-roles")
    public R<List<EmployeeAppRoleVo>> listAppRoles() {
        return R.ok(employeeAppRoleService.listRoles());
    }

    /**
     * 查询可关联给小程序人员的系统角色。
     *
     * @return 当前租户全部正常系统角色
     */
    @GetMapping("/miniapp-role-options")
    public R<List<RemoteRoleVo>> listMiniappRoleOptions() {
        return R.ok(miniappApprovalPermissionService.listBindableRoles());
    }

    /**
     * 查询可关联给小程序人员的系统角色。
     *
     * @return 当前租户全部正常系统角色
     * @deprecated 请使用 {@code /miniapp-role-options}
     */
    @Deprecated
    @GetMapping("/miniapp-register-approval-role-options")
    public R<List<RemoteRoleVo>> listMiniappRegisterApprovalRoleOptions() {
        return listMiniappRoleOptions();
    }

    /**
     * 查询已录入人员分页列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 人员分页列表
     */
    @GetMapping("/list")
    public R<PageResult<SysEmployeeVo>> list(SysEmployeeBo bo, PageQuery pageQuery) {
        return R.ok(employeeService.queryPage(bo, pageQuery));
    }

    /**
     * 根据人员ID查询详情。
     *
     * @param employeeId 人员ID
     * @return 人员详情
     */
    @GetMapping("/{employeeId}")
    public R<SysEmployeeVo> getInfo(@PathVariable Long employeeId) {
        return R.ok(employeeService.queryById(employeeId));
    }

    /**
     * 根据人员ID查询人员档案详情。
     *
     * @param employeeId 人员ID
     * @return 人员档案详情
     */
    @GetMapping("/{employeeId}/profile")
    public R<SysEmployeeProfileVo> profile(@PathVariable Long employeeId) {
        return R.ok(employeeService.queryProfileById(employeeId));
    }

    /**
     * 编辑人员档案基本信息（微信 openid / 微信关联手机号不可编辑）。
     *
     * @param employeeId 人员ID
     * @param bo         编辑参数
     * @return 修改结果
     */
    @Log(title = "人员管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{employeeId}/profile")
    public R<Void> updateProfile(@PathVariable Long employeeId, @Validated @RequestBody SysEmployeeUpdateBo bo) {
        if (!Objects.equals(employeeId, bo.getEmployeeId())) {
            throw new ServiceException("人员ID不一致");
        }
        return toAjax(employeeService.updateProfile(bo));
    }

    /**
     * 后台录入外部人员并生成4位绑定码。
     *
     * @param bo 人员信息
     * @return 人员与绑定码
     */
    @Log(title = "人员管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/create-with-code")
    public R<EmployeeCreateResultVo> createWithCode(@Validated @RequestBody SysEmployeeBo bo) {
        return R.ok(employeeService.createWithBindCode(bo));
    }

    /**
     * 重新生成4位绑定码。
     *
     * @param employeeId 人员ID
     * @return 新绑定码
     */
    @Log(title = "人员管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{employeeId}/regenerate-code")
    public R<EmployeeCreateResultVo> regenerateCode(@PathVariable Long employeeId) {
        return R.ok(employeeService.regenerateBindCode(employeeId));
    }

    /**
     * 删除未绑定人员。
     *
     * @param employeeId 人员ID
     * @return 删除结果
     */
    @Log(title = "人员管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/delete-unbind/{employeeId}")
    public R<Void> deleteUnbind(@PathVariable Long employeeId) {
        return toAjax(employeeService.deleteUnboundEmployee(employeeId));
    }

    /**
     * 批量删除人员（人员列表/审核列表通用）。
     *
     * @param employeeIds 人员ID，多个用逗号分隔
     * @return 删除结果
     */
    @Log(title = "人员管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{employeeIds}")
    public R<Void> remove(@PathVariable Long[] employeeIds) {
        return toAjax(employeeService.deleteByIds(employeeIds));
    }

    /**
     * 修改人员小程序角色。
     *
     * @param bo 角色信息
     * @return 修改结果
     */
    @Log(title = "人员管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/update-role")
    public R<Void> updateRole(@RequestBody SysEmployeeBo bo) {
        return toAjax(employeeService.updateRole(bo));
    }

    /**
     * 查询待审核/已拒绝人员分页列表（不含已通过）。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 审核分页列表
     */
    @GetMapping("/review-list")
    public R<PageResult<SysEmployeeVo>> reviewList(SysEmployeeBo bo, PageQuery pageQuery) {
        return R.ok(employeeService.queryReviewPage(bo, pageQuery));
    }

    /**
     * 查询审核详情。
     *
     * @param employeeId 人员ID
     * @return 审核详情
     */
    @GetMapping("/review-detail/{employeeId}")
    public R<SysEmployeeVo> reviewDetail(@PathVariable Long employeeId) {
        return R.ok(employeeService.queryById(employeeId));
    }

    /**
     * 提交审核结果。
     *
     * @param bo 审核参数
     * @return 审核结果
     */
    @Log(title = "人员审核", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/review")
    public R<Void> review(@RequestBody SysEmployeeBo bo) {
        if (bo.getEmployeeId() == null) {
            throw new IllegalArgumentException("人员ID不能为空");
        }
        return toAjax(employeeService.review(bo));
    }

    /**
     * 批量审核通过。
     *
     * @param bo 批量审核参数
     * @return 审核结果
     */
    @Log(title = "人员审核", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/review/batch-approve")
    public R<Void> batchApprove(@Validated @RequestBody SysEmployeeReviewBatchBo bo) {
        return toAjax(employeeService.batchReview(bo, EmployeeConstants.REVIEW_APPROVED));
    }

    /**
     * 批量审核拒绝。
     *
     * @param bo 批量审核参数
     * @return 审核结果
     */
    @Log(title = "人员审核", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/review/batch-reject")
    public R<Void> batchReject(@Validated @RequestBody SysEmployeeReviewBatchBo bo) {
        return toAjax(employeeService.batchReview(bo, EmployeeConstants.REVIEW_REJECTED));
    }
}
