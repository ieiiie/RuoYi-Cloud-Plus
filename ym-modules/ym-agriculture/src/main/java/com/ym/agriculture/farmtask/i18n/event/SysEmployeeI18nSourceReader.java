package com.ym.agriculture.farmtask.i18n.event;

import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.event.SysEmployeeTextChangedEvent;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.agriculture.farmtask.employee.service.ISysEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 在主数据源的新事务中读取人员当前姓名，避免并发旧事件覆盖新姓名。
 */
@Component
@RequiredArgsConstructor
public class SysEmployeeI18nSourceReader {

    private static final Set<String> STASK_ROLES = Set.of(
        EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN,
        EmployeeConstants.APP_ROLE_STASK_EXPERT,
        EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER,
        EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER,
        EmployeeConstants.APP_ROLE_STASK_WORKER
    );

    private final ISysEmployeeService employeeService;

    /**
     * 读取事件指向人员的当前姓名翻译源。
     *
     * @param event 人员文本变化事件
     * @return 当前姓名翻译源；不属于 stask 四类人员或姓名为空时返回 null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public I18nTextSource read(SysEmployeeTextChangedEvent event) {
        if (event == null || StringUtils.isBlank(event.tenantId()) || event.employeeId() == null) {
            return null;
        }
        List<SysEmployeeVo> rows = TenantHelper.dynamic(event.tenantId(),
            () -> employeeService.queryBasicByIds(List.of(event.employeeId())));
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        SysEmployeeVo employee = rows.get(0);
        if (!STASK_ROLES.contains(employee.getAppRoleCode()) || StringUtils.isBlank(employee.getName())) {
            return null;
        }
        return new I18nTextSource(I18nResourceType.SYSTEM_EMPLOYEE, employee.getEmployeeId(), "name",
            employee.getName());
    }
}
