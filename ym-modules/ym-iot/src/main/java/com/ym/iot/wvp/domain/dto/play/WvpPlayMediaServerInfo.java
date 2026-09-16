package com.ym.iot.wvp.domain.dto.play;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * {@code mediaInfo.mediaServer} 节点。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WvpPlayMediaServerInfo {

    private String id;
    private String ip;
    private String hookIp;
    private String sdpIp;
    private String streamIp;
    private Integer httpPort;
    private Integer httpSSlPort;
    private Integer rtmpPort;
    private Integer flvPort;
    private Integer flvSSLPort;
    private Integer wsFlvPort;
    private Integer wsFlvSSLPort;
    private Integer rtmpSSlPort;
    private Integer rtpProxyPort;
    private Integer rtspPort;
    private Integer rtspSSLPort;
    private Boolean autoConfig;
    private String secret;
    private Integer hookAliveInterval;
    private Boolean rtpEnable;
    private Boolean status;
    private String rtpPortRange;
    private String sendRtpPortRange;
    private Integer recordAssistPort;
    private String createTime;
    private String updateTime;
    private String lastKeepaliveTime;
    private Boolean defaultServer;
    private Integer recordDay;
    private String recordPath;
    private String type;
    private String transcodeSuffix;
    private String serverId;
}
