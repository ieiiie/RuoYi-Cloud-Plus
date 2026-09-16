package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 大屏阀门台账项。
 */
@Data
public class SfBigscreenValveItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备主键 */
    private Long deviceId;

    /** 设备编号 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 产品标识 */
    private String productKey;

    /** 是否在线（档案在线状态） */
    private Boolean online;

    /** ONLINE / OFFLINE / UNKNOWN；待确认不能视为离线。 */
    private String onlineStatus;

    /** 1 号阀口角度，单位：度；分体阀设备级状态还会聚合 2 号阀口 */
    private BigDecimal angleDeg;

    /** 瞬时流量，单位：m³/h */
    private BigDecimal flowRateM3h;

    /** 是否打开：在线且任一有效阀口或较新会话判定为开启；离线恒为 false */
    private Boolean open;

    /** 是否灌溉中：在线且瞬时流量大于 0 */
    private Boolean irrigating;

    /** 开/关状态文案：开启 / 关闭 / 未定档（中间角，非协议离散档位） */
    private String switchStatus;
}
