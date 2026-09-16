package com.ym.iot.motorvalve.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * 电动阀控制参数。
 *
 * <p>单设备一次只控制一个阀门；{@code valveNo} 未传时默认为 1。
 * {@code position} 为 0-360 度整数；{@link #action} 仅为快捷预设。
 */
@Data
public class ValveControlBo {

    /**
     * 控制动作（4G 直连必填），与 GET control-profile 返回的 action 一致。
     * 如 close、open_a、open_b、open_c、open_d。
     */
    private String action;

    /**
     * 阀门类型编码（single_port / three_way_l / three_way_t / five_port）；
     * 未传时从设备 configJson.motorvalveValveType 读取。
     */
    private String valveType;

    /** 阀门编号，从 1 开始；未传时默认 1 */
    private Integer valveNo;

    /** 目标角度，单位：度，允许 0-360 */
    private Integer position;

    /** LoRa 子阀地址列表；业务上一次只允许 0 或 1 个地址，4G 直连禁止传 */
    private List<String> loraAddrs;
}
