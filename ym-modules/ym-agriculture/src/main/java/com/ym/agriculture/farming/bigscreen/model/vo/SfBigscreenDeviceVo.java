package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 大屏地图设备点。
 */
@Data
public class SfBigscreenDeviceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备主键 */
    private Long deviceId;

    /** 设备编号 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 产品标识 product_key */
    private String productKey;

    /** 设备大类 device_category */
    private String deviceCategory;

    /** 地图图层类型：sensor / camera / valve / facility / uav */
    private String layerType;

    /** 传感器子类型（仅 sensor 层） */
    private String sensorSubType;

    /** 虚拟分区：main / zone210 / zone49 / zone43 */
    private String virtualZone;

    /** 虚拟分区中文标签 */
    private String virtualZoneLabel;

    /** 经度；设备未配置坐标时为 null（不使用地块中心兜底） */
    private BigDecimal lng;

    /** 纬度；设备未配置坐标时为 null（不使用地块中心兜底） */
    private BigDecimal lat;

    /** 是否在线 */
    private Boolean online;

    /** ONLINE / OFFLINE / UNKNOWN；待确认不能视为离线。 */
    private String onlineStatus;

    /** 地图默认图标 URL，来自 iot_product.map_icon_url */
    private String mapIconUrl;

    /** 选中态图标 URL，来自 iot_product.map_selected_icon_url（可选） */
    private String mapSelectedIconUrl;
}
