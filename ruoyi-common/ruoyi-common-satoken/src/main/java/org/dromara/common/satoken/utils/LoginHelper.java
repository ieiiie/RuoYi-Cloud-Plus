package org.dromara.common.satoken.utils;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ip.AddressUtils;
import org.dromara.system.api.model.LoginUser;

/**
 * 登录鉴权助手
 * <p>
 * user_type 为 用户类型 同一个用户表 可以有多种用户类型 例如 pc,app
 * deivce 为 设备类型 同一个用户类型 可以有 多种设备类型 例如 web,ios
 * 可以组成 用户类型与设备类型多对多的 权限灵活控制
 * <p>
 * 多用户体系 针对 多种用户类型 但权限控制不一致
 * 可以组成 多用户类型表与多设备类型 分别控制权限
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginHelper {

    public static final String LOGIN_USER_KEY = "loginUser";
    public static final String USER_KEY = "userId";
    public static final String GLOBAL_USER_KEY = "globalUserId";
    public static final String USER_NAME_KEY = "userName";
    public static final String TENANT_KEY = "tenantId";
    public static final String DEPT_KEY = "deptId";
    public static final String DEPT_NAME_KEY = "deptName";
    public static final String DEPT_CATEGORY_KEY = "deptCategory";
    public static final String CLIENT_KEY = "clientid";
    public static final String CLIENT_ACCESS_PATH_KEY = "clientAccessPath";
    public static final String CLIENT_IP_WHITELIST_KEY = "clientIpWhitelist";

    /**
     * 登录系统 基于 设备类型
     * 针对相同用户体系不同设备
     *
     * @param loginUser 登录用户信息
     * @param model     配置参数
     */
    public static void login(LoginUser loginUser, SaLoginParameter model) {
        model = ObjectUtil.defaultIfNull(model, new SaLoginParameter());
        fillRequestContext(loginUser, model);
        model.setExtra(USER_KEY, loginUser.getUserId())
            .setExtra(GLOBAL_USER_KEY, loginUser.getGlobalUserId())
            .setExtra(USER_NAME_KEY, loginUser.getUsername())
            .setExtra(DEPT_KEY, loginUser.getDeptId())
            .setExtra(DEPT_NAME_KEY, loginUser.getDeptName())
            .setExtra(DEPT_CATEGORY_KEY, loginUser.getDeptCategory());
        if (StringUtils.isNotBlank(loginUser.getTenantId())) {
            model.setExtra(TENANT_KEY, loginUser.getTenantId());
        }
        StpUtil.login(loginUser.getLoginId(), model);
        StpUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /**
     * 原子替换当前 token 会话中的有效租户用户。
     *
     * <p>Sa-Token 的登录 ID 在首次登录时生成，切换租户不能重新登录或换发 token；
     * 因此授权、菜单、数据权限等实时身份信息统一从 token session 内的
     * {@link LoginUser} 读取。切换时保留首次登录记录的客户端和终端信息。</p>
     *
     * @param loginUser 切换后的租户成员登录信息
     */
    public static void updateLoginUser(LoginUser loginUser) {
        SaSession session = StpUtil.getTokenSession();
        LoginUser previous = getLoginUser(session);
        if (ObjectUtil.isNotNull(previous)) {
            preserveSessionContext(previous, loginUser);
        }
        session.set(LOGIN_USER_KEY, loginUser);
    }

    private static void preserveSessionContext(LoginUser previous, LoginUser current) {
        if (StringUtils.isBlank(current.getClientKey())) {
            current.setClientKey(previous.getClientKey());
        }
        if (StringUtils.isBlank(current.getDeviceType())) {
            current.setDeviceType(previous.getDeviceType());
        }
        if (StringUtils.isBlank(current.getIpaddr())) {
            current.setIpaddr(previous.getIpaddr());
        }
        if (StringUtils.isBlank(current.getLoginLocation())) {
            current.setLoginLocation(previous.getLoginLocation());
        }
        if (StringUtils.isBlank(current.getBrowser())) {
            current.setBrowser(previous.getBrowser());
        }
        if (StringUtils.isBlank(current.getOs())) {
            current.setOs(previous.getOs());
        }
        if (ObjectUtil.isNull(current.getLoginTime())) {
            current.setLoginTime(previous.getLoginTime());
        }
        if (ObjectUtil.isNull(current.getExpireTime())) {
            current.setExpireTime(previous.getExpireTime());
        }
    }

    /**
     * 在登录时补充当前请求上下文，避免登录态中的终端信息缺失。
     *
     * @param loginUser 登录用户
     * @param model     登录参数
     */
    private static void fillRequestContext(LoginUser loginUser, SaLoginParameter model) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (ObjectUtil.isNull(request)) {
            return;
        }
        String ip = ServletUtils.getClientIP(request);
        if (StringUtils.isBlank(loginUser.getIpaddr())) {
            loginUser.setIpaddr(ip);
        }
        if (StringUtils.isBlank(loginUser.getLoginLocation()) && StringUtils.isNotBlank(ip)) {
            loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        }
        UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        if (StringUtils.isBlank(loginUser.getBrowser())) {
            loginUser.setBrowser(userAgent.getBrowser().getName());
        }
        if (StringUtils.isBlank(loginUser.getOs())) {
            loginUser.setOs(userAgent.getOs().getName());
        }
        if (StringUtils.isBlank(loginUser.getDeviceType()) && StringUtils.isNotBlank(model.getDeviceType())) {
            loginUser.setDeviceType(model.getDeviceType());
        }
    }

    /**
     * 获取用户(多级缓存)
     */
    public static <T extends LoginUser> T getLoginUser() {
        try {
            return getLoginUser(StpUtil.getTokenSession());
        } catch (NotLoginException e) {
            return null;
        }
    }

    /**
     * 获取用户基于token
     */
    public static <T extends LoginUser> T getLoginUser(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        SaSession session;
        try {
            session = StpUtil.getTokenSessionByToken(token);
        } catch (NotLoginException e) {
            return null;
        }
        return getLoginUser(session);
    }

    /**
     * 从会话中读取登录用户。
     *
     * @param session 登录会话
     * @return 登录用户
     */
    @SuppressWarnings("unchecked")
    private static <T extends LoginUser> T getLoginUser(SaSession session) {
        if (ObjectUtil.isNull(session)) {
            return null;
        }
        return (T) session.get(LOGIN_USER_KEY);
    }

    /**
     * 获取用户id
     */
    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        if (ObjectUtil.isNotNull(loginUser) && ObjectUtil.isNotNull(loginUser.getUserId())) {
            return loginUser.getUserId();
        }
        return Convert.toLong(getExtra(USER_KEY));
    }

    /**
     * 获取用户id
     */
    public static String getUserIdStr() {
        return Convert.toStr(getUserId());
    }

    /**
     * 获取当前全局账号ID。
     */
    public static Long getGlobalUserId() {
        LoginUser loginUser = getLoginUser();
        if (ObjectUtil.isNotNull(loginUser) && ObjectUtil.isNotNull(loginUser.getGlobalUserId())) {
            return loginUser.getGlobalUserId();
        }
        return Convert.toLong(getExtra(GLOBAL_USER_KEY));
    }

    /**
     * 获取用户账户
     */
    public static String getUsername() {
        LoginUser loginUser = getLoginUser();
        if (ObjectUtil.isNotNull(loginUser) && StringUtils.isNotBlank(loginUser.getUsername())) {
            return loginUser.getUsername();
        }
        return Convert.toStr(getExtra(USER_NAME_KEY));
    }

    /**
     * 获取租户编号。
     */
    public static String getTenantId() {
        LoginUser loginUser = getLoginUser();
        if (ObjectUtil.isNotNull(loginUser) && StringUtils.isNotBlank(loginUser.getTenantId())) {
            return loginUser.getTenantId();
        }
        return Convert.toStr(getExtra(TENANT_KEY));
    }

    /**
     * 获取部门ID
     */
    public static Long getDeptId() {
        LoginUser loginUser = getLoginUser();
        if (ObjectUtil.isNotNull(loginUser) && ObjectUtil.isNotNull(loginUser.getDeptId())) {
            return loginUser.getDeptId();
        }
        return Convert.toLong(getExtra(DEPT_KEY));
    }

    /**
     * 获取部门名
     */
    public static String getDeptName() {
        LoginUser loginUser = getLoginUser();
        if (ObjectUtil.isNotNull(loginUser) && StringUtils.isNotBlank(loginUser.getDeptName())) {
            return loginUser.getDeptName();
        }
        return Convert.toStr(getExtra(DEPT_NAME_KEY));
    }

    /**
     * 获取部门类别编码
     */
    public static String getDeptCategory() {
        LoginUser loginUser = getLoginUser();
        if (ObjectUtil.isNotNull(loginUser) && StringUtils.isNotBlank(loginUser.getDeptCategory())) {
            return loginUser.getDeptCategory();
        }
        return Convert.toStr(getExtra(DEPT_CATEGORY_KEY));
    }

    /**
     * 获取当前 Token 的扩展信息
     *
     * @param key 键值
     * @return 对应的扩展数据
     */
    private static Object getExtra(String key) {
        try {
            return StpUtil.getExtra(key);
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * 获取用户类型
     */
    public static UserType getUserType() {
        String loginType = StpUtil.getLoginIdAsString();
        return UserType.getUserType(loginType);
    }

    /**
     * 是否为超级管理员
     *
     * @param userId 用户ID
     * @return 结果
     */
    public static boolean isSuperAdmin(Long userId) {
        return SystemConstants.SUPER_ADMIN_USER_ID.equals(userId);
    }

    /**
     * 是否为超级管理员
     *
     * @return 结果
     */
    public static boolean isSuperAdmin() {
        return isSuperAdmin(getUserId());
    }

    /**
     * 检查当前用户是否已登录
     *
     * @return 结果
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

}
