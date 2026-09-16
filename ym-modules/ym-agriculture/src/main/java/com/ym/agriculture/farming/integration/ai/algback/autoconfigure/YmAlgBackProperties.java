package com.ym.agriculture.farming.integration.ai.algback.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 算法中台配置，前缀 {@code ym.alg-back}。
 * <p>
 * {@code base-url} 示例：{@code https://alg.example.com:8080/api/}（须含 API 根且以 {@code /} 结尾）。
 *
 * @author ym-cloud
 */
@Data
@ConfigurationProperties(prefix = "ym.alg-back")
public class YmAlgBackProperties {

    /**
     * 算法中台 API 根路径（Base URL），须包含 {@code /api} 且<strong>以 {@code /} 结尾</strong>。
     * 空串时不启用本模块自动配置。
     */
    private String baseUrl = "";

    /**
     * <strong>所有中台客户共用的推理回调完整 URL</strong>（与中台客户 {@code httpReqUrl} 一致）。
     * 公网须可达，路径一般为 {@code .../smart-farming/ai/callback/inference}。
     * 配置键：{@code ym.alg-back.callback-url}。
     */
    private String callbackUrl = "";

    /**
     * 推理模式：{@code VIDEO}（默认，视频推理）或 {@code IMAGE}（图片推理）。
     * 由运维全局配置，决定 UAV AI 分析走视频推理还是图片推理链路。
     * 配置键：{@code ym.alg-back.infer-mode}。
     */
    private String inferMode = "IMAGE";

    /**
     * 图片推理回调完整 URL（与 {@link #callbackUrl} 类似但用于图片推理独立回调入口）。
     * 路径一般为 {@code .../smart-farming/ai/callback/image-inference}。
     * 配置键：{@code ym.alg-back.image-infer-callback-url}。
     */
    private String imageInferCallbackUrl = "";

    /**
     * 为 true 时，业务侧在收到「租户已创建」事件后可自动调用中台 {@code addCustomer} 并启用客户（需已配置 base-url、
     * {@link #callbackUrl} 及有效 token）。配置键：{@code ym.alg-back.auto-create-customer}。
     */
    private boolean autoCreateCustomer = true;

    /** OkHttp 超时，可被 YAML 嵌套 {@code ym.alg-back.timeout.*} 覆盖。 */
    private final Timeout timeout = new Timeout();

    /** 可选启动自动登录凭据，见 {@link Auth#autoLoginOnStartup}。 */
    private final Auth auth = new Auth();

    /** 由 Snail Job 广播使用配置账号重新登录续期 token。 */
    @NestedConfigurationProperty
    private final TokenRefresh tokenRefresh = new TokenRefresh();

    @Data
    public static class Timeout {

        private int connectSeconds = 15;

        /** 拉流/分析类接口可能较慢，默认大于连接超时。 */
        private int readSeconds = 120;

        private int writeSeconds = 30;
    }

    @Data
    public static class Auth {

        /** 登录用户名。 */
        private String username = "";

        /**
         * 登录用<strong>明文</strong>密码；{@link com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackAuthService#login} 发送前会做
         * {@code SHA-256(hex)(明文 + 固定盐 kfk20ac23sj99kfk)}。勿提交到版本库，建议用环境变量或密钥管理。
         */
        private String password = "";

        /**
         * 为 true 且 username/password 均非空时，应用启动后执行一次 {@link com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackAuthService#login}；
         * 失败仅打日志，不阻止启动。
         */
        private boolean autoLoginOnStartup = false;

        /**
         * 为 true 时，在首条非登录 HTTP 请求发出前若本地尚无 token，则先用本段 username/password 同步登录一次
         * （与 HTTP 401 静默重登、Snail Job 广播刷新互补，减少冷启动后首请求失败）。
         * 无有效凭据时不调用中台；默认开启。
         */
        private boolean eagerLoginWhenTokenMissing = true;
    }

    @Data
    public static class TokenRefresh {

        /**
         * 是否启用 Snail Job 广播刷新（需配置 {@link Auth#getUsername()} / {@link Auth#getPassword()}）。
         */
        private boolean enabled = true;

        /**
         * 距 JWT 过期不足该秒数时视为需要续期；与 {@link #skipWhenJwtValid} 配合使用。
         */
        private long renewBeforeExpireSeconds = 300L;

        /**
         * 为 true 且能解析出 JWT {@code exp} 时，若当前时间仍早于「过期前 renewBeforeExpireSeconds」，则跳过本次定时登录。
         */
        private boolean skipWhenJwtValid = true;
    }
}
