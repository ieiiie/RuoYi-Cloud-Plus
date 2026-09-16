package com.ym.agriculture.farmtask.inspectionphotoarchive.support;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.helper.DataPermissionHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.agriculture.farmtask.employee.service.EmployeeMiniappApprovalPermissionService;
import com.ym.agriculture.farmtask.employee.service.ISysEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 巡查照片归档在智慧农业数据源中访问主库人员权限的桥接。
 *
 * @author ym-cloud
 */
@Component
@RequiredArgsConstructor
public class SfInspectionPhotoArchiveMasterPermissionAccessor {

    /** 系统主数据源名称。 */
    private static final String MASTER = "master";

    /** 小程序巡棚拍照入口权限。 */
    private static final String PERMISSION_INSPECTION_PHOTO_ACCESS = "miniapp:inspection-photo:access";

    private final ISysEmployeeService employeeService;
    private final EmployeeMiniappApprovalPermissionService miniappPermissionService;

    /**
     * 在主数据源读取人员档案并校验巡棚拍照权限。
     *
     * <p>调用方可能处于智慧农业数据源事务中，因此必须挂起该事务后再切换主库，
     * 避免 {@code sys_employee}、角色和菜单查询落到农业库。</p>
     *
     * @param employeeId 小程序人员 ID
     * @return 人员所属租户 ID
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public String requireInspectionPhotoAccess(Long employeeId) {
        DynamicDataSourceContextHolder.push(MASTER);
        try {
            SysEmployeeVo employee = DataPermissionHelper.ignore(() -> TenantHelper.ignore(
                () -> employeeService.queryById(employeeId)));
            if (employee == null) {
                throw new ServiceException("当前小程序人员不存在或已删除");
            }
            if (StringUtils.isBlank(employee.getTenantId())) {
                throw new ServiceException("当前人员未关联租户");
            }
            boolean permitted = TenantHelper.dynamic(employee.getTenantId(),
                () -> miniappPermissionService.resolvePermissions(employee)
                    .contains(PERMISSION_INSPECTION_PHOTO_ACCESS));
            if (!permitted) {
                throw new ServiceException("没有巡棚拍照权限");
            }
            return employee.getTenantId();
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }
}
