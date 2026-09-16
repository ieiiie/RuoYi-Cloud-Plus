package com.ym.agriculture.farming.weather.remote.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 高德实况 {@code lives[]} 元素（{@code extensions=base}）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapWeatherLive {

    @JsonProperty("province")
    private String province;

    @JsonProperty("city")
    private String city;

    @JsonProperty("adcode")
    private String adcode;

    @JsonProperty("weather")
    private String weather;

    @JsonProperty("temperature")
    private String temperature;

    @JsonProperty("winddirection")
    private String windDirection;

    @JsonProperty("windpower")
    private String windPower;

    @JsonProperty("humidity")
    private String humidity;

    @JsonProperty("reporttime")
    private String reportTime;
}
