package com.ym.agriculture.farmtask.employee.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.mybatis.annotation.DataColumn;
import com.ym.common.mybatis.annotation.DataPermission;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.employee.model.entity.SysEmployee;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;

/**
 * 员工主数据 Mapper。
 */
public interface SysEmployeeMapper extends BaseMapperPlus<SysEmployee, SysEmployeeVo> {

    /**
     * 分页查询员工列表，并应用数据权限。
     *
     * @param page         分页对象
     * @param queryWrapper 查询条件
     * @return 员工分页结果
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "create_dept"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default Page<SysEmployeeVo> selectPageEmployeeList(Page<SysEmployee> page, Wrapper<SysEmployee> queryWrapper) {
        return this.selectVoPage(page, queryWrapper);
    }
}
