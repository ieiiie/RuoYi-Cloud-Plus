package com.ym.iot.api.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class RemoteValveSessionVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long id;
    private String tenantId;
    private Long deviceId;
    private String deviceCode;
    private Integer valveNo;
    private String valveType;
    private Integer openPosition;
    private Integer closePosition;
    private Date openTime;
    private Date closeTime;
    private Integer durationSeconds;
    private Integer currentDurationSeconds;
    private String status;
    private Long openControlLogId;
    private Long closeControlLogId;
    private String openBy;
    private String closeBy;
    private String remark;
    private Date createTime;
}
