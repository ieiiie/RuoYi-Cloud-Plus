package com.ym.agriculture.farming.dashboard.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 农田地图上单个传感器锚点。
 *
 * @author ym-cloud
 */
@Data
public class SfDashboardSensorMarkerVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 物联网设备主键（与 {@code /iot/device/{deviceId}/series} 一致）
     */
    private Long deviceId;

    /**
     * 设备编号（SN，与地块绑定 {@code sf_field_iot.device_sn} 一致）
     */
    private String deviceCode;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备大类（展示分组用）
     */
    private String deviceCategory;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 经度（无则回落为地块中心）
     */
    private BigDecimal lng;

    /**
     * 纬度
     */
    private BigDecimal lat;

    /**
     * 是否在线（最后上报在 5 分钟内）
     */
    private Boolean online;

    /** ONLINE / OFFLINE / UNKNOWN；待确认不能视为离线。 */
    private String onlineStatus;

    /**
     * 最后上报时间
     */
    private Date lastReportTime;

    /**
     * 所属地块 ID
     */
    private Long fieldId;
}
