package com.ym.agriculture.farmtask.employee.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.employee.model.bo.SysInviteCodeBo;
import com.ym.agriculture.farmtask.employee.model.entity.SysInviteCode;
import com.ym.agriculture.farmtask.employee.model.vo.InviteVerifyVo;
import com.ym.agriculture.farmtask.employee.model.vo.MiniProgramCodeVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysInviteCodeVo;

/**
 * 邀请码服务接口。
 */
public interface ISysInviteCodeService {

    /**
     * 分页查询角色邀请码。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 邀请码分页结果
     */
    PageResult<SysInviteCodeVo> queryPage(SysInviteCodeBo bo, PageQuery pageQuery);

    /**
     * 生成角色邀请码。
     *
     * @param bo 生成参数
     * @return 生成的邀请码
     */
    SysInviteCodeVo generateRoleInviteCode(SysInviteCodeBo bo);

    /**
     * 生成员工绑定码。
     *
     * @param employeeId 员工ID
     * @return 生成的绑定码
     */
    SysInviteCodeVo generateEmployeeBindCode(Long employeeId);

    /**
     * 校验角色邀请码。
     *
     * @param inviteCode 6位角色邀请码
     * @return 邀请码信息
     */
    InviteVerifyVo verifyRoleInviteCode(String inviteCode);

    /**
     * 校验角色邀请码（可识别已用该码注册过的微信用户再次进入）。
     *
     * @param inviteCode 6位角色邀请码
     * @param wxOpenid   微信 openid，非空时已注册用户可重复使用已达上限的邀请码
     * @return 邀请码信息
     */
    InviteVerifyVo verifyRoleInviteCode(String inviteCode, String wxOpenid);

    /**
     * 根据ID查询邀请码。
     *
     * @param codeId 邀请码ID
     * @return 邀请码信息
     */
    SysInviteCodeVo queryById(Long codeId);

    /**
     * 获取有效角色邀请码实体。
     *
     * @param inviteCode 邀请码
     * @return 邀请码实体
     */
    SysInviteCode requireValidRoleInviteCode(String inviteCode);

    /**
     * 获取有效角色邀请码实体（已用该码注册过的 openid 在次数用尽时仍可访问）。
     *
     * @param inviteCode 邀请码
     * @param wxOpenid   微信 openid
     * @return 邀请码实体
     */
    SysInviteCode requireValidRoleInviteCode(String inviteCode, String wxOpenid);

    /**
     * 软删除邀请码。
     *
     * @param codeId 邀请码ID
     * @return 影响行数
     */
    int deleteInviteCode(Long codeId);

    /**
     * 生成携带邀请码参数的小程序码。
     *
     * @param codeId 邀请码ID
     * @param bo     小程序码参数
     * @return 小程序码图片
     */
    MiniProgramCodeVo generateMiniProgramCode(Long codeId, SysInviteCodeBo bo);
}
