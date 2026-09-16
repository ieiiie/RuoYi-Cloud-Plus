package com.ym.agriculture.farmtask.employee.model.bo;

import com.ym.common.mybatis.core.domain.BaseEntity;
import com.ym.agriculture.farmtask.employee.model.entity.SysEmployee;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 员工业务对象 sys_employee。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysEmployee.class, reverseConvertGenerate = false)
public class SysEmployeeBo extends BaseEntity {

    /**
     * 员工ID。
     */
    private Long employeeId;

    /**
     * 关联 sys_user.user_id。
     */
    private Long userId;

    /**
     * 登录账号。
     */
    private String userName;

    /**
     * 姓名，2-20 个字符。
     */
    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 20, message = "姓名长度必须在{min}到{max}个字符之间")
    private String name;

    /**
     * 手机号，11 位手机号。
     */
    @NotBlank(message = "手机号不能为空")
    @Size(min = 11, max = 11, message = "手机号必须为11位")
    private String phone;

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
     * 绑定码或角色邀请码。
     */
    private String inviteCode;

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
    @NotBlank(message = "小程序角色不能为空")
    private String appRoleCode;

    /**
     * 旧的小程序注册审批系统角色ID，仅兼容历史调用。
     */
    private Long miniappRegisterApprovalRoleId;

    /**
     * 小程序绑定的系统角色ID集合；由后台角色权限决定可用功能。
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
     * 审核结果：1通过 2拒绝。
     */
    private String reviewResult;

    /**
     * 审核备注，最多 200 字。
     */
    @Size(max = 200, message = "审核备注不能超过{max}个字符")
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
     * 搜索关键字，匹配姓名、手机号或邀请码。
     */
    private String keyword;

    /**
     * 备注。
     */
    private String remark;
}
