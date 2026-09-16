package com.ym.system.mapper;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.annotation.DataColumn;
import com.ym.common.mybatis.annotation.DataPermission;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.common.mybatis.core.query.LambdaJoinQueryBuilder;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.system.domain.SysDept;
import com.ym.system.domain.SysGlobalUser;
import com.ym.system.domain.SysUser;
import com.ym.system.domain.bo.SysUserBo;
import com.ym.system.domain.vo.SysUserExportVo;
import com.ym.system.domain.vo.SysUserVo;

import java.util.Collection;
import java.util.List;

/**
 * 用户表 数据层。
 *
 * <p>租户成员表不保存用户名、手机号和密码；所有需要展示或筛选这两个认证标识的
 * 查询都通过 {@code global_user_id} 关联 {@code sys_global_user}。</p>
 *
 * @author Lion Li
 */
public interface SysUserMapper extends BaseMapperPlus<SysUser, SysUserVo>, MPJBaseMapper<SysUser> {

    /**
     * 物理删除当前租户中的成员记录。
     *
     * <p>全局账号关联采用 {@code (tenant_id, global_user_id)} 唯一约束。成员移除后
     * 必须释放该关系，才能将同一全局账号再次添加到当前租户；租户行拦截器仍会为
     * 此语句追加当前 {@code tenant_id} 条件。</p>
     *
     * @param userId 租户内成员ID
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_user WHERE user_id = #{userId}")
    int deleteTenantMemberById(@Param("userId") Long userId);

    /**
     * 分页查询用户列表，并进行数据权限控制。
     *
     * @param page    分页参数
     * @param user    查询条件
     * @param deptIds 部门及子部门ID集合
     * @return 分页的用户信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.create_by")
    })
    default Page<SysUserVo> selectPageUserList(Page<SysUserVo> page, SysUserBo user, List<Long> deptIds) {
        MPJLambdaWrapper<SysUser> wrapper = buildUserGlobalJoinBuilder()
            .eqIfPresent("u", SysUser::getUserId, user.getUserId())
            .in(StringUtils.isNotBlank(user.getUserIds()), "u", SysUser::getUserId,
                StringUtils.splitTo(user.getUserIds(), Convert::toLong))
            .likeIfText("gu", SysGlobalUser::getUserName, user.getUserName())
            .likeIfText("u", SysUser::getNickName, user.getNickName())
            .eqIfText("u", SysUser::getStatus, user.getStatus())
            .likeIfText("gu", SysGlobalUser::getPhoneNumber, user.getPhoneNumber())
            .betweenParams("u", SysUser::getCreateTime, user.getParams(), "beginTime", "endTime")
            .inIfNotEmpty("u", SysUser::getDeptId, deptIds)
            .notIn(StringUtils.isNotBlank(user.getExcludeUserIds()), "u", SysUser::getUserId,
                StringUtils.splitTo(user.getExcludeUserIds(), Convert::toLong))
            .orderByAsc("u", SysUser::getUserId)
            .build();
        return this.selectJoinPage(page, SysUserVo.class, wrapper);
    }

    /**
     * 角色工作台查询当前租户全部用户，不叠加当前操作者的数据范围过滤。
     */
    default Page<SysUserVo> selectRoleAuthUserPage(Page<SysUserVo> page, SysUserBo user) {
        MPJLambdaWrapper<SysUser> wrapper = buildUserGlobalJoinBuilder()
            .likeIfText("gu", SysGlobalUser::getUserName, user.getUserName())
            .likeIfText("u", SysUser::getNickName, user.getNickName())
            .likeIfText("gu", SysGlobalUser::getPhoneNumber, user.getPhoneNumber())
            .eqIfText("u", SysUser::getStatus, user.getStatus())
            .orderByAsc("u", SysUser::getUserId)
            .build();
        return this.selectJoinPage(page, SysUserVo.class, wrapper);
    }

    /**
     * 根据条件查询用户导出数据，并进行数据权限控制。
     *
     * @param user    查询条件
     * @param deptIds 部门ID集合
     * @return 用户信息集合
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.create_by")
    })
    default List<SysUserExportVo> selectUserExportList(SysUserBo user, List<Long> deptIds) {
        MPJLambdaWrapper<SysUser> wrapper = buildUserGlobalJoinBuilder()
            .leftJoin(SysUser.class, "u1", SysUser::getUserId, "d", SysDept::getLeader)
            .leftJoin(SysGlobalUser.class, "gu1", SysGlobalUser::getGlobalUserId, "u1", SysUser::getGlobalUserId)
            .selectAs("gu", SysGlobalUser::getUserName, SysUserExportVo::getUserName)
            .selectAs("gu", SysGlobalUser::getPhoneNumber, SysUserExportVo::getPhoneNumber)
            .selectAs("gu1", SysGlobalUser::getUserName, SysUserExportVo::getLeaderName)
            .likeIfText("gu", SysGlobalUser::getUserName, user.getUserName())
            .likeIfText("u", SysUser::getNickName, user.getNickName())
            .eqIfText("u", SysUser::getStatus, user.getStatus())
            .likeIfText("gu", SysGlobalUser::getPhoneNumber, user.getPhoneNumber())
            .betweenParams("u", SysUser::getCreateTime, user.getParams(), "beginTime", "endTime")
            .inIfNotEmpty("u", SysUser::getDeptId, deptIds)
            .orderByAsc("u", SysUser::getUserId)
            .build();
        return this.selectJoinList(SysUserExportVo.class, wrapper);
    }

    /**
     * 按用户名查询当前租户成员。
     *
     * @param userName 全局用户名
     * @return 租户成员视图
     */
    default SysUserVo selectUserByUserName(String userName) {
        return this.selectJoinOne(SysUserVo.class, buildUserGlobalJoinBuilder()
            .eq("gu", SysGlobalUser::getUserName, userName)
            .build());
    }

