package com.ym.agriculture.farmtask.employee.model.vo;

import com.ym.agriculture.farmtask.employee.model.entity.SysInviteCode;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 邀请码视图对象 sys_invite_code。
 */
@Data
@AutoMapper(target = SysInviteCode.class)
public class SysInviteCodeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 邀请码ID。
     */
    private Long codeId;

    /**
     * 租户ID。
     */
    private String tenantId;

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
     * 关联应用角色编码，对应 ym.employee.app-roles 配置中的 code。
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
     * 最大使用次数。
     */
    private Integer maxUses;

    /**
     * 已使用次数。
     */
    private Integer usedCount;

    /**
     * 过期时间，NULL 表示永久有效。
     */
    private Date expireTime;

    /**
     * 状态：1有效 0停用。
     */
    private String status;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 备注。
     */
    private String remark;
}
