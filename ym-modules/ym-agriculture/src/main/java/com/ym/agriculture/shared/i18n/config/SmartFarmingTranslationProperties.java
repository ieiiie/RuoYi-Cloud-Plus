package com.ym.agriculture.shared.i18n.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 智慧农业中文到维文翻译配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "ym.smart-farming.translation")
public class SmartFarmingTranslationProperties {

    /** 是否启用真实第三方翻译调用。 */
    private Boolean enabled;

    /** 讯飞机器翻译地址。 */
    private String translateUrl;

    /** 讯飞应用 ID。 */
    private String appId;

    /** 讯飞 API Key。 */
    private String apiKey;

    /** 讯飞 API Secret。 */
    private String apiSecret;

    /** 翻译目标语言，讯飞语种编码默认为 uy。 */
    private String translateTo = "uy";

    /** HTTP 超时配置。 */
    private Timeout timeout = new Timeout();

    /** 后台处理配置。 */
    private Worker worker = new Worker();

    /**
     * 旧 stask 语音配置兼容读取一个发布周期，优先级低于本配置前缀。
     */
    @Value("${ym.stask.voice.enabled:false}")
    private boolean legacyVoiceEnabled;

    @Value("${ym.stask.voice.translate-url:}")
    private String legacyVoiceTranslateUrl;

    @Value("${ym.stask.voice.app-id:}")
    private String legacyVoiceAppId;

    @Value("${ym.stask.voice.api-key:}")
    private String legacyVoiceApiKey;

    @Value("${ym.stask.voice.api-secret:}")
    private String legacyVoiceApiSecret;

    @Value("${ym.stask.voice.translate-to:uy}")
    private String legacyVoiceTranslateTo;

    public boolean isEnabled() {
        return enabled != null ? enabled : legacyVoiceEnabled;
    }

    public String getTranslateUrl() {
        return firstConfigured(translateUrl, legacyVoiceTranslateUrl);
    }

    public String getAppId() {
        return firstConfigured(appId, legacyVoiceAppId);
    }

    public String getApiKey() {
        return firstConfigured(apiKey, legacyVoiceApiKey);
    }

    public String getApiSecret() {
        return firstConfigured(apiSecret, legacyVoiceApiSecret);
    }

    public String getTranslateTo() {
        return firstConfigured(translateTo, legacyVoiceTranslateTo);
    }

    private static String firstConfigured(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    /** HTTP 超时。 */
    @Data
    public static class Timeout {
        /** 建连超时秒数。 */
        private int connect = 10;

        /** 读写超时秒数。 */
        private int read = 30;
    }

    /** 后台 Worker。 */
    @Data
    public static class Worker {
        /** 是否启动后台扫描。 */
        private boolean enabled = false;

        /** 单轮最多领取数。 */
        private int batchSize = 20;

        /** 领取租约分钟数。 */
        private int leaseMinutes = 10;

        /** 首次调用失败后的最大自动重试次数；默认 3 次，总调用上限为 4 次。 */
        private int maxRetryCount = 3;
    }
}