    /**
     * 按手机号查询当前租户成员。
     *
     * @param phoneNumber 全局手机号
     * @return 租户成员视图
     */
    default SysUserVo selectUserByPhoneNumber(String phoneNumber) {
        return this.selectJoinOne(SysUserVo.class, buildUserGlobalJoinBuilder()
            .eq("gu", SysGlobalUser::getPhoneNumber, phoneNumber)
            .build());
    }

    /**
     * 按租户成员ID查询包含全局认证资料的用户视图。
     *
     * @param userId 租户成员ID
     * @return 用户视图
     */
    default SysUserVo selectUserVoById(Long userId) {
        return this.selectJoinOne(SysUserVo.class, buildUserGlobalJoinBuilder()
            .eq("u", SysUser::getUserId, userId)
            .build());
    }

    /**
     * 根据用户ID和部门筛选当前租户可选用户，并进行数据权限控制。
     *
     * @param userIds 用户ID集合
     * @param deptId  部门ID
     * @return 用户视图集合
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.create_by")
    })
    default List<SysUserVo> selectUserByIds(List<Long> userIds, Long deptId) {
        return this.selectJoinList(SysUserVo.class, buildUserGlobalJoinBuilder()
            .eq("u", SysUser::getStatus, SystemConstants.NORMAL)
            .eqIfPresent("u", SysUser::getDeptId, deptId)
            .inIfNotEmpty("u", SysUser::getUserId, userIds)
            .build());
    }

    /**
     * 查询当前租户指定成员的全局资料视图。
     *
     * @param userIds 成员ID集合
     * @return 用户视图集合
     */
    default List<SysUserVo> selectActiveUserVoListByIds(Collection<Long> userIds) {
        return this.selectJoinList(SysUserVo.class, buildUserGlobalJoinBuilder()
            .eq("u", SysUser::getStatus, SystemConstants.NORMAL)
            .inIfNotEmpty("u", SysUser::getUserId, userIds)
            .build());
    }

    /**
     * 查询当前租户指定部门的成员全局资料视图。
     *
     * @param deptIds 部门ID集合
     * @return 用户视图集合
     */
    default List<SysUserVo> selectActiveUserVoListByDeptIds(Collection<Long> deptIds) {
        return this.selectJoinList(SysUserVo.class, buildUserGlobalJoinBuilder()
            .eq("u", SysUser::getStatus, SystemConstants.NORMAL)
            .inIfNotEmpty("u", SysUser::getDeptId, deptIds)
            .build());
    }

    /**
     * 查询当前租户一个部门下的成员全局资料视图。
     *
     * @param deptId 部门ID
     * @return 用户视图集合
     */
    default List<SysUserVo> selectUserVoListByDeptId(Long deptId) {
        return this.selectJoinList(SysUserVo.class, buildUserGlobalJoinBuilder()
            .eq("u", SysUser::getDeptId, deptId)
            .orderByAsc("u", SysUser::getUserId)
            .build());
    }

    /**
     * 根据用户ID统计用户数量。
     *
     * @param userId 用户ID
     * @return 用户数量
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default long countUserById(Long userId) {
        return lambda().eq(SysUser::getUserId, userId).count();
    }

    /**
     * 根据条件更新用户数据。
     *
     * @param user          要更新的用户实体
     * @param updateWrapper 更新条件封装器
     * @return 更新操作影响的行数
     */
    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    int update(@Param(Constants.ENTITY) SysUser user, @Param(Constants.WRAPPER) Wrapper<SysUser> updateWrapper);

    /**
     * 根据用户ID更新用户数据。
     *
     * @param user 要更新的用户实体
     * @return 更新操作影响的行数
     */
    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    int updateById(@Param(Constants.ENTITY) SysUser user);

    /**
     * 构建租户成员与全局账号的联表条件。
     *
     * <p>选择 {@code sys_user} 的全部租户属性，并将全局用户名、手机号投影到
     * {@link SysUserVo}，从根源上避免 SQL 再访问已移除的本地凭据列。</p>
     *
     * @return 用户联表查询包装器
     */
    default LambdaJoinQueryBuilder<SysUser> buildUserGlobalJoinBuilder() {
        return QueryBuilder.lambdaJoin("u", SysUser.class)
            .selectAll(SysUser.class)
            .selectAs("gu", SysGlobalUser::getUserName, SysUserVo::getUserName)
            .selectAs("gu", SysGlobalUser::getPhoneNumber, SysUserVo::getPhoneNumber)
            .leftJoin(SysGlobalUser.class, "gu", SysGlobalUser::getGlobalUserId, "u", SysUser::getGlobalUserId)
            .leftJoin(SysDept.class, "d", SysDept::getDeptId, "u", SysUser::getDeptId)
            .eq("u", SysUser::getDelFlag, SystemConstants.NORMAL);
    }

}
