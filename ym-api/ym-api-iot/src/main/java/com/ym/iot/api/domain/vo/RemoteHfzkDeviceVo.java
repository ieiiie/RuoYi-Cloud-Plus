package com.ym.iot.api.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class RemoteHfzkDeviceVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long id;
    private String externalDeviceId;
    private String deviceSn;
    private String deviceType;
    private String deviceName;
    private String longitude;
    private String latitude;
    private String externalUserId;
    private String remark;
    private String extraInfo;
    private Date lastSyncTime;
    private Date createTime;
    private Date updateTime;
    private String tenantId;
}
