package com.ym.agriculture.farming.weather.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 一次预报同步结果摘要（仅 {@code extensions=all}）。
 *
 * @author ym-cloud
 */
@Data
public class SfWeatherSyncResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 本次同步批次主键（库表 {@code sf_weather_sync} 等）
     */
    private Long syncId;

    /**
     * 高德行政区划 adcode
     */
    private String adcode;

    /**
     * 本次写入预报行数（通常 4：今天 + 未来 3 天）
     */
    private int forecastRows;
}
