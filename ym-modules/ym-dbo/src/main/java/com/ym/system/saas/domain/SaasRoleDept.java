package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 租户角色部门关系。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_dept")
public class SaasRoleDept {
    private Long roleId;
    private Long deptId;
}
