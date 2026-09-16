package com.ym.agriculture.farming.weather.remote;

import com.ym.agriculture.farming.weather.remote.dto.AmapWeatherInfoResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * 高德 Web 服务「天气查询」Retrofit 声明，文档：
 * <a href="https://lbs.amap.com/api/webservice/guide/api-advanced/weatherinfo">weatherInfo</a>
 */
public interface AmapWeatherApi {

    @GET("v3/weather/weatherInfo")
    Call<AmapWeatherInfoResponse> weatherInfo(
        @Query("key") String key,
        @Query("city") String city,
        @Query("extensions") String extensions,
        @Query("output") String output
    );
}
