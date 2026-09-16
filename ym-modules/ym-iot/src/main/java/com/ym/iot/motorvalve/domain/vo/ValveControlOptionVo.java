package com.ym.iot.motorvalve.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 电动阀可控档位（前端按钮/选项）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValveControlOptionVo {

    /** 控制动作编码，如 close、open_a、open_b */
    private String action;

    /** 展示文案 */
    private String label;

    /** 对应 MQTT 目标位置（度） */
    private Integer position;
}
