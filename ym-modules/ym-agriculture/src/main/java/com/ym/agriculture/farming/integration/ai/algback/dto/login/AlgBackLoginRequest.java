package com.ym.agriculture.farming.integration.ai.algback.dto.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求体 {@code POST /sys/auth/login}；{@code password} 由 {@link com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackAuthService} 做加盐摘要后传入。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackLoginRequest {

    private String username;

    /** 中台侧：{@code SHA-256(hex)(明文密码 + kfk20ac23sj99kfk)}。 */
    private String password;
}
