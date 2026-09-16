package com.ym.agriculture.farmtask.employee.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 人员档案编辑业务对象（不含微信 openid / 微信关联手机号）。
 */
@Data
public class SysEmployeeUpdateBo {

    /**
     * 员工ID。
     */
    @NotNull(message = "人员ID不能为空")
    private Long employeeId;

    /**
     * 姓名，2-20 个字符。
     */
    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 20, message = "姓名长度必须在{min}到{max}个字符之间")
    private String name;

    /**
     * 手机号，11 位手机号，可为空。
     */
    private String phone;

    /**
     * 人员类型：0内部 1外部。
     */
    @NotBlank(message = "人员类型不能为空")
    private String personType;

    /**
     * 状态：0正常 1停用。
     */
    @NotBlank(message = "状态不能为空")
    private String status;

    /**
     * 身份证号，18 位，可为空。
     */
    private String idCard;

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
     * 应用角色编码。
     */
    @NotBlank(message = "小程序角色不能为空")
    private String appRoleCode;

    /**
     * 旧的小程序注册审批系统角色ID，仅兼容历史调用。
     */
    private Long miniappRegisterApprovalRoleId;

    /**
     * 小程序绑定的系统角色ID集合；传空集合时清空绑定。
     */
    private List<Long> miniappRoleIds;

    /**
     * 备注。
     */
    @Size(max = 500, message = "备注不能超过{max}个字符")
    private String remark;
}
