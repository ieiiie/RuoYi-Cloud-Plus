package com.ym.common.core.utils;

import com.ym.common.core.exception.ServiceException;
import java.util.Set;

/** 统一应用短地址及可信登录页面的配置边界。 */
public final class ApplicationEntryPolicy {
    private static final Set<String> RESERVED = Set.of(
        "api", "apps", "assets", "auth", "composite-apps", "css", "dashboard",
        "dev-api", "fonts", "images", "iot-api", "js", "login", "micro-apps",
        "monitor", "profile", "resource", "saas", "social-callback", "svg", "system");
    private ApplicationEntryPolicy() { }

    public static void requireApplicationKey(String key) {
        if (key == null || !key.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$") || RESERVED.contains(key)) {
            throw new ServiceException("应用标识无效或与系统入口冲突");
        }
    }

    public static String resolveLoginTheme(String theme) {
        if (!Set.of("agriculture", "iot").contains(theme == null ? "" : theme)) {
            throw new ServiceException("应用尚未配置有效的登录页面");
        }
        return theme;
    }
}
