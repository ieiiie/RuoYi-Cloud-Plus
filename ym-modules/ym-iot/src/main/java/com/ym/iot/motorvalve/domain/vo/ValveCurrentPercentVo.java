package com.ym.iot.motorvalve.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 电动阀当前百分比状态——从遥测角度反向换算而来。
 *
 * <p>单通阀返回 percent（0-100）；
 * 三通/五通返回 selectedOutlet（当前选中出口方向）+ outletPercent（出口百分比）+ channels（全通道映射）。
 */
@Data
public class ValveCurrentPercentVo {

    /** 阀门类型编码 */
    private String valveType;

    /** 阀门类型标签 */
    private String valveTypeLabel;

    /** 是否为单通阀 */
    private boolean singlePort;

    /** 各阀门的百分比状态（多阀设备包含多个） */
    private List<ValvePercentItem> valves;

    /**
     * 单个阀门的百分比状态。
     */
    @Data
    public static class ValvePercentItem {

        /** 阀门编号，从 1 开始 */
        private Integer valveNo;

        /** 当前遥测角度（度） */
        private Integer angle;

        /** 单通阀百分比（0-100）；三通/五通为选中通道的百分比 */
        private Integer percent;

        /** V1.0：当前选中的控制通道（A/B/C/D），全关或单通阀为空字符串 */
        private String channel;

        /** 三通/五通：当前选中的出口方向（A/B/C/D），空字符串表示全关；与 channel 同义，保留兼容 */
        private String selectedOutlet;

        /** 三通/五通：选中出口的开度百分比（0-100） */
        private Integer outletPercent;

        /** 三通/五通：完整通道百分比映射，如 {"A":0,"B":100}；单通阀为 null */
        private Map<String, Integer> channels;
    }
}
