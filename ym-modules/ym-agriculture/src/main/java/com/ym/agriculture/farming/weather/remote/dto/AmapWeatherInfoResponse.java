package com.ym.agriculture.farming.weather.remote.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;

/**
 * 高德 {@code v3/weather/weatherInfo} 根响应。
 * <p>{@code extensions=base} 返回 {@code lives}；{@code extensions=all} 返回 {@code forecasts}。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapWeatherInfoResponse {

    @JsonDeserialize(using = AmapFlexibleStringDeserializer.class)
    @JsonProperty("status")
    private String status;

    @JsonProperty("info")
    private String info;

    @JsonDeserialize(using = AmapFlexibleStringDeserializer.class)
    @JsonProperty("infocode")
    private String infocode;

    @JsonProperty("lives")
    private List<AmapWeatherLive> lives;

    @JsonProperty("forecasts")
    private List<AmapWeatherForecastBlock> forecasts;
}
