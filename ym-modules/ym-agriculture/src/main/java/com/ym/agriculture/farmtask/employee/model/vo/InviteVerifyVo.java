package com.ym.agriculture.farmtask.employee.model.vo;

import lombok.Data;

/**
 * 邀请码校验返回结果。
 */
@Data
public class InviteVerifyVo {

    /**
     * 邀请码ID。
     */
    private Long codeId;

    /**
     * 邀请码。
     */
    private String inviteCode;

    /**
     * 应用角色编码，对应 ym.employee.app-roles 配置中的 code。
     */
    private String appRoleCode;

    /**
     * 应用角色名称，对应 ym.employee.app-roles 配置中的 name。
     */
    private String appRoleName;

    /**
     * 人员类型：0内部 1外部。
     */
    private String personType;

    /**
     * 是否有效。
     */
    private Boolean valid;

    /**
     * 当前微信是否已使用该邀请码注册过（含待审核、已通过、已拒绝）。
     */
    private Boolean returningUser;

    /**
     * 老用户人员登录态，取值：login / bind_required / review_pending / review_rejected。
     */
    private String employeeLoginStatus;
}
