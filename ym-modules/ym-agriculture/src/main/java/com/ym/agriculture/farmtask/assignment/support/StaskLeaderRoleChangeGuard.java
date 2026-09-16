package com.ym.agriculture.farmtask.assignment.support;

import com.ym.common.core.exception.ServiceException;
import com.ym.agriculture.farmtask.assignment.dao.SfFarmWorkAssignmentMapper;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.entity.SysEmployee;
import com.ym.agriculture.farmtask.employee.support.EmployeeRoleChangeGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 防止仍有农事分配的组长被改为其他岗位。
 */
@Component
@RequiredArgsConstructor
public class StaskLeaderRoleChangeGuard implements EmployeeRoleChangeGuard {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfFarmWorkAssignmentMapper assignmentMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void validateRoleChange(SysEmployee employee, String targetRoleCode) {
        if (employee == null
            || !EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(employee.getAppRoleCode())
            || Objects.equals(EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER, targetRoleCode)) {
            return;
        }
        if (assignmentMapper.existsByLeaderId(employee.getTenantId(), employee.getEmployeeId())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_LEADER_ROLE_CHANGE_BLOCKED);
        }
    }
}
