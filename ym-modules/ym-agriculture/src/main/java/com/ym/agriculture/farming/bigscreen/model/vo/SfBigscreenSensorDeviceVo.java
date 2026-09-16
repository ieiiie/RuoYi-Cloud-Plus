package com.ym.agriculture.farming.bigscreen.model.vo;

import com.ym.iot.api.domain.vo.RemotePestChartVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大屏单台传感器设备及遥测。
 */
@Data
public class SfBigscreenSensorDeviceVo implements Serializable {

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

    /** 传感器子类型：weather / soil / pest / unknown */
    private String sensorSubType;

    /** 是否在线 */
    private Boolean online;

    /** ONLINE / OFFLINE / UNKNOWN；待确认不能视为离线。 */
    private String onlineStatus;

    /** 最新测点值 Map，key 为测点编码 */
    private Map<String, Object> metrics = new LinkedHashMap<>();

    /** 测点单位 Map */
    private Map<String, String> units = new LinkedHashMap<>();

    /** 土壤分层数据（hfzk-2） */
    private List<SfBigscreenSoilDepthVo> soilDepths = new ArrayList<>();

    /** 虫情图表（hfzk-3） */
    private RemotePestChartVo pestChart;
}
