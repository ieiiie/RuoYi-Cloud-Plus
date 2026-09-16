package com.ym.system.service;

import com.ym.system.domain.SysGlobalUser;
import com.ym.system.domain.SysUser;
import com.ym.system.domain.bo.SysGlobalUserBo;
import com.ym.system.domain.bo.SysUserProfileBo;
import com.ym.system.domain.vo.SysGlobalUserVo;

import java.util.List;

/**
 * 全局账号服务。
 */
public interface ISysGlobalUserService {

    /** 根据主键查询全局账号。 */
    SysGlobalUser queryById(Long globalUserId);

    /** 根据主键查询脱敏后的全局账号。 */
    SysGlobalUserVo queryVoById(Long globalUserId);

    /** 根据任一全局认证标识查询账号。 */
    SysGlobalUser queryByIdentifier(String identifier);

    /** 查询全局账号列表。 */
    List<SysGlobalUserVo> queryList(SysGlobalUserBo bo);

    /** 解析已有账号，或为首次添加人员创建账号。 */
    SysGlobalUser resolveOrCreate(SysGlobalUser candidate);

    /** 将允许镜像展示的全局资料复制到租户内用户，用于新增成员。 */
    void applyToTenantUser(SysGlobalUser globalUser, SysUser tenantUser);

    /** 修改全局账号资料并同步关联租户用户的展示资料。 */
    int updateGlobalUser(SysGlobalUserBo bo);

    /** 修改本人资料并同步关联租户用户的展示资料。 */
    int updateProfile(Long globalUserId, SysUserProfileBo profile);

    /** 修改全局账号密码。 */
    int resetPassword(Long globalUserId, String password);
}
