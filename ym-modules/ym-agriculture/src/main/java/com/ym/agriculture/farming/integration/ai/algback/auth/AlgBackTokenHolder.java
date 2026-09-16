package com.ym.agriculture.farming.integration.ai.algback.auth;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 保存中台登录返回的 JWT，供 {@link AlgBackTokenInterceptor} 写入请求头 {@code token}。
 * <p>
 * 可选记录 JWT {@code exp}（秒），供定时刷新时跳过仍有效的 token。
 *
 * @author ym-cloud
 */
public final class AlgBackTokenHolder {

    private final AtomicReference<String> token = new AtomicReference<>();

    /** JWT {@code exp}（秒，Unix）；未解析或非 JWT 时为 0。 */
    private final AtomicLong tokenExpiresAtEpochSecond = new AtomicLong(0L);

    /** 当前 token，未登录时为 {@code null} 或空串。 */
    public String get() {
        return token.get();
    }

    /** 覆盖 token并清除过期时间解析结果。 */
    public void set(String value) {
        token.set(value != null ? value.trim() : null);
        tokenExpiresAtEpochSecond.set(0L);
    }

    /**
     * 设置 token 与 JWT 过期时间（秒级时间戳）；{@code expiresAtEpochSecond} 为空或无效时等价于仅清掉 exp 记录。
     */
    public void setTokenAndExpiry(String value, Long expiresAtEpochSecondOrNull) {
        token.set(value != null ? value.trim() : null);
        if (expiresAtEpochSecondOrNull == null || expiresAtEpochSecondOrNull <= 0) {
            tokenExpiresAtEpochSecond.set(0L);
        } else {
            tokenExpiresAtEpochSecond.set(expiresAtEpochSecondOrNull);
        }
    }

    /** JWT {@code exp}（秒）；未知时为 0。 */
    public long getTokenExpiresAtEpochSecond() {
        return tokenExpiresAtEpochSecond.get();
    }
    /**
     * 在已知 {@code exp} 时，若当前时间距过期仍大于 {@code renewBeforeExpireSeconds}，则认为短期内仍有效（可跳过重复登录）。
     */
    public boolean isLikelyValidUntil(long nowEpochSecond, long renewBeforeExpireSeconds) {
        long exp = tokenExpiresAtEpochSecond.get();
        if (exp <= 0) {
            return false;
        }
        long threshold = exp - Math.max(0L, renewBeforeExpireSeconds);
        return nowEpochSecond < threshold;
    }

    /** 等价于 {@code set(null)}。 */
    public void clear() {
        token.set(null);
        tokenExpiresAtEpochSecond.set(0L);
    }

    /** 非空且非空白时返回 true。 */
    public boolean hasToken() {
        String t = token.get();
        return t != null && !t.isEmpty();
    }
}
