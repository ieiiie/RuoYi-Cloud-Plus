package com.ym.agriculture.farmtask.employee.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 平台邀请码对象 sys_invite_code。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_invite_code")
public class SysInviteCode extends TenantEntity {

    /**
     * 邀请码ID。
     */
    @TableId(value = "code_id")
    private Long codeId;

    /**
     * 邀请码，方案一为4位数字，方案二为6位数字。
     */
    private String inviteCode;

    /**
     * 邀请码类型：1角色邀请码 2人员绑定码。
     */
    private String codeType;

    /**
     * 绑定目标员工ID，方案一专用。
     */
    private Long employeeId;

    /**
     * 关联应用角色编码，方案二专用。
     */
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
     * 过期时间，NULL 表示永久有效。
     */
    private Date expireTime;

    /**
     * 状态：1有效 0停用。
     */
    private String status;

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
