package com.ym.auth.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 登录验证信息
 *
 * @author Michelle.Chung
 */
@Data
public class LoginVo {

    /**
     * 授权令牌
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 刷新令牌
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 授权令牌 access_token 的有效期
     */
    @JsonProperty("expire_in")
    private Long expireIn;

    /**
     * 刷新令牌 refresh_token 的有效期
     */
    @JsonProperty("refresh_expire_in")
    private Long refreshExpireIn;

    /**
     * 应用id
     */
    @JsonProperty("client_id")
    private String clientId;

    /**
     * 令牌权限
     */
    private String scope;

    /**
     * 用户 openid
     */
    private String openid;

    /**
     * 本次登录自动进入的租户。
     */
    private TenantLoginVo currentTenant;

    /**
     * 当前全局账号可进入的全部有效租户。
     */
    private List<TenantLoginVo> tenants;

}
