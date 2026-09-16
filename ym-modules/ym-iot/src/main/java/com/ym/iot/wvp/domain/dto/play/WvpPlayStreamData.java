package com.ym.iot.wvp.domain.dto.play;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * {@code data} 节点：ZLMediaKit 各协议播放地址（snake_case，与 WVP 返回一致）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WvpPlayStreamData {

    private String app;
    private String stream;
    private String ip;

    @JsonProperty("flv")
    private String flv;

    @JsonProperty("https_flv")
    private String httpsFlv;

    @JsonProperty("ws_flv")
    private String wsFlv;

    @JsonProperty("wss_flv")
    private String wssFlv;

    @JsonProperty("fmp4")
    private String fmp4;

    @JsonProperty("https_fmp4")
    private String httpsFmp4;

    @JsonProperty("ws_fmp4")
    private String wsFmp4;

    @JsonProperty("wss_fmp4")
    private String wssFmp4;

    @JsonProperty("hls")
    private String hls;

    @JsonProperty("https_hls")
    private String httpsHls;

    @JsonProperty("ws_hls")
    private String wsHls;

    @JsonProperty("wss_hls")
    private String wssHls;

    @JsonProperty("ts")
    private String ts;

    @JsonProperty("https_ts")
    private String httpsTs;

    @JsonProperty("ws_ts")
    private String wsTs;

    @JsonProperty("wss_ts")
    private String wssTs;

    @JsonProperty("rtmp")
    private String rtmp;

    @JsonProperty("rtmps")
    private String rtmps;

    @JsonProperty("rtsp")
    private String rtsp;

    @JsonProperty("rtsps")
    private String rtsps;

    @JsonProperty("rtc")
    private String rtc;

    @JsonProperty("rtcs")
    private String rtcs;

    @JsonProperty("mediaServerId")
    private String mediaServerId;

    @JsonProperty("mediaInfo")
    private WvpPlayMediaInfo mediaInfo;

    @JsonProperty("startTime")
    private String startTime;

    @JsonProperty("endTime")
    private String endTime;

    @JsonProperty("downLoadFilePath")
    private String downLoadFilePath;

    @JsonProperty("transcodeStream")
    private Object transcodeStream;

    @JsonProperty("progress")
    private Integer progress;
}
