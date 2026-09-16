package com.ym.iot.wvp.service;

import com.ym.iot.wvp.domain.dto.play.WvpPlayStreamData;

/**
 * WVP Web 播放 API（{@code /api/play/start|stop/...}），与前端 {@code web/src/api/play.js} 一致。
 */
public interface IWvpPlayService {

    /**
     * 开始播放，对应 {@code GET /api/play/start/{deviceId}/{channelId}}。
     *
     * @return {@code data} 节点（各协议地址）
     */
    WvpPlayStreamData playStart(String deviceId, String channelId);

    /**
     * 停止播放，对应 {@code GET /api/play/stop/{deviceId}/{channelId}}。
     */
    void playStop(String deviceId, String channelId);
}
