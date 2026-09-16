package com.ym.system.api.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局账号在一个可登录租户中的成员信息。
 *
 * <p>{@code userId} 是该租户内的 {@code sys_user.user_id}，不是全局账号ID。</p>
 */
@Data
public class RemoteTenantUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 租户编号。 */
    private String tenantId;

    /** 租户名称。 */
    private String tenantName;

    /** 租户品牌 Logo 地址。 */
    private String logoUrl;

    /** 当前租户内的成员用户ID；平台管理员尚未进入的租户可为空，切换后返回实际 ID。 */
    private Long userId;
}
