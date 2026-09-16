package com.ym.agriculture.farmtask.workorder.support;

import com.ym.common.mybatis.helper.DataPermissionHelper;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeBo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeLeaderOptionVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.agriculture.farmtask.employee.service.ISysEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * stask 工单模块访问主库员工数据的桥接。
 *
 * <p>员工查询必须开启独立只读事务，以挂起外层 {@code smart-farming} 的事务同步和
 * MyBatis 会话，避免复用已绑定到农业库的连接。</p>
 */
@Component
@RequiredArgsConstructor
public class SfStaskEmployeeAccessor {

    private final ISysEmployeeService employeeService;

    /**
     * 在主库按员工ID批量查询员工。
     *
     * @param employeeIds 员工ID集合
     * @return 员工列表
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<SysEmployeeVo> queryByIds(Collection<Long> employeeIds) {
        return employeeService.queryByIds(employeeIds);
    }

    /**
     * 在主库按员工ID批量查询员工基础信息，不补充邀请码等档案扩展字段。
     *
     * @param employeeIds 员工ID集合
     * @return 员工基础信息列表
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<SysEmployeeVo> queryBasicByIds(Collection<Long> employeeIds) {
        return employeeService.queryBasicByIds(employeeIds);
    }

    /**
     * 在主库按角色查询可选员工。
     *
     * @param appRoleCode        应用角色编码
     * @param keyword            搜索关键字
     * @param excludeEmployeeIds 排除员工ID集合
     * @return 可选员工列表
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<SysEmployeeLeaderOptionVo> queryOptionsByRole(String appRoleCode, String keyword,
        Collection<Long> excludeEmployeeIds) {
        return employeeService.queryOptionsByRole(appRoleCode, keyword, excludeEmployeeIds);
    }

    /**
     * 在主库查询小程序组长候选列表。
     *
     * @param keyword 搜索关键字
     * @param status  人员状态，不传默认启用
     * @return 组长候选列表
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<SysEmployeeLeaderOptionVo> queryLeaderOptions(String keyword, String status) {
        return employeeService.queryLeaderOptions(keyword, status);
    }

    /**
     * 在主库校验人员应用角色。
     *
     * @param employeeId  人员ID
     * @param appRoleCode 应用角色编码
     * @return 人员存在且角色匹配时返回 {@code true}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean hasAppRole(Long employeeId, String appRoleCode) {
        return employeeService.hasAppRole(employeeId, appRoleCode);
    }

    /**
     * 在主库分页查询人员列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 人员分页列表
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public PageResult<SysEmployeeVo> queryEmployeePage(SysEmployeeBo bo, PageQuery pageQuery) {
        // 小程序人员无 sys_role，@DataPermission 会因 roles 为空 NPE；按租户 + 业务条件查询即可
        return DataPermissionHelper.ignore(() -> employeeService.queryPage(bo, pageQuery));
    }
}
