package com.ym.iot.api.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class RemoteValveControlLogVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long id;
    private String tenantId;
    private Long deviceId;
    private String deviceCode;
    private String productType;
    private String loraAddr;
    private Integer valveNo;
    private Integer targetPosition;
    private Integer targetPercent;
    private String targetChannel;
    private String targetChannels;
    private String commandType;
    private String commandText;
    private String status;
    private String replyText;
    private String errorCode;
    private String createBy;
    private Date createTime;
}
