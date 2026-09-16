package com.ym.agriculture.farming.weather.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 高德 Web 服务「天气查询」配置（预报 {@code extensions=all}、实况 {@code extensions=base}），文档：
 * <a href="https://lbs.amap.com/api/webservice/guide/api-advanced/weatherinfo">天气查询</a>
 */
@Data
@Component
@ConfigurationProperties(prefix = "amap-weather")
public class AmapWeatherProperties {

    /**
     * 是否启用天气同步（未配置 key 时应在业务层拒绝调用）。
     */
    private boolean enabled = true;

    /**
     * Web 服务 Key，勿提交到仓库，建议使用环境变量注入。
     */
    private String key = "";

    /**
     * Retrofit 根地址（须含协议与主机，建议以 {@code /} 结尾；若写成带 path 的旧版完整 URL，配置类会规范为「根 + /」）。
     * 实际请求路径为 {@code v3/weather/weatherInfo}，见 {@link com.ym.agriculture.farming.weather.remote.AmapWeatherApi}。
     */
    private String baseUrl = "https://restapi.amap.com/";

    /**
     * 实况缓存日刷新边界（小时，0 表示 00:00/24:00）；{@code pull_time} 落在当前时段内则读库，否则按需拉取。
     */
    private List<Integer> liveRefreshHours = List.of(6, 12, 18, 0);

    private Timeout timeout = new Timeout();

    @Data
    public static class Timeout {
        private int connect = 10;
        private int read = 20;
    }
}
