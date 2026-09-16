package com.ym.iot.wvp.service;

import com.ym.iot.wvp.domain.vo.WvpSnapshotVo;

/**
 * WVP 通道快照查询服务。
 */
public interface IWvpSnapshotService {

    /**
     * 查询摄像头通道当前快照图片地址。
     *
     * @param deviceId  WVP 国标设备编号
     * @param channelId WVP 国标通道编号
     * @return 快照地址结果
     */
    WvpSnapshotVo querySnapshot(String deviceId, String channelId);
}
