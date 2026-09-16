package com.ym.system.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.constant.TenantConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.SpringUtils;
import com.ym.common.core.utils.StreamUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.system.api.model.LoginUser;
import com.ym.system.domain.SysRole;
import com.ym.system.domain.SysRoleDept;
import com.ym.system.domain.SysRoleMenu;
import com.ym.system.domain.SysUserRole;
import com.ym.system.domain.SysUser;
import com.ym.system.domain.bo.SysRoleAuthUserBo;
import com.ym.system.domain.bo.SysRoleBo;
import com.ym.system.domain.bo.SysRoleMenuBo;
import com.ym.system.domain.vo.SysRoleVo;
import com.ym.system.event.OnlineUserCleanEvent;
import com.ym.system.mapper.SysRoleDeptMapper;
import com.ym.system.mapper.SysRoleMapper;
import com.ym.system.mapper.SysRoleMenuMapper;
import com.ym.system.mapper.SysUserRoleMapper;
import com.ym.system.mapper.SysUserMapper;
import com.ym.system.service.ISysMenuService;
import com.ym.system.service.ISysRoleService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 角色 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysRoleServiceImpl implements ISysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final ISysMenuService menuService;

    /**
     * 分页查询角色列表
     *
     * @param role      查询条件
     * @param pageQuery 分页参数
     * @return 角色分页列表
     */
    @Override
    public PageResult<SysRoleVo> selectPageRoleList(SysRoleBo role, PageQuery pageQuery) {
        Page<SysRoleVo> page = roleMapper.selectPageRoleList(pageQuery.build(), this.buildQueryWrapper(role));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 根据条件查询角色数据
     *
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    @Override
    public List<SysRoleVo> selectRoleList(SysRoleBo role) {
        return roleMapper.selectRoleList(this.buildQueryWrapper(role));
    }

    @Override
    public List<SysRoleVo> selectTenantRoleList(SysRoleBo role) {
        return roleMapper.selectVoList(this.buildQueryWrapper(role));
    }

    private Wrapper<SysRole> buildQueryWrapper(SysRoleBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SysRole.class)
            .eqIfPresent(SysRole::getRoleId, bo.getRoleId())
            .likeIfText(SysRole::getRoleName, bo.getRoleName())
            .eqIfText(SysRole::getStatus, bo.getStatus())
            .likeIfText(SysRole::getRoleKey, bo.getRoleKey())
            .betweenParams(SysRole::getCreateTime, params, "beginTime", "endTime")
            .orderByAsc(SysRole::getRoleSort, SysRole::getCreateTime)
            .build();
    }

    /**
     * 根据用户ID查询角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRolesByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    /**
     * 根据用户ID查询角色列表(包含被授权状态)
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRolesAuthByUserId(Long userId) {
        List<SysRoleVo> userRoles = roleMapper.selectRolesByUserId(userId);
        List<SysRoleVo> roles = selectRoleAll();
        // 使用HashSet提高查找效率
        Set<Long> userRoleIds = StreamUtils.toSet(userRoles, SysRoleVo::getRoleId);
        for (SysRoleVo role : roles) {
            if (userRoleIds.contains(role.getRoleId())) {
                role.setFlag(true);
            }
        }
        return roles;
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectRolePermissionByUserId(Long userId) {
        List<SysRoleVo> perms = roleMapper.selectRolesByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRoleVo perm : perms) {
            if (ObjectUtil.isNotNull(perm)) {
                permsSet.addAll(StringUtils.splitList(perm.getRoleKey().trim()));
            }
        }
        return permsSet;
    }

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRoleAll() {
        return this.selectRoleList(new SysRoleBo());
    }

    /**
     * 根据用户ID获取角色选择框列表
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    @Override
    public List<Long> selectRoleListByUserId(Long userId) {
        List<SysRoleVo> list = roleMapper.selectRolesByUserId(userId);
        return StreamUtils.toList(list, SysRoleVo::getRoleId);
    }

    /**
     * 通过角色ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    @Override
    public SysRoleVo selectRoleById(Long roleId) {
        return roleMapper.selectRoleById(roleId);
    }

    /**
     * 通过角色ID串查询角色
     *
     * @param roleIds 角色ID串
     * @return 角色列表信息
     */
    @Override
    public List<SysRoleVo> selectRoleByIds(List<Long> roleIds) {
        return roleMapper.selectRoleList(roleMapper.lambda()
            .eq(SysRole::getStatus, SystemConstants.NORMAL)
            .inIfNotEmpty(SysRole::getRoleId, roleIds)
            .build());
    }

    /**
     * 校验角色名称是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleNameUnique(SysRoleBo role) {
        boolean exist = roleMapper.lambda()
            .eq(SysRole::getRoleName, role.getRoleName())
            .neIfPresent(SysRole::getRoleId, role.getRoleId())
            .exists();
        return !exist;
    }

    /**
     * 校验角色是否允许操作
     *
     * @param role 角色信息
     */
    @Override
    public void checkRoleAllowed(SysRoleBo role) {
        if (ObjectUtil.isNotNull(role.getRoleId()) && SystemConstants.SUPER_ADMIN_ROLE_ID.equals(role.getRoleId())) {
            throw new ServiceException("不允许操作超级管理员角色");
        }
        if (ObjectUtil.isNotNull(role.getRoleId())) {
            SysRole sysRole = roleMapper.selectById(role.getRoleId());
            if (sysRole != null && SystemConstants.SUPER_ADMIN_ROLE_KEY.equals(sysRole.getRoleKey())) {
                throw new ServiceException("不允许操作超级管理员角色");
            }
        }
    }

    /**
     * 校验角色是否有数据权限
     *
     * @param roleId 角色id
     */
    @Override
    public void checkRoleDataScope(Long roleId) {
        if (ObjectUtil.isNull(roleId)) {
            return;
        }
        this.checkRoleDataScope(Collections.singletonList(roleId));
    }

    /**
     * 校验角色是否有数据权限
     *
     * @param roleIds 角色ID列表（支持传单个ID）
     */
    @Override
    public void checkRoleDataScope(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds) || LoginHelper.isSuperAdmin()) {
            return;
        }
        long count = roleMapper.selectRoleCount(roleIds);
        if (count != roleIds.size()) {
            throw new ServiceException("没有权限访问部分角色数据！");
        }
    }

    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public long countUserRoleByRoleId(Long roleId) {
        return userRoleMapper.lambda().eq(SysUserRole::getRoleId, roleId).count();
    }

    /**
     * 新增保存角色信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRole(SysRoleBo bo) {
        long roleId = IdGeneratorUtil.nextLongId();
        bo.setRoleId(roleId);
        bo.setRoleKey("custom_" + roleId);
        SysRole lastRole = roleMapper.lambda()
            .orderByDesc(SysRole::getRoleSort)
            .last("LIMIT 1")
            .one();
        bo.setRoleSort(lastRole == null ? 1 : lastRole.getRoleSort() + 1);
        bo.setStatus(SystemConstants.NORMAL);
        bo.setDataScope("1");
        bo.setMenuCheckStrictly(true);
        bo.setDeptCheckStrictly(true);
        bo.setMenuIds(new Long[0]);
        bo.setDeptIds(new Long[0]);
        bo.setTemplateId(null);
        bo.setTemplateVersion(null);
        bo.setIsBuiltin(false);
        bo.setTenantDeletable(false);
        SysRole role = MapstructUtils.convert(bo, SysRole.class);
        // 新增角色信息
        roleMapper.insert(role);
        bo.setRoleId(role.getRoleId());
        return 1;
    }

    /**
     * 修改角色名称和备注，其他角色属性保持不变。
     *
     * @param bo 角色信息
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_ROLE_CUSTOM, key = "#bo.roleId")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRole(SysRoleBo bo) {
        SysRole current = roleMapper.selectById(bo.getRoleId());
        if (current == null) {
            throw new ServiceException("角色不存在");
        }
        if (Boolean.TRUE.equals(current.getIsBuiltin())) {
            return roleMapper.lambda()
                .set(SysRole::getRoleName, bo.getRoleName())
                .eq(SysRole::getRoleId, bo.getRoleId())
                .updateCount();
        }
        return roleMapper.lambda()
            .set(SysRole::getRoleName, bo.getRoleName())
            .set(SysRole::getRemark, bo.getRemark())
            .eq(SysRole::getRoleId, bo.getRoleId())
            .updateCount();
    }

    /**
     * 修改自定义角色菜单权限。
     *
     * @param bo 菜单权限参数
     * @return 影响行数
     */
    @CacheEvict(cacheNames = CacheNames.SYS_ROLE_CUSTOM, key = "#bo.roleId")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRoleMenu(SysRoleMenuBo bo) {
        checkRoleDataScope(bo.getRoleId());
        SysRole roleEntity = roleMapper.selectById(bo.getRoleId());
        if (roleEntity == null) {
            throw new ServiceException("角色不存在");
        }
        checkRoleAllowed(BeanUtil.toBean(roleEntity, SysRoleBo.class));
        rejectBuiltinRole(bo.getRoleId(), "修改菜单权限");
        validateMenusWithinPackage(bo.getMenuIds());
        int rows = roleMapper.lambda()
            .set(SysRole::getMenuCheckStrictly, bo.getMenuCheckStrictly())
            .eq(SysRole::getRoleId, bo.getRoleId())
            .updateCount();
        roleMenuMapper.lambda().eq(SysRoleMenu::getRoleId, bo.getRoleId()).delete();
        SysRoleBo role = new SysRoleBo();
        role.setRoleId(bo.getRoleId());
        role.setMenuIds(bo.getMenuIds());
        insertRoleMenu(role);
        return Math.max(rows, 1);
    }

    /**
     * 保存当前租户完整角色顺序。
     *
     * @param roleIds 有序角色ID
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRoleSort(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty() || new HashSet<>(roleIds).size() != roleIds.size()) {
            throw new ServiceException("角色排序数据不完整");
        }
        List<Long> currentRoleIds = roleMapper.lambda()
            .select(SysRole::getRoleId)
            .orderByAsc(SysRole::getRoleSort, SysRole::getCreateTime)
            .objs(Convert::toLong);
        if (currentRoleIds.size() != roleIds.size() || !new HashSet<>(currentRoleIds).equals(new HashSet<>(roleIds))) {
            throw new ServiceException("角色列表已变化，请刷新后重试");
        }
        List<SysRole> roles = new ArrayList<>(roleIds.size());
        for (int index = 0; index < roleIds.size(); index++) {
            SysRole role = new SysRole();
            role.setRoleId(roleIds.get(index));
            role.setRoleSort(index + 1);
            roles.add(role);
        }
        if (!roleMapper.updateBatchById(roles)) {
            throw new ServiceException("保存角色排序失败");
        }
        return roles.size();
    }

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态
     * @return 结果
     */
    @Override
    public int updateRoleStatus(Long roleId, String status) {
        rejectBuiltinRole(roleId, "修改状态");
        if (SystemConstants.DISABLE.equals(status) && this.countUserRoleByRoleId(roleId) > 0) {
            throw new ServiceException("角色已分配，不能禁用!");
        }
        return roleMapper.lambda()
            .set(SysRole::getStatus, status)
            .eq(SysRole::getRoleId, roleId)
            .updateCount();
    }

    /**
     * 新增角色菜单信息
     *
     * @param role 角色对象
     */
    private int insertRoleMenu(SysRoleBo role) {
        int rows = 1;
        // 新增用户与角色管理
        List<SysRoleMenu> list = new ArrayList<>();
        for (Long menuId : ObjectUtil.defaultIfNull(role.getMenuIds(), new Long[0])) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(role.getRoleId());
            rm.setMenuId(menuId);
            list.add(rm);
        }
        if (CollUtil.isNotEmpty(list)) {
            rows = roleMenuMapper.insertBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * 通过角色ID删除角色
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_ROLE_CUSTOM, key = "#roleId")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleById(Long roleId) {
        return deleteRoleByIds(List.of(roleId));
    }

    /**
     * 批量删除角色信息
     *
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_ROLE_CUSTOM, allEntries = true)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleByIds(List<Long> roleIds) {
        checkRoleDataScope(roleIds);
        List<SysRole> roles = roleMapper.selectByIds(roleIds);
        for (SysRole role : roles) {
            checkRoleAllowed(BeanUtil.toBean(role, SysRoleBo.class));
            if (Boolean.TRUE.equals(role.getIsBuiltin())
                && (!Boolean.TRUE.equals(role.getTenantDeletable())
                || TenantConstants.TENANT_ADMIN_ROLE_KEY.equals(role.getRoleKey()))) {
                throw new ServiceException("内置角色只能修改名称，不允许删除");
            }
            if (countUserRoleByRoleId(role.getRoleId()) > 0) {
                throw new ServiceException(String.format("%1$s已分配，不能删除!", role.getRoleName()));
            }
        }
        // 删除角色与菜单关联
        roleMenuMapper.lambda().in(SysRoleMenu::getRoleId, roleIds).delete();
        // 删除角色与部门关联
        roleDeptMapper.lambda().in(SysRoleDept::getRoleId, roleIds).delete();
        return roleMapper.deleteByIds(roleIds);
    }

    private void rejectBuiltinRole(Long roleId, String action) {
        SysRole role = roleMapper.selectById(roleId);
        if (role != null && Boolean.TRUE.equals(role.getIsBuiltin())) {
            throw new ServiceException("内置角色只能修改名称，不允许{}", action);
        }
    }

    private void validateMenusWithinPackage(Long[] menuIds) {
        if (menuIds == null || menuIds.length == 0) {
            return;
        }
        Set<Long> allowed = menuService.selectCurrentTenantPackageMenuIds();
        if (!allowed.containsAll(Arrays.asList(menuIds))) {
            throw new ServiceException("自定义角色不能获得当前租户套餐外的菜单权限");
        }
    }

    /**
     * 事务性应用角色用户授权变更。
     *
     * @param bo 授权变更参数
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeAuthUsers(SysRoleAuthUserBo bo) {
        checkRoleDataScope(bo.getRoleId());
        SysRole role = roleMapper.selectById(bo.getRoleId());
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        checkRoleAllowed(BeanUtil.toBean(role, SysRoleBo.class));
        Set<Long> addUserIds = new HashSet<>(Arrays.asList(bo.getAddUserIds()));
        Set<Long> removeUserIds = new HashSet<>(Arrays.asList(bo.getRemoveUserIds()));
        if (!Collections.disjoint(addUserIds, removeUserIds)) {
            throw new ServiceException("新增和取消授权用户不能重复");
        }
        Set<Long> requestedUserIds = new HashSet<>(addUserIds);
        requestedUserIds.addAll(removeUserIds);
        if (removeUserIds.contains(LoginHelper.getUserId())) {
            throw new ServiceException("不允许修改当前用户角色!");
        }
        if (!requestedUserIds.isEmpty()) {
            long userCount = userMapper.lambda().in(SysUser::getUserId, requestedUserIds).count();
            if (userCount != requestedUserIds.size()) {
                throw new ServiceException("部分用户不存在或不属于当前租户");
            }
        }

        Set<Long> authorizedUserIds = new HashSet<>(userRoleMapper.selectUserIdsByRoleId(bo.getRoleId()));
        addUserIds.removeAll(authorizedUserIds);
        removeUserIds.retainAll(authorizedUserIds);

        int rows = 0;
        if (!removeUserIds.isEmpty()) {
            rows += userRoleMapper.lambda()
                .eq(SysUserRole::getRoleId, bo.getRoleId())
                .in(SysUserRole::getUserId, removeUserIds)
                .deleteCount();
        }
        if (!addUserIds.isEmpty()) {
            List<SysUserRole> relations = StreamUtils.toList(addUserIds, userId -> {
                SysUserRole relation = new SysUserRole();
                relation.setRoleId(bo.getRoleId());
                relation.setUserId(userId);
                return relation;
            });
            if (!userRoleMapper.insertBatch(relations)) {
                throw new ServiceException("保存角色用户授权失败");
            }
            rows += relations.size();
        }
        if (rows > 0) {
            Set<Long> changedUserIds = new HashSet<>(addUserIds);
            changedUserIds.addAll(removeUserIds);
            SpringUtils.context().publishEvent(OnlineUserCleanEvent.byUsers(new ArrayList<>(changedUserIds)));
        }
        return Math.max(rows, 1);
    }

    /**
     * 根据角色ID清除该角色关联的所有在线用户的登录状态（踢出在线用户）
     *
     * <p>
     * 先判断角色是否绑定用户，若无绑定则直接返回
     * 然后遍历当前所有在线Token，查找拥有该角色的用户并强制登出
     * 注意：在线用户量过大时，操作可能导致 Redis 阻塞，需谨慎调用
     * </p>
     *
     * @param roleId 角色ID
     */
    @Override
    public void cleanOnlineUserByRole(Long roleId) {
        // 如果角色未绑定用户 直接返回
        Long num = userRoleMapper.lambda().eq(SysUserRole::getRoleId, roleId).count();
        if (num == 0) {
            return;
        }
        List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
        if (CollUtil.isEmpty(keys)) {
            return;
        }
        // 角色关联的在线用户量过大会导致redis阻塞卡顿 谨慎操作
        keys.parallelStream().forEach(key -> {
            String token = StringUtils.substringAfterLast(key, StringUtils.COLON);
            // 如果已经过期则跳过
            if (StpUtil.stpLogic.getTokenActiveTimeoutByToken(token) < -1) {
                return;
            }
            LoginUser loginUser = LoginHelper.getLoginUser(token);
            if (ObjectUtil.isNull(loginUser) || CollUtil.isEmpty(loginUser.getRoles())) {
                return;
            }
            if (loginUser.getRoles().stream().anyMatch(r -> r.getRoleId().equals(roleId))) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (NotLoginException ignored) {
                }
            }
        });
    }

    /**
     * 根据用户ID列表清除对应在线用户的登录状态（踢出指定用户）
     *
     * <p>
     * 遍历当前所有在线Token，匹配用户ID列表中的用户，强制登出
     * 注意：在线用户量过大时，操作可能导致 Redis 阻塞，需谨慎调用
     * </p>
     *
     * @param userIds 需要清除的用户ID列表
     */
    @Override
    public void cleanOnlineUser(List<Long> userIds) {
        List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
        if (CollUtil.isEmpty(keys)) {
            return;
        }
        // 角色关联的在线用户量过大会导致redis阻塞卡顿 谨慎操作
        keys.parallelStream().forEach(key -> {
            String token = StringUtils.substringAfterLast(key, StringUtils.COLON);
            // 如果已经过期则跳过
            if (StpUtil.stpLogic.getTokenActiveTimeoutByToken(token) < -1) {
                return;
            }
            LoginUser loginUser = LoginHelper.getLoginUser(token);
            if (ObjectUtil.isNull(loginUser)) {
                return;
            }
            if (userIds.contains(loginUser.getUserId())) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (NotLoginException ignored) {
                }
            }
        });
    }

}
