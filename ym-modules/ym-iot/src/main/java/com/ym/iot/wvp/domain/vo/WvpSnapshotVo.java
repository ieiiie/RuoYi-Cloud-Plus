package com.ym.iot.wvp.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * WVP 摄像头通道快照结果。
 */
@Data
public class WvpSnapshotVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 国标设备编号，对应 WVP deviceId。
     */
    private String deviceId;

    /**
     * 国标通道编号，对应 WVP channelId。
     */
    private String channelId;

    /**
     * 当前快照图片地址，WVP 未返回时为空。
     */
    private String snapshotUrl;

    /**
     * WVP 业务提示信息。
     */
    private String message;

    /**
     * WVP 返回 data 节点摘要，便于排查字段兼容问题。
     */
    private String rawData;
}
