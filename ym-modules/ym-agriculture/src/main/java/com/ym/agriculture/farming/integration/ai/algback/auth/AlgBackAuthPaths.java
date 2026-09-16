package com.ym.agriculture.farming.integration.ai.algback.auth;

import okhttp3.HttpUrl;

/**
 * 算法中台鉴权路径判断（与 {@link AlgBackTokenInterceptor} 一致）。
 *
 * @author ym-cloud
 */
public final class AlgBackAuthPaths {

    private AlgBackAuthPaths() {
    }

    /** 是否为登录接口（该请求不得带业务 token）。 */
    public static boolean isLogin(HttpUrl url) {
        if (url == null) {
            return false;
        }
        String path = url.encodedPath();
        return path.endsWith("/sys/auth/login") || path.endsWith("sys/auth/login");
    }
}
