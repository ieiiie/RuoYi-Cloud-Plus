package com.ym.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.*;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.domain.SysGlobalUser;
import com.ym.system.domain.SysUser;
import com.ym.system.domain.SysUserPost;
import com.ym.system.domain.SysUserRole;
import com.ym.system.domain.bo.SysUserBo;
import com.ym.system.domain.vo.SysPostVo;
import com.ym.system.domain.vo.SysRoleVo;
import com.ym.system.domain.vo.SysUserExportVo;
import com.ym.system.domain.vo.SysUserVo;
import com.ym.system.mapper.*;
import com.ym.system.service.ISysGlobalUserService;
import com.ym.system.service.ISysUserService;
import com.ym.system.service.ISysTenantService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 用户 业务层处理
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysUserServiceImpl implements ISysUserService {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysPostMapper postMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final ISysTenantService tenantService;
    private final ISysGlobalUserService globalUserService;

    @Override
    public PageResult<SysUserVo> selectPageUserList(SysUserBo user, PageQuery pageQuery) {
        List<Long> deptIds = ObjectUtil.isNotNull(user.getDeptId()) ? deptMapper.selectDeptAndChildById(user.getDeptId()) : null;
        Page<SysUserVo> page = userMapper.selectPageUserList(pageQuery.build(), user, deptIds);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 根据条件分页查询用户列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    public List<SysUserExportVo> selectUserExportList(SysUserBo user) {
        List<Long> deptIds = ObjectUtil.isNotNull(user.getDeptId()) ? deptMapper.selectDeptAndChildById(user.getDeptId()) : null;
        return userMapper.selectUserExportList(user, deptIds);
    }

    /**
     * 分页查询当前租户全部用户，并标记其是否已授权指定角色。
     *
     * @param user      查询条件
     * @param pageQuery 分页参数
     * @return 用户分页列表及授权状态
     */
    @Override
    public PageResult<SysUserVo> selectRoleAuthUserList(SysUserBo user, PageQuery pageQuery) {
        if (user.getRoleId() == null) {
            throw new ServiceException("角色ID不能为空");
        }
        Page<SysUserVo> page = userMapper.selectRoleAuthUserPage(pageQuery.build(), user);
        PageResult<SysUserVo> result = PageResult.build(page.getRecords(), page.getTotal());
        Set<Long> authorizedUserIds = new HashSet<>(userRoleMapper.selectUserIdsByRoleId(user.getRoleId()));
        result.getRows().forEach(item -> item.setAuthorized(authorizedUserIds.contains(item.getUserId())));
        return result;
    }

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserByUserName(String userName) {
        return userMapper.selectUserByUserName(userName);
    }

    /**
     * 通过手机号查询用户
     *
     * @param phoneNumber 手机号
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserByPhoneNumber(String phoneNumber) {
        return userMapper.selectUserByPhoneNumber(phoneNumber);
    }

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserById(Long userId) {
        SysUserVo user = userMapper.selectUserVoById(userId);
        if (ObjectUtil.isNull(user)) {
            return user;
        }
        user.setRoles(roleMapper.selectRolesByUserId(user.getUserId()));
        return user;
    }

    /**
     * 通过用户ID串查询用户
     *
     * @param userIds 用户ID串
     * @param deptId  部门id
     * @return 用户列表信息
     */
    @Override
    public List<SysUserVo> selectUserByIds(List<Long> userIds, Long deptId) {
        return userMapper.selectUserByIds(userIds, deptId);
    }

    /**
     * 查询用户所属角色组
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(Long userId) {
        List<SysRoleVo> list = roleMapper.selectRolesByUserId(userId);
        if (CollUtil.isEmpty(list)) {
            return StringUtils.EMPTY;
        }
        return StreamUtils.join(list, SysRoleVo::getRoleName);
    }

    /**
     * 查询用户所属岗位组
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(Long userId) {
        List<SysPostVo> list = postMapper.selectPostsByUserId(userId);
        if (CollUtil.isEmpty(list)) {
            return StringUtils.EMPTY;
        }
        return StreamUtils.join(list, SysPostVo::getPostName);
    }

    /**
     * 校验用户账号是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(SysUserBo user) {
        return checkGlobalIdentifierUnique(user, user.getUserName());
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     */
    @Override
    public boolean checkPhoneUnique(SysUserBo user) {
        return checkGlobalIdentifierUnique(user, user.getPhoneNumber());
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     */
    @Override
    public boolean checkEmailUnique(SysUserBo user) {
        return checkGlobalIdentifierUnique(user, user.getEmail());
    }

    /**
     * 校验认证标识是否归属于当前用户的全局账号。
     *
     * <p>用户名、手机号和邮箱在全局账号维度唯一；新增成员时命中已有全局账号是
     * “复用账号”的正常场景，因此新增入口不应直接以本方法拒绝该账号。</p>
     *
     * @param user       租户成员请求
     * @param identifier 用户名、手机号或邮箱
     * @return true 标识未被其他全局账号占用
     */
    private boolean checkGlobalIdentifierUnique(SysUserBo user, String identifier) {
        if (StringUtils.isBlank(identifier)) {
            return true;
        }
        SysGlobalUser globalUser = globalUserService.queryByIdentifier(identifier);
        if (ObjectUtil.isNull(globalUser)) {
            return true;
        }
        if (ObjectUtil.isNull(user.getUserId())) {
            return false;
        }
        SysUser tenantUser = userMapper.selectById(user.getUserId());
        return ObjectUtil.isNotNull(tenantUser)
            && ObjectUtil.equals(tenantUser.getGlobalUserId(), globalUser.getGlobalUserId());
    }

    /**
     * 校验用户是否允许操作
     *
     * @param userId 用户ID
     */
    @Override
    public void checkUserAllowed(Long userId) {
        if (ObjectUtil.isNotNull(userId) && LoginHelper.isSuperAdmin(userId)) {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     *
     * @param userId 用户id
     */
    @Override
    public void checkUserDataScope(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            return;
        }
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        if (userMapper.countUserById(userId) == 0) {
            throw new ServiceException("没有权限访问用户数据！");
        }
    }

    /**
     * 新增保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertUser(SysUserBo user) {
        tenantService.checkAccountBalance(TenantHelper.getTenantId());
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        SysGlobalUser globalUser = globalUserService.resolveOrCreate(toGlobalUserCandidate(user));
        boolean exists = userMapper.lambda()
            .eq(SysUser::getGlobalUserId, globalUser.getGlobalUserId())
            .exists();
        if (exists) {
            throw new ServiceException("该全局账号已在当前租户中存在");
        }
        globalUserService.applyToTenantUser(globalUser, sysUser);
        // 新增用户信息
        int rows = userMapper.insert(sysUser);
        user.setUserId(sysUser.getUserId());
        // 新增用户岗位关联
        insertUserPost(user, false);
        // 新增用户与角色管理
        insertUserRole(user, false);
        return rows;
    }

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerUser(SysUserBo user) {
        tenantService.checkAccountBalance(TenantHelper.getTenantId());
        user.setCreateBy(0L);
        user.setUpdateBy(0L);
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        SysGlobalUser globalUser = globalUserService.resolveOrCreate(toGlobalUserCandidate(user));
        boolean exists = userMapper.lambda()
            .eq(SysUser::getGlobalUserId, globalUser.getGlobalUserId())
            .exists();
        if (exists) {
            throw new ServiceException("该全局账号已在当前租户中存在");
        }
        globalUserService.applyToTenantUser(globalUser, sysUser);
        return userMapper.insert(sysUser) > 0;
    }

    /**
     * 将新增或注册请求中的全局账号资料与租户成员资料拆分。
     *
     * <p>sys_user 不再持久化用户名、手机号和密码；这些字段仅作为本次创建或复用
     * 全局账号的输入传给 sys_global_user。</p>
     *
     * @param user 用户请求
     * @return 全局账号候选资料
     */
    private SysGlobalUser toGlobalUserCandidate(SysUserBo user) {
        SysGlobalUser globalUser = new SysGlobalUser();
        globalUser.setUserName(user.getUserName());
        globalUser.setNickName(user.getNickName());
        globalUser.setUserType(user.getUserType());
        globalUser.setEmail(user.getEmail());
        globalUser.setPhoneNumber(user.getPhoneNumber());
        globalUser.setGender(user.getGender());
        globalUser.setAvatar(user.getAvatar());
        globalUser.setPassword(user.getPassword());
        globalUser.setRemark(user.getRemark());
        return globalUser;
    }

    /**
     * 修改保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @CacheEvict(cacheNames = CacheNames.SYS_NICKNAME, key = "#user.userId")
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(SysUserBo user) {
        SysUser current = userMapper.selectById(user.getUserId());
        if (ObjectUtil.isNull(current)) {
            throw new ServiceException("用户不存在");
        }
        SysGlobalUser globalUser = globalUserService.queryById(current.getGlobalUserId());
        if (ObjectUtil.isNull(globalUser)) {
            throw new ServiceException("当前租户用户未关联全局账号");
        }
        checkTenantMemberOnlyUpdate(user, globalUser);
        // 新增用户与角色管理
        insertUserRole(user, true);
        // 新增用户与岗位管理
        insertUserPost(user, true);
        // 租户管理员只能维护本租户成员属性，账号资料由全局账号服务统一维护。
        SysUser sysUser = new SysUser();
        sysUser.setUserId(user.getUserId());
        sysUser.setDeptId(user.getDeptId());
        sysUser.setStatus(user.getStatus());
        sysUser.setRemark(user.getRemark());
        // 防止错误更新后导致的数据误删除
        int flag = userMapper.updateById(sysUser);
        if (flag < 1) {
            throw new ServiceException("修改用户{}信息失败", user.getUserName());
        }
        return flag;
    }

    /**
     * 用户管理编辑接口只允许维护租户成员关系。全局资料必须由账号本人或默认
     * 管理租户的超级管理员通过全局账号接口修改，避免“提交成功但资料未生效”。
     */
    private void checkTenantMemberOnlyUpdate(SysUserBo request, SysGlobalUser globalUser) {
        boolean changed = !StringUtils.equals(request.getUserName(), globalUser.getUserName())
            || !StringUtils.equals(request.getNickName(), globalUser.getNickName())
            || (StringUtils.isNotBlank(request.getUserType()) && !StringUtils.equals(request.getUserType(), globalUser.getUserType()))
            || (StringUtils.isNotBlank(request.getEmail()) && !StringUtils.equals(request.getEmail(), globalUser.getEmail()))
            || (StringUtils.isNotBlank(request.getPhoneNumber()) && !StringUtils.equals(request.getPhoneNumber(), globalUser.getPhoneNumber()))
            || (StringUtils.isNotBlank(request.getGender()) && !StringUtils.equals(request.getGender(), globalUser.getGender()))
            || (ObjectUtil.isNotNull(request.getAvatar()) && !ObjectUtil.equals(request.getAvatar(), globalUser.getAvatar()))
            || StringUtils.isNotBlank(request.getPassword());
        if (changed) {
            throw new ServiceException("租户用户管理不能修改全局账号资料，请由账号本人或默认管理租户超级管理员维护");
        }
    }

    /**
     * 用户授权角色
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUserAuth(Long userId, Long[] roleIds) {
        insertUserRole(userId, roleIds, true);
    }

    /**
     * 修改用户状态
     *
     * @param userId 用户ID
     * @param status 账号状态
     * @return 结果
     */
    @Override
    public int updateUserStatus(Long userId, String status) {
        return userMapper.lambda()
            .set(SysUser::getStatus, status)
            .eq(SysUser::getUserId, userId)
            .updateCount();
    }

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_NICKNAME, key = "#user.userId")
    @Override
    public int updateUserProfile(SysUserBo user) {
        SysUser tenantUser = userMapper.selectById(user.getUserId());
        if (ObjectUtil.isNull(tenantUser) || ObjectUtil.isNull(tenantUser.getGlobalUserId())) {
            throw new ServiceException("当前租户用户未关联全局账号");
        }
        com.ym.system.domain.bo.SysUserProfileBo profile = new com.ym.system.domain.bo.SysUserProfileBo();
        profile.setUserName(user.getUserName());
        profile.setNickName(user.getNickName());
        profile.setAvatar(user.getAvatar());
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setEmail(user.getEmail());
        profile.setGender(user.getGender());
        return globalUserService.updateProfile(tenantUser.getGlobalUserId(), profile);
    }

    /**
     * 重置用户密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(Long userId, String password) {
        SysUser tenantUser = userMapper.selectById(userId);
        if (ObjectUtil.isNull(tenantUser) || ObjectUtil.isNull(tenantUser.getGlobalUserId())) {
            throw new ServiceException("当前租户用户未关联全局账号");
        }
        return globalUserService.resetPassword(tenantUser.getGlobalUserId(), password);
    }

    /**
     * 新增用户角色信息
     *
     * @param user  用户对象
     * @param clear 清除已存在的关联数据
     */
    private void insertUserRole(SysUserBo user, boolean clear) {
        this.insertUserRole(user.getUserId(), user.getRoleIds(), clear);
    }

    /**
     * 新增用户岗位信息
     *
     * @param user  用户对象
     * @param clear 清除已存在的关联数据
     */
    private void insertUserPost(SysUserBo user, boolean clear) {
        Long[] postIdArr = user.getPostIds();
        if (ArrayUtil.isEmpty(postIdArr)) {
            return;
        }
        List<Long> postIds = Arrays.asList(postIdArr);

        // 校验是否有权限操作这些岗位（含数据权限控制）
        if (postMapper.selectPostCount(postIds) != postIds.size()) {
            throw new ServiceException("没有权限访问岗位的数据");
        }

        // 是否清除旧的用户岗位绑定
        if (clear) {
            userPostMapper.lambda().eq(SysUserPost::getUserId, user.getUserId()).deleteCount();
        }

        // 构建用户岗位关联列表并批量插入
        List<SysUserPost> list = StreamUtils.toList(postIds,
            postId -> {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                return up;
            });
        userPostMapper.insertBatch(list);
    }

    /**
     * 新增用户角色信息
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     * @param clear   清除已存在的关联数据
     */
    private void insertUserRole(Long userId, Long[] roleIds, boolean clear) {
        if (ArrayUtil.isEmpty(roleIds)) {
            return;
        }

        List<Long> roleList = new ArrayList<>(Arrays.asList(roleIds));

        // 非超级管理员，禁止包含超级管理员角色
        if (!LoginHelper.isSuperAdmin(userId)) {
            roleList.remove(SystemConstants.SUPER_ADMIN_ROLE_ID);
        }

        // 移除超管角色后若无剩余角色，说明仅选了超管角色且不允许分配，显式报错
        if (roleList.isEmpty()) {
            throw new ServiceException("不允许为普通用户分配超级管理员角色，请至少选择一个其他角色");
        }

        // 校验是否有权限访问这些角色（含数据权限控制）
        if (roleMapper.selectRoleCount(roleList) != roleList.size()) {
            throw new ServiceException("没有权限访问角色的数据");
        }

        // 是否清除原有绑定
        if (clear) {
            userRoleMapper.lambda().eq(SysUserRole::getUserId, userId).deleteCount();
        }

        // 批量插入用户-角色关联
        List<SysUserRole> list = StreamUtils.toList(roleList,
            roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                return ur;
            });
        userRoleMapper.insertBatch(list);
    }

    /**
     * 通过用户ID删除用户
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserById(Long userId) {
        checkUserAllowed(userId);
        checkUserDataScope(userId);
        // 删除用户与角色关联
        userRoleMapper.lambda().eq(SysUserRole::getUserId, userId).deleteCount();
        // 删除用户与岗位表
        userPostMapper.lambda().eq(SysUserPost::getUserId, userId).deleteCount();
        // 仅删除当前租户成员，保留 sys_global_user；物理删除可释放成员唯一关系以便再次加入。
        int flag = userMapper.deleteTenantMemberById(userId);
        if (flag < 1) {
            throw new ServiceException("删除用户失败!");
        }
        return flag;
    }

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserByIds(Long[] userIds) {
        for (Long userId : userIds) {
            checkUserAllowed(userId);
            checkUserDataScope(userId);
        }
        List<Long> ids = List.of(userIds);
        // 删除用户与角色关联
        userRoleMapper.lambda().in(SysUserRole::getUserId, ids).delete();
        // 删除用户与岗位表
        userPostMapper.lambda().in(SysUserPost::getUserId, ids).delete();
        int flag = 0;
        for (Long userId : ids) {
            flag += userMapper.deleteTenantMemberById(userId);
        }
        if (flag != ids.size()) {
            throw new ServiceException("删除用户失败!");
        }
        return flag;
    }

    /**
     * 通过部门id查询当前部门所有用户
     *
     * @param deptId 部门ID
     * @return 用户信息集合信息
     */
    @Override
    public List<SysUserVo> selectUserListByDept(Long deptId) {
        return userMapper.selectUserVoListByDeptId(deptId);
    }

    @Override
    public List<Long> selectUserIdsByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return List.of();
        }
        List<SysUserRole> userRoles = userRoleMapper.lambda().in(SysUserRole::getRoleId, roleIds).list();
        return StreamUtils.toList(userRoles, SysUserRole::getUserId);
    }

    /**
     * 通过用户ID查询用户账户
     *
     * @param userId 用户ID
     * @return 用户账户
     */
    @Cacheable(cacheNames = CacheNames.SYS_USER_NAME, key = "#userId")
    @Override
    public String selectUserNameById(Long userId) {
        SysUserVo user = userMapper.selectUserVoById(userId);
        return ObjectUtils.notNullGetter(user, SysUserVo::getUserName);
    }

    /**
     * 通过用户ID查询用户昵称
     *
     * @param userId 用户ID
     * @return 用户昵称
     */
    @Override
    @Cacheable(cacheNames = CacheNames.SYS_NICKNAME, key = "#userId")
    public String selectNicknameById(Long userId) {
        SysUser sysUser = userMapper.lambda()
            .select(SysUser::getNickName)
            .eq(SysUser::getUserId, userId)
            .one();
        return ObjectUtils.notNullGetter(sysUser, SysUser::getNickName);
    }

    @Override
    public String selectNicknameByIds(String userIds) {
        List<String> list = new ArrayList<>();
        for (Long id : StringUtils.splitTo(userIds, Convert::toLong)) {
            String nickname = SpringUtils.getAopProxy(this).selectNicknameById(id);
            if (StringUtils.isNotBlank(nickname)) {
                list.add(nickname);
            }
        }
        return StringUtils.joinComma(list);
    }

    /**
     * 通过用户ID查询用户手机号
     *
     * @param userId 用户id
     * @return 用户手机号
     */
    @Override
    public String selectPhonenumberById(Long userId) {
        SysUserVo user = userMapper.selectUserVoById(userId);
        return ObjectUtils.notNullGetter(user, SysUserVo::getPhoneNumber);
    }

    /**
     * 通过用户ID查询用户邮箱
     *
     * @param userId 用户id
     * @return 用户邮箱
     */
    @Override
    public String selectEmailById(Long userId) {
        SysUser sysUser = userMapper.lambda()
            .select(SysUser::getEmail)
            .eq(SysUser::getUserId, userId)
            .one();
        return ObjectUtils.notNullGetter(sysUser, SysUser::getEmail);
    }

}
