package com.ym.agriculture.farming.trace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 瓜果溯源配置：H5 跳转基址等。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ym.trace")
public class TraceProperties {

    /**
     * H5 溯源页基址，不含末尾斜杠，如 {@code https://h5.example.com/trace}。
     */
    private String h5BaseUrl = "https://h5.example.com/trace";

    /** 单次生成溯源码数量上限 */
    private int maxGeneratePerRequest = 5000;
}
