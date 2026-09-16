package com.ym.iot.api.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 设备跨服务摘要。
 */
@Data
public class RemoteDeviceSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long deviceId;
    private String tenantId;
    private Long productId;
    private String productName;
    private String productKey;
    private String deviceCode;
    private String deviceName;
    private Integer sortOrder;
    private String imei;
    private String mac;
    private String deviceCategory;
    private BigDecimal lng;
    private BigDecimal lat;
    private String onlineStatus;
    private Date lastReportTime;
    private Date lastRemoteFetchTime;
    private Date lastOfflineTime;
    private String ipAddress;
    private String protocol;
    private String firmwareVersion;
    private String status;
    private Date createTime;
    private Date updateTime;
    private String remark;
}
