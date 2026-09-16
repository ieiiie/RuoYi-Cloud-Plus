package com.ym.agriculture.farming.news.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "smart-farming.news.media-transfer")
public class SfNewsMediaProperties {
    private boolean enabled = true;
    private int batchSize = 10;
    private int maxItemsPerRevision = 30;
    private long imageMaxBytes = 10L * 1024 * 1024;
    private long videoMaxBytes = 200L * 1024 * 1024;
    private long revisionMaxBytes = 500L * 1024 * 1024;
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 60;
    private int callTimeoutSeconds = 600;
    private int maxRedirects = 3;
    private int maxAttempts = 3;
    private List<Integer> retryDelaysSeconds = List.of(60, 300, 1800);
    private String tempDir = System.getProperty("java.io.tmpdir") + "/ym-news-media";
    private String platformTenantId = "000000";
}
