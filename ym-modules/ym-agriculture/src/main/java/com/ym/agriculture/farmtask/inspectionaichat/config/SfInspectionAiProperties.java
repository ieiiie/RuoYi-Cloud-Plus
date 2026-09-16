package com.ym.agriculture.farmtask.inspectionaichat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 巡查照片 AI 问答配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "smart-farming.inspection-ai")
public class SfInspectionAiProperties {
    /** 是否启用。 */
    private boolean enabled;
    /** 百炼 OpenAI 兼容接口根地址。 */
    private String baseUrl;
    /** 接口路径。 */
    private String chatCompletionsPath = "/chat/completions";
    /** 视觉问答模型。 */
    private String modelId = "qwen-vl-max";
    /** 视觉模型最大上下文 Token。 */
    private int maxContextTokens = 32_768;
    /** API Key，仅后端使用。 */
    private String apiKey;
    /** 连接超时秒数。 */
    private int connectTimeoutSeconds = 10;
    /** 读取超时秒数。 */
    private int readTimeoutSeconds = 60;
    /** 单次请求最大图片数。 */
    private int maxImagesPerRequest = 6;
    /** 单图最大字节数。 */
    private long maxImageBytes = 10L * 1024 * 1024;
    /** 单图最大像素数。 */
    private long maxImagePixels = 16_777_216L;
    /** 传给模型的签名 URL 有效期。 */
    private int signedUrlTtlSeconds = 600;
    /** 问题最大字符数。 */
    private int maxQuestionLength = 2000;
    /** 每用户每分钟最大请求数。 */
    private int perUserPerMinute = 10;
    /** 过期生成任务秒数。 */
    private int staleGenerationSeconds = 120;
    /** 上下文文字估算安全系数。 */
    private double contextEstimateSafetyFactor = 1.30;
    /** 摘要配置。 */
    private Summary summary = new Summary();

    /** 历史摘要模型配置。 */
    @Data
    public static class Summary {
        /** 主摘要模型。 */
        private String modelId = "tongyi-xiaomi-analysis-flash";
        /** 临时故障时使用的备选模型。 */
        private String fallbackModelId = "qwen-turbo";
        /** 摘要温度。 */
        private int temperature;
        /** 摘要 top_k。 */
        private int topK = 1;
        /** 上下文达到该比例后压缩。 */
        private double contextThresholdRatio = 0.70;
        /** 保留完整的最近对话轮数。 */
        private int retainRecentRounds = 12;
    }
}
