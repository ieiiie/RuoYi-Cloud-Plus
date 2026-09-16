package com.ym.auth.domain.vo;

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

    /** 租户品牌 Logo 地址；未配置时为空。 */
    private String logoUrl;

    /** 当前租户内的成员用户ID；平台管理员首次进入前可为空。 */
    private Long userId;
}
