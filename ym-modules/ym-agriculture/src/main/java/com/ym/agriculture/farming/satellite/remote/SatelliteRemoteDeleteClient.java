package com.ym.agriculture.farming.satellite.remote;

import com.ym.agriculture.farming.satellite.remote.dto.SatelliteRemoteTaskDeleteRequest;
import com.ym.agriculture.farming.satellite.remote.dto.SatelliteRemoteTaskResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 外部遥感删除服务 HTTP 接口。
 */
public interface SatelliteRemoteDeleteClient {

    @POST("satellite-task/delete")
    Call<SatelliteRemoteTaskResponse> deleteTask(@Body SatelliteRemoteTaskDeleteRequest request);
}
