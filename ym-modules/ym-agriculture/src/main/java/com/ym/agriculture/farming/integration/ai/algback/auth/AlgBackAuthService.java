package com.ym.agriculture.farming.integration.ai.algback.auth;

import cn.hutool.crypto.digest.DigestUtil;
import com.ym.agriculture.farming.integration.ai.algback.api.AlgBackApi;
import com.ym.agriculture.farming.integration.ai.algback.autoconfigure.YmAlgBackProperties;
import com.ym.agriculture.farming.integration.ai.algback.client.AlgBackSync;
import com.ym.agriculture.farming.integration.ai.algback.dto.login.AlgBackLoginData;
import com.ym.agriculture.farming.integration.ai.algback.dto.login.AlgBackLoginRequest;
import lombok.RequiredArgsConstructor;

import java.util.OptionalLong;

/**
 * 算法中台登录：调用 {@link AlgBackApi#login} 后将 JWT 写入 {@link AlgBackTokenHolder}，
 * 供 {@link AlgBackTokenInterceptor} 加到后续请求。
 * <p>
 * 多线程下注意 holder 策略（每租户独立或串行调用等）。
 *
 * @author ym-cloud
 */
@RequiredArgsConstructor
public class AlgBackAuthService {

    private static final String LOGIN_PASSWORD_SALT = "kfk20ac23sj99kfk";

    private final AlgBackApi api;

    private final AlgBackTokenHolder tokenHolder;

    private final Object loginLock = new Object();

    /**
     * 登录并缓存 token；成功时 {@link AlgBackLoginData#getToken()} 非空则已写入 holder。
     * <p>
     * 与定时刷新、HTTP 401 重登共用同一把锁，避免并发登录。
     *
     * @return 中台 {@code data}，含 token、userInfo 等
     */
    public AlgBackLoginData login(String username, String password) {
        synchronized (loginLock) {
            String passwordForApi = encodePasswordForLogin(password);
            AlgBackLoginData data = AlgBackSync.execute(api.login(new AlgBackLoginRequest(username, passwordForApi)));
            if (data != null && data.getToken() != null && !data.getToken().isEmpty()) {
                String t = data.getToken();
                OptionalLong exp = AlgBackJwtExp.parseExpEpochSeconds(t);
                tokenHolder.setTokenAndExpiry(t, exp.isPresent() ? Long.valueOf(exp.getAsLong()) : null);
            }
            return data;
        }
    }

    /**
     * 使用 {@link YmAlgBackProperties#getAuth()} 中的账号密码登录；用户名或密码为空时不调用中台。
     */
    public void loginWithConfiguredCredentials(YmAlgBackProperties properties) {
        if (properties == null) {
            return;
        }
        YmAlgBackProperties.Auth a = properties.getAuth();
        if (a == null) {
            return;
        }
        String user = a.getUsername();
        String pass = a.getPassword();
        if (user == null || user.isBlank() || pass == null || pass.isBlank()) {
            return;
        }
        login(user.trim(), pass);
    }

    /** 清除本地 token；下次请求不再带 Header（直至再次 {@link #login}）。 */
    public void logout() {
        tokenHolder.clear();
    }

    /**
     * {@code SHA-256(hex)(明文密码 + LOGIN_PASSWORD_SALT)}，UTF-8 字符串拼接后摘要。
     */
    private static String encodePasswordForLogin(String password) {
        if (password == null) {
            return null;
        }
        return DigestUtil.sha256Hex(password + LOGIN_PASSWORD_SALT);
    }
}
