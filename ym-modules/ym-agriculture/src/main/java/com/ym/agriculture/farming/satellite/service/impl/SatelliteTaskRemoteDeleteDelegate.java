package com.ym.agriculture.farming.satellite.service.impl;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTask;
import com.ym.agriculture.farming.satellite.remote.SatelliteRemoteDeleteClient;
import com.ym.agriculture.farming.satellite.remote.dto.SatelliteRemoteTaskDeleteRequest;
import com.ym.agriculture.farming.satellite.remote.dto.SatelliteRemoteTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.ObjectProvider;
import retrofit2.Response;

import java.io.IOException;

/**
 * 外部遥感任务删除调用。
 */
@Slf4j
@RequiredArgsConstructor
final class SatelliteTaskRemoteDeleteDelegate {

    private final ObjectProvider<SatelliteRemoteDeleteClient> satelliteRemoteDeleteClientProvider;

    void deleteRemote(SfSatelliteTask task) {
        SatelliteRemoteDeleteClient satelliteRemoteDeleteClient = satelliteRemoteDeleteClientProvider.getIfAvailable();
        if (satelliteRemoteDeleteClient == null) {
            log.warn("Satellite remote delete service unavailable, continue local delete, dkId={}", task.getDkId());
            return;
        }
        SatelliteRemoteTaskDeleteRequest request = buildRemoteDeleteRequest(task);
        log.info("Start satellite remote task delete, dkId={}, startDate={}, endDate={}, taskType={}",
            request.getDkId(), request.getStartDate(), request.getEndDate(), request.getTaskType());
        try {
            Response<SatelliteRemoteTaskResponse> response = satelliteRemoteDeleteClient.deleteTask(request).execute();
            if (response.code() == 404) {
                log.info("Satellite remote task not found, delete local task instead, dkId={}", task.getDkId());
                return;
            }
            if (!response.isSuccessful()) {
                log.warn("Satellite remote task delete failed, continue local delete, dkId={}, reason={}",
                    task.getDkId(), handleErrorBody(response.errorBody()));
                return;
            }
            SatelliteRemoteTaskResponse body = response.body();
            if (body == null) {
                log.warn("Satellite remote task delete returned empty body, continue local delete, dkId={}", task.getDkId());
                return;
            }
            if (body.getCode() == null || (body.getCode() != 200 && body.getCode() != 404)) {
                String message = StringUtils.isBlank(body.getMessage()) ? "code=" + body.getCode() : body.getMessage();
                log.warn("Satellite remote task delete rejected, continue local delete, dkId={}, reason={}",
                    task.getDkId(), message);
                return;
            }
            if (body.getCode() == 404) {
                log.info("Satellite remote task not found, delete local task instead, dkId={}", task.getDkId());
            } else {
                log.info("Satellite remote task deleted dkId={}", task.getDkId());
            }
        } catch (IOException e) {
            log.warn("Satellite remote task delete request failed, continue local delete, dkId={}, reason={}",
                task.getDkId(), e.getMessage());
        }
    }

    private static SatelliteRemoteTaskDeleteRequest buildRemoteDeleteRequest(SfSatelliteTask task) {
        SatelliteRemoteTaskDeleteRequest request = new SatelliteRemoteTaskDeleteRequest();
        request.setDkId(task.getDkId());
        request.setStartDate(task.getStartDate());
        request.setEndDate(task.getEndDate());
        request.setTaskType(task.getTaskType());
        return request;
    }

    private static String handleErrorBody(ResponseBody errorBody) throws IOException {
        if (errorBody != null) {
            return "server error: " + errorBody.string();
        }
        return "server error response";
    }
}
