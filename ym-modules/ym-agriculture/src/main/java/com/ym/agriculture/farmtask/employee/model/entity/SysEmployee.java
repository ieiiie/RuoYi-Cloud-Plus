package com.ym.agriculture.farmtask.employee.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 平台员工主数据对象 sys_employee。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_employee")
public class SysEmployee extends TenantEntity {

    /**
     * 员工ID。
     */
    @TableId(value = "employee_id")
    private Long employeeId;

    /**
     * 关联 sys_user.user_id，外部人员为空。
     */
    private Long userId;

    /**
     * 登录账号，关联系统用户时冗余 sys_user.user_name。
     */
    private String userName;

    /**
     * 姓名，2-20 个字符。
     */
    private String name;

    /**
     * 手机号，11 位手机号。
     */
    private String phone;

    /**
     * 身份证号。
     */
    private String idCard;

    /**
     * 微信授权手机号。
     */
    private String wxPhone;

    /**
     * 微信 openid，用于小程序免登录。
     */
    private String wxOpenid;

    /**
     * 使用的绑定邀请码ID，方案一使用。
     */
    private Long inviteCodeId;

    /**
     * 性别：0男 1女 2未知。
     */
    private String gender;

    /**
     * 出生日期。
     */
    private Date birthDate;

    /**
     * 照片 OSS URL。
     */
    private String photo;

    /**
     * 人员类型：0内部 1外部。
     */
    private String personType;

    /**
     * 应用角色编码。
     */
    private String appRoleCode;

    /**
     * 小程序注册审批系统角色ID；为空表示未授权审批。
     */
    private Long miniappRegisterApprovalRoleId;

    /**
     * 状态：0正常 1停用。
     */
    private String status;

    /**
     * 审核状态：0待审核 1通过 2拒绝；后台录入无需审核时为空。
     */
    private String reviewStatus;

    /**
     * 审核人ID，关联 sys_user.user_id。
     */
    private Long reviewBy;

    /**
     * 审核时间。
     */
    private Date reviewTime;

    /**
     * 审核备注，通过或不通过原因，最多 200 字。
     */
    private String reviewRemark;

    /**
     * 提交设备信息，小程序端获取。
     */
    private String submitDevice;

    /**
     * 删除标志：0存在 1删除。
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注。
     */
    private String remark;
}
