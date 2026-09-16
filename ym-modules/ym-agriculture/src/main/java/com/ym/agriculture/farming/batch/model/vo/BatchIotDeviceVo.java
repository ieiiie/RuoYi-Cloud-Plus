package com.ym.agriculture.farming.batch.model.vo;

import com.ym.iot.api.domain.vo.RemoteLatestTelemetryVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 种植批次关联的物联网设备 + 最新测点数据。
 *
 * @author ym-cloud
 */
@Data
public class BatchIotDeviceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private String deviceCategory;

    private String productName;

    private String onlineStatus;

    private Date lastReportTime;

    private BigDecimal lng;

    private BigDecimal lat;

    /** 设备各测点最新上报数据 */
    private RemoteLatestTelemetryVo latest;
}
