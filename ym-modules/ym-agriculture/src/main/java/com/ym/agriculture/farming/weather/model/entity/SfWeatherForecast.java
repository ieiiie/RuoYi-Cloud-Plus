package com.ym.agriculture.farming.weather.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 高德预报天气落库（单日一行），对应 {@code sf_weather_forecast}。
 * <p>
 * 表无租户、逻辑删除与通用审计字段；多租户 SQL 拦截需排除本表（见 {@code tenant.excludes}）。
 */
@Data
@NoArgsConstructor
@TableName("sf_weather_forecast")
public class SfWeatherForecast implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "forecast_id", type = IdType.ASSIGN_ID)
    private Long forecastId;

    private Long syncId;
    private String adcode;
    private String province;
    private String cityName;
    private String forecastReportTime;
    private String castDate;
    private String weekNum;
    private String dayWeather;
    private String nightWeather;
    private String dayTemp;
    private String nightTemp;
    private String dayWind;
    private String nightWind;
    private String dayPower;
    private String nightPower;
    private String dayTempFloat;
    private String nightTempFloat;
    private Date pullTime;
}
