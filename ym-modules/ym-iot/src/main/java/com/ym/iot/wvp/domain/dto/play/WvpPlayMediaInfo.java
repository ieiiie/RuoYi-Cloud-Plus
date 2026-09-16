package com.ym.iot.wvp.domain.dto.play;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * {@code data.mediaInfo}，字段随 ZLM/WVP 版本可能增减。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WvpPlayMediaInfo {

    private String app;
    private String stream;

    @JsonProperty("mediaServer")
    private WvpPlayMediaServerInfo mediaServer;

    private String schema;
    private Integer readerCount;
    private String videoCodec;
    private Integer width;
    private Integer height;
    private Double fps;
    private Double loss;
    private String audioCodec;
    private Integer audioChannels;
    private Integer audioSampleRate;
    private Double duration;
    private Boolean online;
    private Integer originType;
    private String originTypeStr;
    private String originUrl;
    private Integer aliveSecond;
    private Integer bytesSpeed;
    private String callId;
    private String serverId;
}
