package com.ym.agriculture.farmtask.dubbo;

import com.ym.agriculture.api.farmtask.RemoteEmployeeAdmissionService;
import com.ym.agriculture.api.farmtask.domain.vo.RemoteEmployeeAdmissionVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.agriculture.farmtask.employee.service.ISysEmployeeService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

/** 员工准入 Provider；查询仍受农业库租户拦截器约束。 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteEmployeeAdmissionServiceImpl implements RemoteEmployeeAdmissionService {

    private final ISysEmployeeService employeeService;

    @Override
    public RemoteEmployeeAdmissionVo checkByOpenid(String openid) {
        SysEmployeeVo employee = employeeService.queryLoginEmployeeByOpenid(openid);
        RemoteEmployeeAdmissionVo result = new RemoteEmployeeAdmissionVo();
        if (employee == null) {
            result.setAllowed(false);
            result.setMessage("员工未绑定、未审核通过或已停用");
            return result;
        }
        result.setAllowed(true);
        result.setEmployeeId(employee.getEmployeeId());
        result.setUserId(employee.getUserId());
        result.setTenantId(employee.getTenantId());
        result.setName(employee.getName());
        result.setAppRoleCode(employee.getAppRoleCode());
        result.setAppRoleName(employee.getAppRoleName());
        result.setReviewStatus(employee.getReviewStatus());
        return result;
    }
}
