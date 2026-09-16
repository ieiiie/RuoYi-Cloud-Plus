package com.ym.agriculture.farming.weather.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 单日天气预报（读模型，字段与高德/落库列对齐）。
 *
 * @author ym-cloud
 */
@Data
public class SfWeatherForecastVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 预报行主键
     */
    private Long forecastId;

    /**
     * 所属同步批次 ID
     */
    private Long syncId;

    /**
     * 行政区划 adcode
     */
    private String adcode;

    /**
     * 省级名称
     */
    private String province;

    /**
     * 城市名称
     */
    private String cityName;

    /**
     * 预报发布时间（高德 reporttime）
     */
    private String forecastReportTime;

    /**
     * 预报日期 {@code yyyy-MM-dd}
     */
    private String castDate;

    /**
     * 星期
     */
    private String weekNum;

    /**
     * 白天天气现象
     */
    private String dayWeather;

    /**
     * 夜间天气现象
     */
    private String nightWeather;

    /**
     * 白天温度（字符串，含单位或原文）
     */
    private String dayTemp;

    /**
     * 夜间温度（字符串）
     */
    private String nightTemp;

    /**
     * 白天风向
     */
    private String dayWind;

    /**
     * 夜间风向
     */
    private String nightWind;

    /**
     * 白天风力
     */
    private String dayPower;

    /**
     * 夜间风力
     */
    private String nightPower;

    /**
     * 白天温度浮点字符串（若有）
     */
    private String dayTempFloat;

    /**
     * 夜间温度浮点字符串（若有）
     */
    private String nightTempFloat;

    /**
     * 本平台拉取/入库时间
     */
    private Date pullTime;
}
