package com.ym.agriculture.farming.satellite.remote;

import com.ym.agriculture.farming.satellite.remote.dto.SatelliteRemoteTaskRequest;
import com.ym.agriculture.farming.satellite.remote.dto.SatelliteRemoteTaskResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 外部遥感服务 HTTP 接口（与 ym-gis {@code SatelliteServiceClient} 一致：POST satellite-task）。
 */
public interface SatelliteRemoteClient {

    @POST("satellite-task")
    Call<SatelliteRemoteTaskResponse> submitTask(@Body SatelliteRemoteTaskRequest request);
}
