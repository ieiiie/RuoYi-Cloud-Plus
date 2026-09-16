package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * SaaS 租户成员，仅供新租户管理员初始化。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SaasTenantUser extends BaseEntity {
    @TableId("user_id")
    private Long userId;
    private String tenantId;
    private Long globalUserId;
    private Long deptId;
    private String nickName;
    private String userType;
    private String email;
    private String gender;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
