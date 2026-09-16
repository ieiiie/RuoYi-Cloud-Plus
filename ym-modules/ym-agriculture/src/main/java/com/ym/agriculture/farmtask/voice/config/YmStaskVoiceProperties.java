package com.ym.agriculture.farmtask.voice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * stask 任务维语播报配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ym.stask.voice")
public class YmStaskVoiceProperties {

    /**
     * 是否启用真实语音生成。
     */
    private boolean enabled = false;

    /**
     * 讯飞维语音色。
     */
    private String voiceName = "";

    /**
     * 音频格式。
     */
    private String audioFormat = "mp3";

    /**
     * 讯飞 TTS 音频编码。mp3 文件对应 lame。
     */
    private String ttsAue = "lame";

    /**
     * 讯飞 TTS 引擎类型。
     */
    private String ttsEnt = "intp65";

    /**
     * 讯飞 TTS 文本编码。小语种使用 unicode。
     */
    private String ttsTextEncoding = "unicode";

    /**
     * 语音缓存版本。调整脚本规则、术语或音频策略时递增，避免复用旧音频。
     */
    private String cacheVersion = "v1";

    /**
     * 讯飞机器翻译 URL。
     */
    private String translateUrl = "";

    /**
     * 讯飞新机器翻译目标语种。维吾尔语在 niutrans 中使用 uy。
     */
    private String translateTo = "uy";

    /**
     * 讯飞理想语音合成 WebSocket URL。
     */
    private String ttsUrl = "";

    private String appId = "";

    private String apiKey = "";

    private String apiSecret = "";

    private Timeout timeout = new Timeout();

    /**
     * 持久化语音生成 worker 配置。
     */
    private Worker worker = new Worker();

    @Data
    public static class Timeout {
        private int connect = 10;
        private int read = 30;
    }

    @Data
    public static class Worker {
        /**
         * 是否启用后台 worker 扫描待生成记录。
         */
        private boolean enabled = false;

        /**
         * 单轮最多处理记录数。
         */
        private int batchSize = 10;

        /**
         * PROCESSING 超过该分钟数视为服务中断遗留，可重新领取。
         */
        private int processingTimeoutMinutes = 10;

        /**
         * 首次调用后的最大重试次数；默认 3 表示最多调用 4 次。
         */
        private int maxRetryCount = 3;

        /**
         * 首次失败后三次重试的等待分钟数。
         */
        private List<Integer> retryDelayMinutes = List.of(1, 5, 30);
    }
}
