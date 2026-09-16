package com.ym.iot.motorvalve.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 设备控阀能力描述（4G 直连 / 网关）。
 */
@Data
public class ValveControlProfileVo {

    /** 是否 4G 直连（非网关 LoRa 汇聚） */
    private boolean directConnect;

    /** 阀门类型编码 */
    private String valveType;

    /** 阀门类型名称 */
    private String valveTypeLabel;

    /** 阀位测点范围，按 valveNo 对应 K01~K04 */
    private String positionTelemetryKey;

    /** 可选控制档位 */
    private List<ValveControlOptionVo> options;
}
