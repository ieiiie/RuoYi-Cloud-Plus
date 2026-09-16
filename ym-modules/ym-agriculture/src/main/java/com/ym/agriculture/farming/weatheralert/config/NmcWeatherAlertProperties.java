package com.ym.agriculture.farming.weatheralert.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 中央气象台公开预警同步配置（非官方 SLA，必须缓存与降级）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "nmc-weather-alert")
public class NmcWeatherAlertProperties {

    /** 是否启用同步与远程拉取。 */
    private boolean enabled = true;

    /** Retrofit 根地址，须以 / 结尾。 */
    private String baseUrl = "https://www.nmc.cn/";

    /** 允许访问的主机名（小写）。 */
    private String allowedHost = "www.nmc.cn";

    /** 单省列表每页条数。 */
    private int pageSize = 50;

    /** 单省最多翻页数，防止异常数据量拖垮任务。 */
    private int maxPagesPerProvince = 20;

    /** 相邻省份请求间隔毫秒。 */
    private long provinceIntervalMs = 500L;

    /** 最近成功同步后多少分钟内视为 FRESH。 */
    private int freshWithinMinutes = 30;

    /** 连续多少次成功同步未出现则标记失效。 */
    private int deactivateAfterMissing = 2;

    /** 是否尝试抓取详情页正文与防御指南。 */
    private boolean fetchDetail = true;

    private Timeout timeout = new Timeout();

    @Data
    public static class Timeout {
        private int connect = 8;
        private int read = 15;
    }
}
