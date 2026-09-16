package com.ym.agriculture.farmtask.employee.model.bo;

import com.ym.common.mybatis.core.domain.BaseEntity;
import com.ym.agriculture.farmtask.employee.model.entity.SysInviteCode;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 邀请码业务对象 sys_invite_code。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysInviteCode.class, reverseConvertGenerate = false)
public class SysInviteCodeBo extends BaseEntity {

    /**
     * 邀请码ID。
     */
    private Long codeId;

    /**
     * 邀请码。
     */
    private String inviteCode;

    /**
     * 邀请码类型：1角色邀请码 2人员绑定码。
     */
    private String codeType;

    /**
     * 绑定目标员工ID。
     */
    private Long employeeId;

    /**
     * 应用角色编码。
     */
    @NotBlank(message = "小程序角色不能为空")
    private String appRoleCode;

    /**
     * 人员类型：0内部 1外部。
     */
    private String personType;

    /**
     * 最大使用次数。
     */
    private Integer maxUses;

    /**
     * 已使用次数。
     */
    private Integer usedCount;

    /**
     * 过期时间，为空表示永久有效。
     */
    private Date expireTime;

    /**
     * 状态：1有效 0停用。
     */
    private String status;

    /**
     * 列表状态：active生效中 expired已过期。
     */
    private String listStatus;

    /**
     * 搜索关键字，匹配邀请码。
     */
    private String keyword;

    /**
     * 小程序码页面路径。
     */
    private String page;

    /**
     * 小程序环境版本：release/trial/develop。
     */
    private String envVersion;

    /**
     * 备注。
     */
    private String remark;
}
