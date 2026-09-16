package com.ym.agriculture.farming.weather.remote.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 高德预报 {@code forecasts[0].casts[]} 单日元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapWeatherCast {

    @JsonProperty("date")
    private String date;

    @JsonProperty("week")
    private String week;

    @JsonProperty("dayweather")
    private String dayWeather;

    @JsonProperty("nightweather")
    private String nightWeather;

    @JsonProperty("daytemp")
    private String dayTemp;

    @JsonProperty("nighttemp")
    private String nightTemp;

    @JsonProperty("daywind")
    private String dayWind;

    @JsonProperty("nightwind")
    private String nightWind;

    @JsonProperty("daypower")
    private String dayPower;

    @JsonProperty("nightpower")
    private String nightPower;

    @JsonProperty("daytemp_float")
    private String dayTempFloat;

    @JsonProperty("nighttemp_float")
    private String nightTempFloat;
}
