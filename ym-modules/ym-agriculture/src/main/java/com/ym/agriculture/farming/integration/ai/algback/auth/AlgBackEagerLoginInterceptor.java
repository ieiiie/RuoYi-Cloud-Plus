package com.ym.agriculture.farming.integration.ai.algback.auth;

import com.ym.agriculture.farming.integration.ai.algback.autoconfigure.YmAlgBackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;

/**
 * 在 token 拦截器之前执行：若无 JWT 且开启 {@link YmAlgBackProperties.Auth#isEagerLoginWhenTokenMissing()}，
 * 则用配置账号同步登录，避免首条业务请求空 token。
 * <p>
 * 使用 {@link ObjectProvider}{@code <AlgBackAuthService>} 延迟解析，避免与 {@link AlgBackAuthService} 循环依赖。
 * 登录子请求走 {@link AlgBackAuthPaths#isLogin}，不递归 eager。
 *
 * @author ym-cloud
 */
@Slf4j
@RequiredArgsConstructor
public final class AlgBackEagerLoginInterceptor implements Interceptor {

    private final AlgBackTokenHolder tokenHolder;

    private final YmAlgBackProperties properties;

    private final ObjectProvider<AlgBackAuthService> authServiceProvider;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (AlgBackAuthPaths.isLogin(request.url())) {
            return chain.proceed(request);
        }
        YmAlgBackProperties.Auth auth = properties.getAuth();
        if (auth == null || !auth.isEagerLoginWhenTokenMissing()) {
            return chain.proceed(request);
        }
        if (!tokenHolder.hasToken()) {
            AlgBackAuthService svc = authServiceProvider.getIfAvailable();
            if (svc != null) {
                try {
                    svc.loginWithConfiguredCredentials(properties);
                    if (tokenHolder.hasToken()) {
                        log.debug("ym.alg-back 请求前同步登录成功");
                    }
                } catch (Exception e) {
                    log.warn("ym.alg-back 请求前同步登录失败: {}", e.getMessage());
                }
            }
        }
        return chain.proceed(request);
    }
}
