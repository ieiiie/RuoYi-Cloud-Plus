package com.ym.agriculture.farmtask.assignment.support;

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
 * 农事分配模块访问主库人员数据的桥接。
 *
 * <p>人员查询必须开启独立只读事务，以挂起外层 {@code smart-farming} 的事务同步和
 * MyBatis 会话，避免复用已绑定到农业库的连接。</p>
 */
@Component
@RequiredArgsConstructor
public class SfFarmAssignEmployeeAccessor {

    private final ISysEmployeeService employeeService;

    /**
     * 在主库查询可选组长列表。
     *
     * @param keyword            搜索关键字
     * @param excludeEmployeeIds 需要排除的人员ID集合
     * @return 可选组长列表
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<SysEmployeeLeaderOptionVo> queryLeaderOptions(String keyword, Collection<Long> excludeEmployeeIds) {
        return employeeService.queryLeaderOptions(keyword, excludeEmployeeIds);
    }

    /**
     * 在主库按人员ID查询人员详情。
     *
     * @param employeeId 人员ID
     * @return 人员详情，不存在时返回 null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public SysEmployeeVo queryEmployeeById(Long employeeId) {
        return employeeService.queryById(employeeId);
    }

    /**
     * 在主库批量查询人员详情，供分配详情一次性组装组长信息。
     *
     * @param employeeIds 人员ID集合
     * @return 人员详情列表
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<SysEmployeeVo> queryEmployeesByIds(Collection<Long> employeeIds) {
        return employeeService.queryByIds(employeeIds);
    }
}
