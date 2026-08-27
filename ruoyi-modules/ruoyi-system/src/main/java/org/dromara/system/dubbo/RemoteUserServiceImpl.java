package org.dromara.system.dubbo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.enums.UserStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.exception.user.UserException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ThreadUtils;
import org.dromara.common.mybatis.helper.DataPermissionHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.bo.RemoteUserBo;
import org.dromara.system.api.domain.vo.RemoteTenantUserVo;
import org.dromara.system.api.domain.vo.RemoteUserVo;
import org.dromara.system.api.model.LoginUser;
import org.dromara.system.api.model.PostDTO;
import org.dromara.system.api.model.RoleDTO;
import org.dromara.system.api.model.XcxLoginUser;
import org.dromara.system.domain.SysGlobalSocial;
import org.dromara.system.domain.SysGlobalUser;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserPost;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysTenantVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysGlobalSocialMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserPostMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.ISysConfigService;
import org.dromara.system.service.ISysDeptService;
import org.dromara.system.service.ISysGlobalUserService;
import org.dromara.system.service.ISysPermissionService;
import org.dromara.system.service.ISysPostService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysTenantService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 用户远程服务。
 *
 * <p>认证标识和密码只从 {@code sys_global_user} 获取；实际登录身份仍是当前租户
 * 的 {@code sys_user}，这样既保留原有角色、部门、岗位和数据权限，也能让一个
 * 账号进入多个租户。</p>
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteUserServiceImpl implements RemoteUserService {

    private final ISysUserService userService;
    private final ISysPermissionService permissionService;
    private final ISysConfigService configService;
    private final ISysRoleService roleService;
    private final ISysDeptService deptService;
    private final ISysPostService postService;
    private final ISysTenantService tenantService;
    private final ISysGlobalUserService globalUserService;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysGlobalSocialMapper globalSocialMapper;

    /**
     * 通过用户名（也允许手机号、邮箱）查询默认可登录租户。
     */
    @Override
    public LoginUser getUserInfo(String username) throws UserException {
        return getUserInfoByGlobalUserId(requireActiveGlobalUser(username).getGlobalUserId());
    }

    /**
     * 兼容旧调用方：先按全局账号定位，再进入调用方指定的租户。
     */
    @Override
    public LoginUser getUserInfo(String username, String tenantId) throws UserException {
        return getUserInfoByGlobalUserId(requireActiveGlobalUser(username).getGlobalUserId(), tenantId);
    }

    /**
     * 按当前租户内成员ID查询登录信息，供既有服务内部调用使用。
     */
    @Override
    public LoginUser getUserInfo(Long userId) throws UserException {
        SysUser tenantUser = userMapper.selectById(userId);
        if (ObjectUtil.isNull(tenantUser)) {
            throw new UserException("user.not.exists", StringUtils.EMPTY);
        }
        SysGlobalUser globalUser = requireActiveGlobalUser(tenantUser);
        return buildLoginUser(globalUser, requireActiveTenantUser(globalUser, tenantUser));
    }

    @Override
    public LoginUser getUserInfo(Long userId, String tenantId) throws UserException {
        return executeInTenant(tenantId, () -> getUserInfo(userId));
    }

    @Override
    public LoginUser getUserInfoByGlobalUserId(Long globalUserId) throws UserException {
        SysGlobalUser globalUser = requireActiveGlobalUser(globalUserId);
        List<SysUser> tenantUsers = TenantHelper.ignore(() -> userMapper.lambda()
            .eq(SysUser::getGlobalUserId, globalUserId)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SysUser::getCreateTime)
            .orderByAsc(SysUser::getUserId)
            .list());

        ServiceException unavailableTenant = null;
        for (SysUser tenantUser : tenantUsers) {
            try {
                tenantService.checkTenantAvailable(tenantUser.getTenantId());
            } catch (ServiceException e) {
                // 账号有多个成员时，跳过已停用或到期的租户，继续按创建时间选择。
                unavailableTenant = e;
                continue;
            }
            return buildLoginUser(globalUser, requireActiveTenantUser(globalUser, tenantUser));
        }
        if (ObjectUtil.isNotNull(unavailableTenant)) {
            throw unavailableTenant;
        }
        throw new UserException("user.blocked", globalUser.getUserName());
    }

    @Override
    public LoginUser getUserInfoByGlobalUserId(Long globalUserId, String tenantId) throws UserException {
        SysGlobalUser globalUser = requireActiveGlobalUser(globalUserId);
        if (StringUtils.isBlank(tenantId)) {
            return getUserInfoByGlobalUserId(globalUserId);
        }
        tenantService.checkTenantAvailable(tenantId);
        SysUser tenantUser = TenantHelper.dynamic(tenantId, () -> userMapper.lambda()
            .eq(SysUser::getGlobalUserId, globalUserId)
            .one());
        if (ObjectUtil.isNull(tenantUser)) {
            throw new ServiceException("当前账号不属于目标租户");
        }
        return buildLoginUser(globalUser, requireActiveTenantUser(globalUser, tenantUser));
    }

    @Override
    public List<RemoteTenantUserVo> listTenantUsers(Long globalUserId) {
        if (ObjectUtil.isNull(globalUserId)) {
            return List.of();
        }
        List<SysUser> tenantUsers = TenantHelper.ignore(() -> userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getTenantId, SysUser::getStatus, SysUser::getCreateTime)
            .eq(SysUser::getGlobalUserId, globalUserId)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SysUser::getCreateTime)
            .orderByAsc(SysUser::getUserId)
            .list());
        List<RemoteTenantUserVo> result = new ArrayList<>(tenantUsers.size());
        for (SysUser tenantUser : tenantUsers) {
            try {
                tenantService.checkTenantAvailable(tenantUser.getTenantId());
                SysTenantVo tenant = tenantService.queryByTenantId(tenantUser.getTenantId());
                if (ObjectUtil.isNull(tenant)) {
                    continue;
                }
                RemoteTenantUserVo item = new RemoteTenantUserVo();
                item.setTenantId(tenantUser.getTenantId());
                item.setTenantName(tenant.getCompanyName());
                item.setUserId(tenantUser.getUserId());
                result.add(item);
            } catch (ServiceException ignored) {
                // 已停用、过期或删除的租户不能出现在可切换列表中。
            }
        }
        return result;
    }

    /**
     * 通过手机号查询用户信息。
     */
    @Override
    public LoginUser getUserInfoByPhoneNumber(String phoneNumber) throws UserException {
        return getUserInfoByGlobalUserId(requireActiveGlobalUser(phoneNumber).getGlobalUserId());
    }

    @Override
    public LoginUser getUserInfoByPhoneNumber(String phoneNumber, String tenantId) throws UserException {
        return getUserInfoByGlobalUserId(requireActiveGlobalUser(phoneNumber).getGlobalUserId(), tenantId);
    }

    /**
     * 通过邮箱查询用户信息。
     */
    @Override
    public LoginUser getUserInfoByEmail(String email) throws UserException {
        return getUserInfoByGlobalUserId(requireActiveGlobalUser(email).getGlobalUserId());
    }

    @Override
    public LoginUser getUserInfoByEmail(String email, String tenantId) throws UserException {
        return getUserInfoByGlobalUserId(requireActiveGlobalUser(email).getGlobalUserId(), tenantId);
    }

    /**
     * 通过全局第三方绑定定位小程序账号。
     */
    @Override
    public XcxLoginUser getUserInfoByOpenid(String openid) throws UserException {
        SysGlobalSocial social = findGlobalSocialByOpenid(openid);
        LoginUser loginUser = getUserInfoByGlobalUserId(social.getGlobalUserId());
        return toXcxLoginUser(loginUser, openid);
    }

    @Override
    public XcxLoginUser getUserInfoByOpenid(String openid, String tenantId) throws UserException {
        SysGlobalSocial social = findGlobalSocialByOpenid(openid);
        LoginUser loginUser = getUserInfoByGlobalUserId(social.getGlobalUserId(), tenantId);
        return toXcxLoginUser(loginUser, openid);
    }

    /**
     * 注册仅能创建新全局账号，不能借由匿名注册把已有账号加入租户。
     */
    @Override
    public Boolean registerUserInfo(RemoteUserBo remoteUserBo) throws UserException, ServiceException {
        return executeInTenant(remoteUserBo.getTenantId(), () -> registerUserInfoInternal(remoteUserBo));
    }

    private Boolean registerUserInfoInternal(RemoteUserBo remoteUserBo) {
        SysUserBo sysUserBo = MapstructUtils.convert(remoteUserBo, SysUserBo.class);
        String username = sysUserBo.getUserName();
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser")))) {
            throw new ServiceException("当前系统没有开启注册功能");
        }
        if (ObjectUtil.isNotNull(globalUserService.queryByIdentifier(username))) {
            throw new UserException("user.register.save.error", username);
        }
        return userService.registerUser(sysUserBo);
    }

    @Override
    public String selectUserNameById(Long userId) {
        return userService.selectUserNameById(userId);
    }

    @Override
    public String selectNicknameById(Long userId) {
        return userService.selectNicknameById(userId);
    }

    @Override
    public String selectNicknameByIds(String userIds) {
        return userService.selectNicknameByIds(userIds);
    }

    @Override
    public String selectPhonenumberById(Long userId) {
        return userService.selectPhonenumberById(userId);
    }

    @Override
    public String selectEmailById(Long userId) {
        return userService.selectEmailById(userId);
    }

    /**
     * 构建当前租户的登录用户，并在同一租户上下文加载部门、权限、角色和岗位。
     */
    private LoginUser buildLoginUser(SysGlobalUser globalUser, SysUser tenantUser) {
        return TenantHelper.dynamic(tenantUser.getTenantId(), () -> {
            LoginUser loginUser = new LoginUser();
            Long userId = tenantUser.getUserId();
            loginUser.setUserId(userId);
            loginUser.setGlobalUserId(globalUser.getGlobalUserId());
            loginUser.setTenantId(tenantUser.getTenantId());
            SysTenantVo tenant = tenantService.queryByTenantId(tenantUser.getTenantId());
            loginUser.setTenantName(tenant == null ? StringUtils.EMPTY : tenant.getCompanyName());
            loginUser.setDeptId(tenantUser.getDeptId());
            loginUser.setUsername(globalUser.getUserName());
            loginUser.setNickname(globalUser.getNickName());
            loginUser.setPassword(globalUser.getPassword());
            loginUser.setUserType(globalUser.getUserType());
            if (ObjectUtil.isNotNull(tenantUser.getDeptId())) {
                Opt<SysDeptVo> deptOpt = Opt.of(tenantUser.getDeptId()).map(deptService::selectDeptById);
                loginUser.setDeptName(deptOpt.map(SysDeptVo::getDeptName).orElse(StringUtils.EMPTY));
                loginUser.setDeptCategory(deptOpt.map(SysDeptVo::getDeptCategory).orElse(StringUtils.EMPTY));
            }
            String tenantId = tenantUser.getTenantId();
            ThreadUtils.virtualSubmit(() -> TenantHelper.dynamic(tenantId,
                () -> loginUser.setMenuPermission(permissionService.getMenuPermission(userId))),
                () -> TenantHelper.dynamic(tenantId,
                    () -> loginUser.setRolePermission(permissionService.getRolePermission(userId))),
                () -> TenantHelper.dynamic(tenantId, () -> {
                    List<SysRoleVo> roles = roleService.selectRolesByUserId(userId);
                    List<RoleDTO> roleDtos = BeanUtil.copyToList(roles, RoleDTO.class);
                    loginUser.setRoles(roleDtos);
                    loginUser.setDataScopeRoleMap(permissionService.getDataScopeRoleMap(roleDtos));
                }),
                () -> TenantHelper.dynamic(tenantId, () -> {
                    List<SysPostVo> posts = postService.selectPostsByUserId(userId);
                    loginUser.setPosts(BeanUtil.copyToList(posts, PostDTO.class));
                }));
            return loginUser;
        });
    }

    private XcxLoginUser toXcxLoginUser(LoginUser loginUser, String openid) {
        XcxLoginUser result = new XcxLoginUser();
        BeanUtil.copyProperties(loginUser, result);
        result.setOpenid(openid);
        return result;
    }

    private SysGlobalSocial findGlobalSocialByOpenid(String openid) {
        SysGlobalSocial social = TenantHelper.ignore(() -> globalSocialMapper.lambda()
            .eq(SysGlobalSocial::getOpenId, openid)
            .one());
        if (ObjectUtil.isNull(social)) {
            throw new UserException("user.not.exists", openid);
        }
        return social;
    }

    private SysGlobalUser requireActiveGlobalUser(String identifier) {
        SysGlobalUser globalUser = globalUserService.queryByIdentifier(identifier);
        if (ObjectUtil.isNull(globalUser)) {
            throw new UserException("user.not.exists", identifier);
        }
        return requireActiveGlobalUser(globalUser);
    }

    private SysGlobalUser requireActiveGlobalUser(Long globalUserId) {
        SysGlobalUser globalUser = globalUserService.queryById(globalUserId);
        if (ObjectUtil.isNull(globalUser)) {
            throw new UserException("user.not.exists", StringUtils.EMPTY);
        }
        return requireActiveGlobalUser(globalUser);
    }

    private SysGlobalUser requireActiveGlobalUser(SysUser tenantUser) {
        if (ObjectUtil.isNull(tenantUser.getGlobalUserId())) {
            throw new ServiceException("租户成员未关联全局账号");
        }
        return requireActiveGlobalUser(tenantUser.getGlobalUserId());
    }

    private SysGlobalUser requireActiveGlobalUser(SysGlobalUser globalUser) {
        if (UserStatus.DISABLE.getCode().equals(globalUser.getStatus())) {
            throw new UserException("user.blocked", globalUser.getUserName());
        }
        return globalUser;
    }

    private SysUser requireActiveTenantUser(SysGlobalUser globalUser, SysUser tenantUser) {
        if (UserStatus.DISABLE.getCode().equals(tenantUser.getStatus())) {
            throw new UserException("user.blocked", globalUser.getUserName());
        }
        return tenantUser;
    }

    /**
     * 更新当前租户成员的登录信息。
     */
    @Override
    public void recordLoginInfo(Long userId, String ip) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(ip);
        sysUser.setLoginDate(LocalDateTime.now());
        sysUser.setUpdateBy(userId);
        DataPermissionHelper.ignore(() -> userMapper.updateById(sysUser));
    }

    @Override
    public void recordLoginInfo(Long userId, String ip, String tenantId) {
        executeInTenant(tenantId, () -> {
            recordLoginInfo(userId, ip);
            return null;
        });
    }

    /**
     * 用于注册和兼容旧调用的明确租户上下文。
     */
    private <T> T executeInTenant(String tenantId, Supplier<T> action) {
        tenantService.checkTenantAvailable(tenantId);
        return TenantHelper.dynamic(tenantId, action);
    }

    @Override
    public List<RemoteUserVo> selectListByIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        List<SysUserVo> list = userMapper.selectActiveUserVoListByIds(userIds);
        return MapstructUtils.convert(list, RemoteUserVo.class);
    }

    @Override
    public List<Long> selectUserIdsByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return List.of();
        }
        return userService.selectUserIdsByRoleIds(roleIds);
    }

    @Override
    public List<RemoteUserVo> selectUsersByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return List.of();
        }
        List<SysUserRole> userRoles = userRoleMapper.lambda()
            .in(SysUserRole::getRoleId, roleIds)
            .list();
        Set<Long> userIds = StreamUtils.toSet(userRoles, SysUserRole::getUserId);
        return selectListByIds(new ArrayList<>(userIds));
    }

    @Override
    public List<RemoteUserVo> selectUsersByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return List.of();
        }
        List<SysUserVo> list = userMapper.selectActiveUserVoListByDeptIds(deptIds);
        return BeanUtil.copyToList(list, RemoteUserVo.class);
    }

    @Override
    public List<RemoteUserVo> selectUsersByPostIds(Collection<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return List.of();
        }
        List<SysUserPost> userPosts = userPostMapper.lambda()
            .in(SysUserPost::getPostId, postIds)
            .list();
        Set<Long> userIds = StreamUtils.toSet(userPosts, SysUserPost::getUserId);
        return selectListByIds(new ArrayList<>(userIds));
    }

    @Override
    public Map<Long, String> selectUserNicksByIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Map.of();
        }
        List<SysUser> list = userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getNickName)
            .in(SysUser::getUserId, userIds)
            .list();
        return StreamUtils.toMap(list, SysUser::getUserId, SysUser::getNickName);
    }
}
