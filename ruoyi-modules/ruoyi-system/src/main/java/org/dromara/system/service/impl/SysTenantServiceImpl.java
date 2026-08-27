package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.SysConfig;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.SysGlobalUser;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysRoleDept;
import org.dromara.system.domain.SysRoleMenu;
import org.dromara.system.domain.SysTenant;
import org.dromara.system.domain.SysTenantPackage;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.bo.SysTenantBo;
import org.dromara.system.domain.vo.SysTenantVo;
import org.dromara.system.mapper.SysConfigMapper;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysRoleDeptMapper;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysRoleMenuMapper;
import org.dromara.system.mapper.SysTenantMapper;
import org.dromara.system.mapper.SysTenantPackageMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.ISysTenantService;
import org.dromara.system.service.ISysGlobalUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 租户服务实现。
 *
 * <p>创建租户是一段跨多张表的初始化事务：先保存平台租户主数据，再在新租户
 * 上下文中创建根部门、租户管理员和角色，最后复制默认租户的参数配置。
 * 字典属于平台全局数据，不会在创建租户时复制。</p>
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysTenantServiceImpl implements ISysTenantService {

    private final SysTenantMapper tenantMapper;
    private final SysTenantPackageMapper tenantPackageMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysConfigMapper configMapper;
    private final ISysGlobalUserService globalUserService;

    @Override
    public SysTenantVo queryById(Long id) {
        return tenantMapper.selectVoById(id);
    }

    @Override
    public SysTenantVo queryByTenantId(String tenantId) {
        return tenantMapper.lambda()
            .eq(SysTenant::getTenantId, tenantId)
            .voOne();
    }

    @Override
    public PageResult<SysTenantVo> queryPageList(SysTenantBo bo, PageQuery pageQuery) {
        Page<SysTenantVo> page = tenantMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<SysTenantVo> queryList(SysTenantBo bo) {
        return tenantMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysTenantBo bo) {
        if (ObjectUtil.isNotNull(bo.getAccountCount()) && bo.getAccountCount() < -1) {
            throw new ServiceException("用户数量上限不能小于-1");
        }
        SysTenant tenant = MapstructUtils.convert(bo, SysTenant.class);
        String tenantId = generateTenantId();
        tenant.setTenantId(tenantId);
        tenant.setStatus(StringUtils.defaultIfBlank(tenant.getStatus(), SystemConstants.NORMAL));
        tenant.setAccountCount(ObjectUtil.defaultIfNull(tenant.getAccountCount(), -1L));
        if (tenantMapper.insert(tenant) <= 0) {
            throw new ServiceException("创建租户失败");
        }
        bo.setId(tenant.getId());
        bo.setTenantId(tenantId);

        TenantHelper.dynamic(tenantId, () -> {
            Long roleId = createTenantAdminRole(tenantId, bo.getPackageId());
            Long deptId = createTenantRootDept(tenantId, bo);
            bindRoleDept(roleId, deptId);
            createTenantAdminUser(tenantId, bo, deptId, roleId);
            copyDefaultConfig(tenantId);
        });
        return true;
    }

    @Override
    public Boolean updateByBo(SysTenantBo bo) {
        SysTenant current = tenantMapper.selectById(bo.getId());
        if (ObjectUtil.isNull(current)) {
            throw new ServiceException("租户不存在");
        }
        checkTenantAllowed(current.getTenantId());
        SysTenant tenant = MapstructUtils.convert(bo, SysTenant.class);
        // 租户编号和套餐必须通过专用接口修改，避免修改基础资料时误覆盖。
        tenant.setTenantId(null);
        tenant.setPackageId(null);
        return tenantMapper.updateById(tenant) > 0;
    }

    @Override
    public int updateTenantStatus(SysTenantBo bo) {
        SysTenant current = tenantMapper.selectById(bo.getId());
        if (ObjectUtil.isNull(current)) {
            throw new ServiceException("租户不存在");
        }
        checkTenantAllowed(current.getTenantId());
        SysTenant tenant = new SysTenant();
        tenant.setId(bo.getId());
        tenant.setStatus(bo.getStatus());
        return tenantMapper.updateById(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        List<SysTenant> tenants = tenantMapper.selectByIds(ids);
        if (Boolean.TRUE.equals(isValid)) {
            for (SysTenant tenant : tenants) {
                checkTenantAllowed(tenant.getTenantId());
            }
        }
        return tenantMapper.deleteByIds(ids) > 0;
    }

    @Override
    public void checkTenantAllowed(String tenantId) {
        if (TenantConstants.DEFAULT_TENANT_ID.equals(tenantId)) {
            throw new ServiceException("不允许操作默认管理租户");
        }
    }

    @Override
    public boolean checkCompanyNameUnique(SysTenantBo bo) {
        boolean exists = tenantMapper.lambda()
            .eq(SysTenant::getCompanyName, bo.getCompanyName())
            .neIfPresent(SysTenant::getId, bo.getId())
            .exists();
        return !exists;
    }

    @Override
    public void checkTenantAvailable(String tenantId) {
        if (!TenantHelper.isEnable()) {
            return;
        }
        TenantHelper.checkTenantId(tenantId);
        SysTenantVo tenant = queryByTenantId(tenantId);
        if (ObjectUtil.isNull(tenant)) {
            throw new ServiceException("租户不存在或已删除");
        }
        if (SystemConstants.DISABLE.equals(tenant.getStatus())) {
            throw new ServiceException("租户已被停用");
        }
        LocalDateTime expireTime = tenant.getExpireTime();
        if (ObjectUtil.isNotNull(expireTime) && !expireTime.isAfter(LocalDateTime.now())) {
            throw new ServiceException("租户已过期");
        }
    }

    @Override
    public void checkAccountBalance(String tenantId) {
        if (!TenantHelper.isEnable()) {
            return;
        }
        checkTenantAvailable(tenantId);
        SysTenantVo tenant = queryByTenantId(tenantId);
        Long accountCount = tenant.getAccountCount();
        if (ObjectUtil.isNull(accountCount) || accountCount == -1L) {
            return;
        }
        long userCount = TenantHelper.dynamic(tenantId, () -> userMapper.lambda().count());
        if (userCount >= accountCount) {
            throw new ServiceException("当前租户账号数量已达上限");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean syncTenantPackage(String tenantId, Long packageId) {
        checkTenantAvailable(tenantId);
        SysTenantPackage tenantPackage = tenantPackageMapper.selectById(packageId);
        if (ObjectUtil.isNull(tenantPackage) || !SystemConstants.NORMAL.equals(tenantPackage.getStatus())) {
            throw new ServiceException("租户套餐不存在或已停用");
        }
        List<Long> menuIds = parseMenuIds(tenantPackage.getMenuIds());

        TenantHelper.dynamic(tenantId, () -> {
            List<SysRole> roles = roleMapper.lambda().list();
            List<Long> normalRoleIds = new ArrayList<>(roles.size());
            for (SysRole role : roles) {
                if (TenantConstants.TENANT_ADMIN_ROLE_KEY.equals(role.getRoleKey())) {
                    replaceRoleMenus(role.getRoleId(), menuIds);
                } else {
                    normalRoleIds.add(role.getRoleId());
                }
            }
            // 普通角色不能在套餐收缩后继续保留套餐外菜单权限。
            if (CollUtil.isNotEmpty(normalRoleIds)) {
                if (CollUtil.isEmpty(menuIds)) {
                    roleMenuMapper.lambda().in(SysRoleMenu::getRoleId, normalRoleIds).delete();
                } else {
                    roleMenuMapper.lambda()
                        .in(SysRoleMenu::getRoleId, normalRoleIds)
                        .notIn(SysRoleMenu::getMenuId, menuIds)
                        .delete();
                }
            }
        });

        SysTenant tenant = queryByTenantId(tenantId) == null ? null : tenantMapper.lambda()
            .eq(SysTenant::getTenantId, tenantId)
            .one();
        if (ObjectUtil.isNull(tenant)) {
            throw new ServiceException("租户不存在或已删除");
        }
        tenant.setPackageId(packageId);
        return tenantMapper.updateById(tenant) > 0;
    }

    private LambdaQueryWrapper<SysTenant> buildQueryWrapper(SysTenantBo bo) {
        return new LambdaQueryWrapper<SysTenant>()
            .eq(StringUtils.isNotBlank(bo.getTenantId()), SysTenant::getTenantId, bo.getTenantId())
            .like(StringUtils.isNotBlank(bo.getContactUserName()), SysTenant::getContactUserName, bo.getContactUserName())
            .eq(StringUtils.isNotBlank(bo.getContactPhone()), SysTenant::getContactPhone, bo.getContactPhone())
            .like(StringUtils.isNotBlank(bo.getCompanyName()), SysTenant::getCompanyName, bo.getCompanyName())
            .eq(StringUtils.isNotBlank(bo.getLicenseNumber()), SysTenant::getLicenseNumber, bo.getLicenseNumber())
            .like(StringUtils.isNotBlank(bo.getDomain()), SysTenant::getDomain, bo.getDomain())
            .eq(ObjectUtil.isNotNull(bo.getPackageId()), SysTenant::getPackageId, bo.getPackageId())
            .eq(StringUtils.isNotBlank(bo.getStatus()), SysTenant::getStatus, bo.getStatus())
            .orderByAsc(SysTenant::getId);
    }

    private String generateTenantId() {
        Set<String> tenantIds = new HashSet<>(tenantMapper.lambda()
            .select(SysTenant::getTenantId)
            .objs(Convert::toStr));
        for (int index = 0; index < 100; index++) {
            String candidate = RandomUtil.randomNumbers(6);
            if (!tenantIds.contains(candidate)) {
                return candidate;
            }
        }
        throw new ServiceException("租户编号生成失败，请稍后重试");
    }

    private Long createTenantAdminRole(String tenantId, Long packageId) {
        SysTenantPackage tenantPackage = tenantPackageMapper.selectById(packageId);
        if (ObjectUtil.isNull(tenantPackage) || !SystemConstants.NORMAL.equals(tenantPackage.getStatus())) {
            throw new ServiceException("租户套餐不存在或已停用");
        }
        SysRole role = new SysRole();
        role.setTenantId(tenantId);
        role.setRoleName(TenantConstants.TENANT_ADMIN_ROLE_NAME);
        role.setRoleKey(TenantConstants.TENANT_ADMIN_ROLE_KEY);
        role.setRoleSort(1);
        role.setDataScope("1");
        role.setMenuCheckStrictly(true);
        role.setDeptCheckStrictly(true);
        role.setStatus(SystemConstants.NORMAL);
        role.setRemark("租户初始化管理员角色");
        if (roleMapper.insert(role) <= 0) {
            throw new ServiceException("创建租户管理员角色失败");
        }
        replaceRoleMenus(role.getRoleId(), parseMenuIds(tenantPackage.getMenuIds()));
        return role.getRoleId();
    }

    private Long createTenantRootDept(String tenantId, SysTenantBo bo) {
        SysDept dept = new SysDept();
        dept.setTenantId(tenantId);
        dept.setParentId(0L);
        dept.setAncestors(SystemConstants.ROOT_DEPT_ANCESTORS);
        dept.setDeptName(bo.getCompanyName());
        dept.setOrderNum(0);
        dept.setPhone(bo.getContactPhone());
        dept.setStatus(SystemConstants.NORMAL);
        if (deptMapper.insert(dept) <= 0) {
            throw new ServiceException("创建租户根部门失败");
        }
        return dept.getDeptId();
    }

    private void bindRoleDept(Long roleId, Long deptId) {
        SysRoleDept roleDept = new SysRoleDept();
        roleDept.setRoleId(roleId);
        roleDept.setDeptId(deptId);
        roleDeptMapper.insert(roleDept);
    }

    private void createTenantAdminUser(String tenantId, SysTenantBo bo, Long deptId, Long roleId) {
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setDeptId(deptId);
        user.setStatus(SystemConstants.NORMAL);

        // 租户管理员的认证资料只写入全局账号表，本地成员只保存租户关系和权限属性。
        SysGlobalUser candidate = new SysGlobalUser();
        candidate.setUserName(bo.getUsername());
        candidate.setNickName(bo.getContactUserName());
        candidate.setPhoneNumber(bo.getContactPhone());
        candidate.setPassword(BCrypt.hashpw(bo.getPassword()));
        SysGlobalUser globalUser = globalUserService.resolveOrCreate(candidate);
        globalUserService.applyToTenantUser(globalUser, user);
        if (userMapper.insert(user) <= 0) {
            throw new ServiceException("创建租户管理员失败");
        }

        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        dept.setLeader(user.getUserId());
        deptMapper.updateById(dept);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getUserId());
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }

    private void copyDefaultConfig(String tenantId) {
        List<SysConfig> configs = TenantHelper.dynamic(TenantConstants.DEFAULT_TENANT_ID,
            () -> configMapper.lambda().list());

        TenantHelper.dynamic(tenantId, () -> {
            configs.forEach(item -> resetConfigForTenant(item, tenantId));
            if (CollUtil.isNotEmpty(configs)) {
                configMapper.insertBatch(configs);
            }
        });
    }

    private void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.lambda().eq(SysRoleMenu::getRoleId, roleId).delete();
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        List<SysRoleMenu> roleMenus = new ArrayList<>(menuIds.size());
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenus.add(roleMenu);
        }
        roleMenuMapper.insertBatch(roleMenus);
    }

    private List<Long> parseMenuIds(String menuIds) {
        if (StringUtils.isBlank(menuIds)) {
            return List.of();
        }
        return StringUtils.splitTo(menuIds, Convert::toLong);
    }

    private void resetConfigForTenant(SysConfig item, String tenantId) {
        item.setConfigId(null);
        item.setTenantId(tenantId);
        clearAuditFields(item);
    }

    private void clearAuditFields(org.dromara.common.mybatis.core.domain.BaseEntity entity) {
        entity.setCreateDept(null);
        entity.setCreateBy(null);
        entity.setCreateTime(null);
        entity.setUpdateBy(null);
        entity.setUpdateTime(null);
    }

}
