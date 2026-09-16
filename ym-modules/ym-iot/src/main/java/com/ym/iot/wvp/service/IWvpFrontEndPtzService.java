package com.ym.iot.wvp.service;

/**
 * WVP 前端云台：{@code GET /api/front-end/ptz/{deviceId}/{channelId}}。
 */
public interface IWvpFrontEndPtzService {

    /**
     * @param command        up / down / left / right / stop
     * @param horizonSpeed   水平速度，可空
     * @param verticalSpeed  垂直速度，可空
     * @param zoomSpeed      变倍速度，可空
     */
    void frontEndPtz(
        String deviceId,
        String channelId,
        String command,
        Integer horizonSpeed,
        Integer verticalSpeed,
        Integer zoomSpeed);
}
