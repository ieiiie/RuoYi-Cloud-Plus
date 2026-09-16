package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 同子类型传感器分组（weather / soil / pest）。
 */
@Data
public class SfBigscreenSensorGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 子类型：weather / soil / pest */
    private String sensorSubType;

    /** 同类型全部设备 */
    private List<SfBigscreenSensorDeviceVo> devices = new ArrayList<>();

    /** 主展示设备（在线优先） */
    private SfBigscreenSensorDeviceVo primary;
}
