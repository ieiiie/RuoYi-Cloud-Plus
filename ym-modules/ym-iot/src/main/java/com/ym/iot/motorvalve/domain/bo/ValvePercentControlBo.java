package com.ym.iot.motorvalve.domain.bo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 电动阀百分比控制参数。
 *
 * <p>平台先将百分比换算为厂商协议角度，再复用普通控阀链路下发。
 */
@Data
public class ValvePercentControlBo {

    /**
     * 阀门类型编码，可选。
     * 支持 single_port / single_valve / three_way_l / three_way_t / five_port / five_way；
     * 未传时从设备档案解析。
     */
    private String valveType;

    /**
     * 阀门编号，从 1 开始；未传时默认 1。
     */
    private Integer valveNo;

    /**
     * 开度百分比，范围 0-100。
     * <p>单通阀：整体开度百分比，必填。
     * <p>三通/五通：选中通道的开度百分比，必填。
     */
    private Integer percent;

    /**
     * 选中通道，A/B/C/D（大小写不敏感）；三通/五通必填，单通阀忽略。
     * <p>V1.0 统一接口：每次只控制一个通道。
     */
    private String channel;

    /**
     * @deprecated V1.0 改用 {@link #channel}（单通道选择）+ {@link #percent}。
     * 该字段仅保留兼容旧请求，新计算器不再读取；传入会被忽略。
     */
    @Deprecated
    private Map<String, Integer> channels;

    /**
     * LoRa 子阀地址列表；业务上一次只允许 0 或 1 个地址，4G 直连禁止传。
     */
    private List<String> loraAddrs;
}
