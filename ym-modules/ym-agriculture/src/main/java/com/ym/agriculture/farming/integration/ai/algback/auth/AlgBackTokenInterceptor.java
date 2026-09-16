package com.ym.agriculture.farming.integration.ai.algback.auth;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * OkHttp 应用拦截器：除登录外为请求追加 {@code token} 头，值为登录返回的 JWT。
 * <p>
 * 登录为 {@code POST .../sys/auth/login}，该请求不带 token。路径判断见 {@link AlgBackAuthPaths#isLogin(okhttp3.HttpUrl)}。
 *
 * @author ym-cloud
 */
public final class AlgBackTokenInterceptor implements Interceptor {

    private final AlgBackTokenHolder tokenHolder;

    public AlgBackTokenInterceptor(AlgBackTokenHolder tokenHolder) {
        this.tokenHolder = tokenHolder;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (AlgBackAuthPaths.isLogin(request.url())) {
            return chain.proceed(request);
        }
        String t = tokenHolder.get();
        if (t == null || t.isEmpty()) {
            return chain.proceed(request);
        }
        return chain.proceed(request.newBuilder().header("token", t).build());
    }

}
