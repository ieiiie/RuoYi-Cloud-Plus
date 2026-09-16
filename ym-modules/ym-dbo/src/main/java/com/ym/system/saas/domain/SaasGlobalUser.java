package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * SaaS 全局账号，仅供新租户管理员初始化。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_global_user")
public class SaasGlobalUser extends BaseEntity {
    @TableId("global_user_id")
    private Long globalUserId;
    private String userName;
    private String nickName;
    private String userType;
    private String email;
    private String phoneNumber;
    private String gender;
    private Long avatar;
    private String password;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
