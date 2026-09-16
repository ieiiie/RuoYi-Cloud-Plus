package com.ym.agriculture.farmtask.employee.model.vo;

import com.ym.agriculture.farmtask.employee.model.entity.SysEmployee;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 员工视图对象 sys_employee。
 */
@Data
@AutoMapper(target = SysEmployee.class)
public class SysEmployeeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 员工ID。
     */
    private Long employeeId;

    /**
     * 租户ID。
     */
    private String tenantId;

    /**
     * 关联 sys_user.user_id。
     */
    private Long userId;

    /**
     * 登录账号。
     */
    private String userName;

    /**
     * 姓名。
     */
    private String name;

    /**
     * 手机号。
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
     * 微信 openid。
     */
    private String wxOpenid;

    /**
     * 邀请码ID。
     */
    private Long inviteCodeId;

    /**
     * 最近有效绑定码或注册邀请码。
     */
    private String inviteCode;

    /**
     * 邀请码类型：1角色邀请码（方案二） 2人员绑定码（方案一）。
     */
    private String codeType;

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
     * 应用角色编码，对应 ym.employee.app-roles 配置中的 code。
     */
    private String appRoleCode;

    /**
     * 应用角色名称，对应 ym.employee.app-roles 配置中的 name。
     */
    private String appRoleName;

    /**
     * 旧的小程序注册审批系统角色ID，仅兼容历史客户端。
     */
    private Long miniappRegisterApprovalRoleId;

    /**
     * 小程序绑定的系统角色ID集合。
     */
    private List<Long> miniappRoleIds;

    /**
     * 状态：0正常 1停用。
     */
    private String status;

    /**
     * 审核状态：0待审核 1通过 2拒绝。
     */
    private String reviewStatus;

    /**
     * 审核人ID。
     */
    private Long reviewBy;

    /**
     * 审核时间。
     */
    private Date reviewTime;

    /**
     * 审核备注。
     */
    private String reviewRemark;

    /**
     * 提交设备信息。
     */
    private String submitDevice;

    /**
     * 绑定状态：0未绑定 1已绑定。
     */
    private String bindStatus;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 备注。
     */
    private String remark;
}
