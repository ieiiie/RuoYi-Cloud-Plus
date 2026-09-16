package com.ym.iot.wvp.domain.dto.gb;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 通道项；WVP 部分字段名带尾部空格，已按实际 JSON 映射。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WvpGbChannelItem {

    @JsonProperty("ID")
    private String id;

    @JsonProperty("DeviceID")
    private String deviceId;

    @JsonProperty("DeviceName")
    private String deviceName;

    @JsonProperty("DeviceOnline")
    private Boolean deviceOnline;

    @JsonProperty("Channel")
    private Integer channel;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Custom")
    private Boolean custom;

    @JsonProperty("CustomName")
    private String customName;

    @JsonProperty("SubCount")
    private Integer subCount;

    @JsonProperty("SnapURL")
    private String snapUrl;

    /** WVP 部分版本字段名带尾部空格，已同时兼容无空格名 */
    @JsonProperty("Manufacturer ")
    @JsonAlias("Manufacturer")
    private String manufacturer;

    @JsonProperty("Model")
    private String model;

    @JsonProperty("Owner")
    private String owner;

    @JsonProperty("CivilCode")
    private String civilCode;

    @JsonProperty("Address")
    private String address;

    @JsonProperty("Parental")
    private Integer parental;

    @JsonProperty("ParentID")
    private String parentId;

    @JsonProperty("Secrecy")
    private String secrecy;

    @JsonProperty("RegisterWay")
    private Integer registerWay;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Longitude")
    private Double longitude;

    @JsonProperty("Latitude")
    private Double latitude;

    @JsonProperty("PTZType ")
    @JsonAlias("PTZType")
    private Integer ptzType;

    @JsonProperty("CustomPTZType")
    private String customPtzType;

    @JsonProperty("StreamID")
    private String streamId;

    @JsonProperty("NumOutputs ")
    @JsonAlias("NumOutputs")
    private Integer numOutputs;
}
