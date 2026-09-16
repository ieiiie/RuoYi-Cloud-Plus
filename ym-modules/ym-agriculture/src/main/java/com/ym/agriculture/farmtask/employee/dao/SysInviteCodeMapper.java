package com.ym.agriculture.farmtask.employee.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.mybatis.annotation.DataColumn;
import com.ym.common.mybatis.annotation.DataPermission;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.employee.model.entity.SysInviteCode;
import com.ym.agriculture.farmtask.employee.model.vo.SysInviteCodeVo;

/**
 * 邀请码 Mapper。
 */
public interface SysInviteCodeMapper extends BaseMapperPlus<SysInviteCode, SysInviteCodeVo> {

    /**
     * 分页查询邀请码列表，并应用数据权限。
     *
     * @param page         分页对象
     * @param queryWrapper 查询条件
     * @return 邀请码分页结果
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "create_dept"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default Page<SysInviteCodeVo> selectPageInviteCodeList(Page<SysInviteCode> page, Wrapper<SysInviteCode> queryWrapper) {
        return this.selectVoPage(page, queryWrapper);
    }
}
