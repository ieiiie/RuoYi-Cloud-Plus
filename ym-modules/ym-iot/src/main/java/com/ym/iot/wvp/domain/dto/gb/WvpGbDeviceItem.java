package com.ym.iot.wvp.domain.dto.gb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * WVP 设备条目，对应 {@code GET /api/device/query/devices} 返回的 {@code data.list} 元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WvpGbDeviceItem {

    /** DB 主键 */
    private Integer id;

    /** 国标设备编号（GB28181 DeviceID） */
    private String deviceId;

    private String name;
    private String manufacturer;
    private String model;
    private String firmware;
    private String transport;
    private String streamMode;
    private String ip;
    private Integer port;
    private String hostAddress;

    /** 在线状态（WVP 字段名为 onLine） */
    private Boolean onLine;

    private String registerTime;
    private String keepaliveTime;
    private Integer heartBeatInterval;
    private Integer heartBeatCount;
    private Integer channelCount;
    private Integer expires;
    private String createTime;
    private String updateTime;
    private String mediaServerId;
    private String charset;
    private String geoCoordSys;
    private String password;
    private String sdpIp;
    private String localIp;
    private Boolean asMessageChannel;
    private String serverId;
    private Boolean ssrcCheck;
}
