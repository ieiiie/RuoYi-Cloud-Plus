package com.ym.iot.motorvalve.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 电动阀百分比控制下发结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValvePercentControlResultVo {

    /**
     * 实际使用的阀门类型编码，统一为 single_port / three_way_l / three_way_t / five_port。
     */
    private String valveType;

    /**
     * 百分比换算后的协议控制角度，单位：度。
     */
    private Integer angle;

    /**
     * 实际下发的 MQTT 指令文本。
     */
    private String commandText;

    /**
     * 选中的控制通道（A/B/C/D），单通阀为 null。
     */
    private String channel;

    /**
     * 目标角度对应的状态文案，如「A口全开」「全关」「67°」，前端直接展示。
     */
    private String angleLabel;
}
