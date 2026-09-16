package com.ym.agriculture.farmtask.inventory.support;

import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 只基于 token 对应员工档案解析小程序库存数据范围。 */
@Component
@RequiredArgsConstructor
public class MiniappInventoryScopeResolver {

    private final SfStaskEmployeeAccessor employeeAccessor;

    public Long employeeId() {
        Long id = LoginHelper.getUserId();
        if (id == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ACTION_FORBIDDEN, 403, "未识别当前员工");
        }
        return id;
    }

    public String roleCode() {
        return resolveRole(
            EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER,
            EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER,
            EmployeeConstants.APP_ROLE_STASK_EXPERT);
    }

    /**
     * 解析仅用于领料单详情的只读岗位；列表和写接口仍使用 {@link #roleCode()} 的原有范围。
     *
     * @return 当前员工的详情数据范围角色
     */
    public String receiptDetailRoleCode() {
        return resolveRole(
            EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER,
            EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER,
            EmployeeConstants.APP_ROLE_STASK_EXPERT,
            EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN,
            EmployeeConstants.APP_ROLE_STASK_LEADER);
    }

    private String resolveRole(String... roles) {
        Long id = employeeId();
        for (String role : roles) {
            if (employeeAccessor.hasAppRole(id, role)) {
                return role;
            }
        }
        throw new InventoryBusinessException(InventoryConstants.ERROR_ACTION_FORBIDDEN, 403, "当前岗位无库存功能权限");
    }

    public boolean isKeeper() {
        return EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER.equals(roleCode());
    }
}
