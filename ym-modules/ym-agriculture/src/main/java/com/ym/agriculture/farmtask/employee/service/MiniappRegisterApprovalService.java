package com.ym.agriculture.farmtask.employee.service;

import com.ym.common.core.constant.HttpStatus;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.bo.MiniappRegisterApprovalQueryBo;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeBo;
import com.ym.agriculture.farmtask.employee.model.vo.MiniappRegisterApprovalPendingCountVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 小程序注册审批应用服务。
 */
@Service
@RequiredArgsConstructor
public class MiniappRegisterApprovalService {

    private final ISysEmployeeService employeeService;
    private final EmployeeMiniappApprovalPermissionService approvalPermissionService;

    /** 校验当前登录人员具有领导审批权限。 */
    public void requireApprovalPermission() {
        if (!EmployeeConstants.USER_TYPE_WX_EMPLOYEE.equals(LoginHelper.getLoginUser().getUserType())) {
            throw new ServiceException("当前登录身份不是小程序人员", HttpStatus.FORBIDDEN);
        }
        SysEmployeeVo currentEmployee = employeeService.queryById(LoginHelper.getUserId());
        if (!approvalPermissionService.canApproveRegister(currentEmployee)) {
            throw new ServiceException("无小程序注册审批权限", HttpStatus.FORBIDDEN);
        }
    }

    /** 查询待审批数量。 */
    public MiniappRegisterApprovalPendingCountVo pendingCount() {
        MiniappRegisterApprovalPendingCountVo vo = new MiniappRegisterApprovalPendingCountVo();
        vo.setPendingCount(employeeService.countMiniappPendingRegisterApprovals());
        return vo;
    }

    /** 查询注册审批列表。 */
    public PageResult<SysEmployeeVo> queryPage(MiniappRegisterApprovalQueryBo bo, PageQuery pageQuery) {
        return employeeService.queryMiniappRegisterApprovalPage(bo, pageQuery);
    }

    /** 查询注册审批详情。 */
    public SysEmployeeVo detail(Long employeeId) {
        return employeeService.queryMiniappRegisterApprovalById(employeeId);
    }

    /** 通过注册申请。 */
    public void approve(Long employeeId) {
        SysEmployeeBo bo = new SysEmployeeBo();
        bo.setEmployeeId(employeeId);
        bo.setReviewResult(EmployeeConstants.REVIEW_APPROVED);
        employeeService.review(bo);
    }

    /** 驳回注册申请。 */
    public void reject(Long employeeId, String reason) {
        SysEmployeeBo bo = new SysEmployeeBo();
        bo.setEmployeeId(employeeId);
        bo.setReviewResult(EmployeeConstants.REVIEW_REJECTED);
        bo.setReviewRemark(reason == null ? null : reason.trim());
        employeeService.review(bo);
    }
}
