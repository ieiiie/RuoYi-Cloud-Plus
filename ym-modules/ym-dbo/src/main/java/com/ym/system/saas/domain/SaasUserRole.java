package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 租户用户角色关系。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_role")
public class SaasUserRole {
    private Long userId;
    private Long roleId;
}
