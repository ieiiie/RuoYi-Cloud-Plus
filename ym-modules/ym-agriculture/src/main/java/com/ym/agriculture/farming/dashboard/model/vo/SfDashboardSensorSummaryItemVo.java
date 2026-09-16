package com.ym.agriculture.farming.dashboard.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 传感器汇总中单台设备（隶属某一 {@link SfDashboardProductVo}）。
 *
 * @author ym-cloud
 */
@Data
public class SfDashboardSensorSummaryItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private Boolean online;

    /** ONLINE / OFFLINE / UNKNOWN；待确认不能视为离线。 */
    private String onlineStatus;
}
