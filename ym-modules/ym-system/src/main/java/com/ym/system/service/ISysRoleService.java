package com.ym.system.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.domain.bo.SysRoleAuthUserBo;
import com.ym.system.domain.bo.SysRoleBo;
import com.ym.system.domain.bo.SysRoleMenuBo;
import com.ym.system.domain.vo.SysRoleVo;

import java.util.List;
import java.util.Set;

/**
 * 角色业务层
 *
 * @author Lion Li
 */
public interface ISysRoleService {

    /**
     * 分页查询角色列表
     *
     * @param role      查询条件
     * @param pageQuery 分页参数
     * @return 角色分页列表
     */
    PageResult<SysRoleVo> selectPageRoleList(SysRoleBo role, PageQuery pageQuery);

    /**
     * 根据条件分页查询角色数据
     *
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    List<SysRoleVo> selectRoleList(SysRoleBo role);

    /**
     * 查询当前租户全部角色，不叠加角色创建人的数据权限过滤。
     *
     * @param role 查询条件
     * @return 完整角色列表
     */
    List<SysRoleVo> selectTenantRoleList(SysRoleBo role);

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRoleVo> selectRolesByUserId(Long userId);

    /**
     * 根据用户ID查询角色列表(包含被授权状态)
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRoleVo> selectRolesAuthByUserId(Long userId);

    /**
     * 根据用户ID查询角色权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    Set<String> selectRolePermissionByUserId(Long userId);

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    List<SysRoleVo> selectRoleAll();

    /**
     * 根据用户ID获取角色选择框列表
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    List<Long> selectRoleListByUserId(Long userId);

    /**
     * 通过角色ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    SysRoleVo selectRoleById(Long roleId);

    /**
     * 通过角色ID串查询角色
     *
     * @param roleIds 角色ID串
     * @return 角色列表信息
     */
    List<SysRoleVo> selectRoleByIds(List<Long> roleIds);

    /**
     * 校验角色名称是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    boolean checkRoleNameUnique(SysRoleBo role);

    /**
     * 校验角色是否允许操作
     *
     * @param role 角色信息
     */
    void checkRoleAllowed(SysRoleBo role);

    /**
     * 校验角色是否有数据权限
     *
     * @param roleId 角色id
     */
    void checkRoleDataScope(Long roleId);

    /**
     * 校验角色是否有数据权限
     *
     * @param roleIds 角色ID列表（支持传单个ID）
     */
    void checkRoleDataScope(List<Long> roleIds);

    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    long countUserRoleByRoleId(Long roleId);

    /**
     * 新增保存角色信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    int insertRole(SysRoleBo bo);

    /**
     * 修改角色信息及权限。
     *
     * @param bo 角色信息
     * @return 影响行数
     */
    int updateRole(SysRoleBo bo);

    /**
     * 修改角色菜单权限。
     *
     * @param bo 菜单权限参数
     * @return 影响行数
     */
    int updateRoleMenu(SysRoleMenuBo bo);

    /**
     * 保存当前租户完整角色顺序。
     *
     * @param roleIds 有序角色ID
     * @return 影响行数
     */
    int updateRoleSort(List<Long> roleIds);

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态
     * @return 结果
     */
    int updateRoleStatus(Long roleId, String status);

    /**
     * 通过角色ID删除角色
     *
     * @param roleId 角色ID
     * @return 结果
     */
    int deleteRoleById(Long roleId);

    /**
     * 批量删除角色信息
     *
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    int deleteRoleByIds(List<Long> roleIds);

    /**
     * 批量应用角色用户授权变更。
     *
     * @param bo 授权变更参数
     * @return 影响行数
     */
    int changeAuthUsers(SysRoleAuthUserBo bo);

    void cleanOnlineUserByRole(Long roleId);

    void cleanOnlineUser(List<Long> userIds);

}
