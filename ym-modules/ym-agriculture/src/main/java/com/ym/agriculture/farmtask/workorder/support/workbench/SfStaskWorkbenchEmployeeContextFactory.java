package com.ym.agriculture.farmtask.workorder.support.workbench;

import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 为单次工作台查询创建员工批量索引。
 */
@Component
@RequiredArgsConstructor
public class SfStaskWorkbenchEmployeeContextFactory {

    private final SfStaskEmployeeAccessor employeeAccessor;

    /**
     * 创建仅在当前公开查询内使用的员工索引。
     *
     * @return 员工查询上下文
     */
    public SfStaskEmployeeQueryContext create() {
        return new SfStaskEmployeeQueryContext(this::loadEmployees);
    }

    private List<SysEmployeeVo> loadEmployees(Collection<Long> employeeIds) {
        List<SysEmployeeVo> employees = employeeAccessor.queryBasicByIds(employeeIds);
        return employees != null ? employees : employeeAccessor.queryByIds(employeeIds);
    }
}
