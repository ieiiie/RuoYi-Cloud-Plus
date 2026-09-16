package com.ym.agriculture.farming.weatheralert.remote;

import com.ym.agriculture.farming.weatheralert.remote.dto.NmcFindAlarmResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * 中央气象台公开预警接口（非官方开放 API）。
 */
public interface NmcWeatherAlertApi {

    @GET("rest/findAlarm")
    Call<NmcFindAlarmResponse> findAlarm(
        @Query("pageNo") int pageNo,
        @Query("pageSize") int pageSize,
        @Query("signaltype") String signalType,
        @Query("signallevel") String signalLevel,
        @Query("province") String province
    );

    /** 详情 HTML；仅允许访问配置的固定主机。 */
    @GET
    Call<ResponseBody> fetchHtml(@Url String absoluteUrl);
}
