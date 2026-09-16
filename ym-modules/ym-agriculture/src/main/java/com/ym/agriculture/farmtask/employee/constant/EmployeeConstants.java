package com.ym.agriculture.farmtask.employee.constant;

/**
 * 员工与邀请码业务常量。
 */
public interface EmployeeConstants {

    /**
     * 人员类型：内部人员。
     */
    String PERSON_TYPE_INTERNAL = "0";

    /**
     * 人员类型：外部人员。
     */
    String PERSON_TYPE_EXTERNAL = "1";

    /**
     * 审核状态：待审核。
     */
    String REVIEW_PENDING = "0";

    /**
     * 审核状态：审核通过。
     */
    String REVIEW_APPROVED = "1";

    /**
     * 审核状态：审核拒绝。
     */
    String REVIEW_REJECTED = "2";

    /**
     * 邀请码类型：角色邀请码。
     */
    String INVITE_CODE_TYPE_ROLE = "1";

    /**
     * 邀请码类型：人员绑定码。
     */
    String INVITE_CODE_TYPE_EMPLOYEE = "2";

    /**
     * 邀请码状态：有效。
     */
    String INVITE_STATUS_VALID = "1";

    /**
     * 邀请码状态：停用。
     */
    String INVITE_STATUS_DISABLED = "0";

    /**
     * 绑定状态：未绑定。
     */
    String BIND_STATUS_UNBOUND = "0";

    /**
     * 绑定状态：已绑定。
     */
    String BIND_STATUS_BOUND = "1";

    /**
     * stask 生产管理员角色编码。
     */
    String APP_ROLE_STASK_PRODUCTION_ADMIN = "stask:production_admin";

    /**
     * stask 专家 / 技术员角色编码。
     */
    String APP_ROLE_STASK_EXPERT = "stask:expert";

    /**
     * stask 农事分配组长角色编码。
     */
    String APP_ROLE_STASK_GROUP_LEADER = "stask:group_leader";

    /**
     * stask 领导角色编码。
     */
    String APP_ROLE_STASK_LEADER = "stask:leader";

    /**
     * stask 工人角色编码。
     */
    String APP_ROLE_STASK_WORKER = "stask:worker";

    /**
     * stask 库管角色编码；只允许后台授予，不允许注册或岗位邀请码自选。
     */
    String APP_ROLE_STASK_WAREHOUSE_KEEPER = "stask:warehouse_keeper";

    /**
     * 小程序注册审批权限标识。
     */
    String PERMISSION_MINIAPP_REGISTER_APPROVE = "miniapp:register:approve";

    /** 小程序巡棚拍照入口权限标识。 */
    String PERMISSION_MINIAPP_INSPECTION_PHOTO_ACCESS = "miniapp:inspection-photo:access";

    /**
     * 微信外部人员登录用户类型。
     */
    String USER_TYPE_WX_EMPLOYEE = "wx_user";
}
