package com.ym.agriculture.farming.weather.remote.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 高德预报 {@code forecasts[0]} 块（城市信息 + casts 列表）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapWeatherForecastBlock {

    @JsonProperty("city")
    private String city;

    @JsonProperty("adcode")
    private String adcode;

    @JsonProperty("province")
    private String province;

    @JsonProperty("reporttime")
    private String reportTime;

    @JsonProperty("casts")
    private List<AmapWeatherCast> casts;
}
