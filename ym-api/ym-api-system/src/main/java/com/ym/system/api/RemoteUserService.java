package com.ym.system.api;

import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.exception.user.UserException;
import com.ym.system.api.domain.bo.RemoteUserBo;
import com.ym.system.api.domain.bo.RemoteUserProfileUpdateBo;
import com.ym.system.api.domain.vo.RemoteTenantUserVo;
import com.ym.system.api.domain.vo.RemoteUserVo;
import com.ym.system.api.model.LoginUser;
import com.ym.system.api.model.XcxLoginUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户服务
 *
 * @author Lion Li
 */
public interface RemoteUserService {

    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @return 结果
     */
    LoginUser getUserInfo(String username) throws UserException;

    /**
     * 通过用户名和租户编号查询用户信息。
     *
     * <p>保留旧方法以兼容已有消费者；新的全局账号登录入口不应传入租户编号。</p>
     *
     * @param username 用户名
     * @param tenantId 租户编号
     * @return 结果
     */
    default LoginUser getUserInfo(String username, String tenantId) throws UserException {
        return getUserInfo(username);
    }

    /**
     * 通过用户id查询用户信息
     *
     * @param userId 用户id
     * @return 结果
     */
    LoginUser getUserInfo(Long userId) throws UserException;

    /**
     * 通过用户 id 和租户编号查询用户信息。
     */
    default LoginUser getUserInfo(Long userId, String tenantId) throws UserException {
        return getUserInfo(userId);
    }

    /**
     * 通过全局账号ID查询默认可登录租户中的成员信息。
     *
     * <p>默认租户按本账号正常成员的 {@code create_time ASC, user_id ASC}
     * 选择，并跳过已停用或已过期的租户。</p>
     */
    LoginUser getUserInfoByGlobalUserId(Long globalUserId) throws UserException;

    /**
     * 通过全局账号ID查询指定租户中的成员信息。
     */
    LoginUser getUserInfoByGlobalUserId(Long globalUserId, String tenantId) throws UserException;

    /**
     * 查询全局账号可进入的租户成员列表。
     */
    List<RemoteTenantUserVo> listTenantUsers(Long globalUserId);

    /**
     * 通过手机号查询用户信息
     *
     * @param phoneNumber 手机号
     * @return 结果
     */
    LoginUser getUserInfoByPhoneNumber(String phoneNumber) throws UserException;

    /**
     * 通过手机号和租户编号查询用户信息。
     */
    default LoginUser getUserInfoByPhoneNumber(String phoneNumber, String tenantId) throws UserException {
        return getUserInfoByPhoneNumber(phoneNumber);
    }

    /**
     * 通过邮箱查询用户信息
     *
     * @param email 邮箱
     * @return 结果
     */
    LoginUser getUserInfoByEmail(String email) throws UserException;

    /**
     * 通过邮箱和租户编号查询用户信息。
     */
    default LoginUser getUserInfoByEmail(String email, String tenantId) throws UserException {
        return getUserInfoByEmail(email);
    }

    /**
     * 通过openid查询用户信息
     *
     * @param openid openid
     * @return 结果
     */
    XcxLoginUser getUserInfoByOpenid(String openid) throws UserException;

    /**
     * 通过 openid 和租户编号查询用户信息。
     */
    default XcxLoginUser getUserInfoByOpenid(String openid, String tenantId) throws UserException {
        return getUserInfoByOpenid(openid);
    }

    /**
     * 注册用户信息
     *
     * @param remoteUserBo 用户信息
     * @return 结果
     */
    Boolean registerUserInfo(RemoteUserBo remoteUserBo) throws UserException, ServiceException;

    /**
     * 通过userId查询用户账户
     *
     * @param userId 用户id
     * @return 结果
     */
    String selectUserNameById(Long userId);

    /**
     * 通过用户ID查询用户昵称
     *
     * @param userId 用户ID
     * @return 用户昵称
     */
    String selectNicknameById(Long userId);

    /**
     * 通过用户ID查询用户昵称
     *
     * @param userIds 用户ID 多个用逗号隔开
     * @return 用户昵称
     */
    String selectNicknameByIds(String userIds);

    /**
     * 通过用户ID查询用户手机号
     *
     * @param userId 用户id
     * @return 用户手机号
     */
    String selectPhonenumberById(Long userId);

    /**
     * 通过用户ID查询用户邮箱
     *
     * @param userId 用户id
     * @return 用户邮箱
     */
    String selectEmailById(Long userId);

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param ip     IP地址
     */
    void recordLoginInfo(Long userId, String ip);

    /**
     * 按租户记录登录信息。
     */
    default void recordLoginInfo(Long userId, String ip, String tenantId) {
        recordLoginInfo(userId, ip);
    }

    /**
     * 通过用户ID查询用户列表
     *
     * @param userIds 用户ids
     * @return 用户列表
     */
    List<RemoteUserVo> selectListByIds(Collection<Long> userIds);

    /**
     * 通过角色ID查询用户ID
     *
     * @param roleIds 角色ids
     * @return 用户ids
     */
    List<Long> selectUserIdsByRoleIds(Collection<Long> roleIds);

    /**
     * 通过角色ID查询用户
     *
     * @param roleIds 角色ids
     * @return 用户
     */
    List<RemoteUserVo> selectUsersByRoleIds(Collection<Long> roleIds);

    /**
     * 通过部门ID查询用户
     *
     * @param deptIds 部门ids
     * @return 用户
     */
    List<RemoteUserVo> selectUsersByDeptIds(Collection<Long> deptIds);

    /**
     * 通过岗位ID查询用户
     *
     * @param postIds 岗位ids
     * @return 用户
     */
    List<RemoteUserVo> selectUsersByPostIds(Collection<Long> postIds);

    /**
     * 根据用户 ID 列表查询用户昵称映射关系
     *
     * @param userIds 用户 ID 列表
     * @return Map，其中 key 为用户 ID，value 为对应的用户昵称
     */
    Map<Long, String> selectUserNicksByIds(Collection<Long> userIds);

    /** 幂等同步平台账号基础档案。 */
    boolean updateUserProfile(RemoteUserProfileUpdateBo command);

}
