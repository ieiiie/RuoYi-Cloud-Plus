package org.dromara.auth.domain.vo;

import lombok.Data;

/**
 * 登录账号可进入的租户信息。
 */
@Data
public class TenantLoginVo {

    /** 租户编号。 */
    private String tenantId;

    /** 租户名称。 */
    private String tenantName;

    /** 当前租户内的成员用户ID。 */
    private Long userId;
}
