package com.ym.agriculture.farmtask.employee.event;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

/**
 * 人员姓名事件发布器，只在原人员事务成功提交后通知下游。
 */
@Component
@RequiredArgsConstructor
public class SysEmployeeTextChangedPublisher {

    private static final Set<String> STASK_ROLES = Set.of(
        EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN,
        EmployeeConstants.APP_ROLE_STASK_EXPERT,
        EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER,
        EmployeeConstants.APP_ROLE_STASK_LEADER,
        EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER,
        EmployeeConstants.APP_ROLE_STASK_WORKER
    );

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 在当前事务提交后发布人员姓名变化事件；无事务时立即发布。
     *
     * @param tenantId 租户编号
     * @param employeeId 员工主键
     * @param appRoleCode stask 应用角色编码
     */
    public void publishAfterCommit(String tenantId, Long employeeId, String appRoleCode) {
        if (StringUtils.isBlank(tenantId) || employeeId == null || !STASK_ROLES.contains(appRoleCode)) {
            return;
        }
        SysEmployeeTextChangedEvent event = new SysEmployeeTextChangedEvent(tenantId, employeeId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(event);
            }
        });
    }
}
