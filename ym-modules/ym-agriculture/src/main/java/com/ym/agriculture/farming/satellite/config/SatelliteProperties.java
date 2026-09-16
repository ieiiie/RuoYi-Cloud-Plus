package com.ym.agriculture.farming.satellite.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 外部遥感服务与本服务回调地址等配置（原 ym-gis {@code satellite.*}）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "satellite")
public class SatelliteProperties {

    /**
     * 遥感服务 baseUrl，须以 / 结尾，如 {@code http://host:5000/api/dk/}
     */
    private String baseUrl = "";

    /**
     * 遥感删除服务 baseUrl，须以 / 结尾；未配置时回退 {@link #baseUrl}。
     */
    private String deleteBaseUrl = "";

    /**
     * 提交任务时传给远端的回调 URL（需公网可达），如 {@code http://your-host:8080/smart-farming/satellite/callback/result}
     */
    private String callUrl = "";


    private Timeout timeout = new Timeout();

    /**
     * 是否注册 Retrofit（未配置 baseUrl 时关闭，避免启动失败）。
     */
    private boolean enabled = true;

    /**
     * 定时批量提交时，同时发往外部遥感服务的在途 HTTP 请求上限（至少为 1，非法或 ≤0 时按 1 处理）。
     */
    private int submitMaxConcurrency = 10;

    @Data
    public static class Timeout {
        private int connect = 30;
        private int read = 30;
        private int write = 30;
    }
}
