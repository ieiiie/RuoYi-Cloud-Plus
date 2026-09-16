package com.ym.agriculture.farmtask.employee.service;

import com.ym.agriculture.farmtask.employee.model.vo.EmployeeAppRoleVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 人员应用角色配置服务。
 */
public interface IEmployeeAppRoleService {

    /**
     * 获取角色显示名称。
     *
     * @param code 角色编码
     * @return 角色名称，未配置时返回 null
     */
    String getRoleName(String code);

    /**
     * 校验角色编码是否在配置中存在，不存在则抛出业务异常。
     *
     * @param code 角色编码
     */
    void requireValidRoleCode(String code);

    /**
     * 校验岗位允许通过小程序注册或岗位邀请码选择。
     *
     * @param code 角色编码
     */
    void requireInviteSelectableRoleCode(String code);

    /**
     * 列出所有已配置的应用角色。
     *
     * @return 角色列表
     */
    List<EmployeeAppRoleVo> listRoles();

    /**
     * 批量获取角色编码与名称映射。
     *
     * @param codes 角色编码集合
     * @return code → name 映射
     */
    Map<String, String> getRoleNameMap(Collection<String> codes);
}
