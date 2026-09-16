package com.ym.iot.fertilizer.domain.bo;

import lombok.Data;

import java.math.BigDecimal;

/** 单罐业务设置，保留现有 HTTP 请求字段，协议参数转换由 JetLinks 执行。 */
@Data
public class FertilizerTankParamBo {
    /** 罐号 1/2/3 */
    private int tankNo;

    /** 延时施肥时间 (min)，地址 100/104/108 */
    private Integer delay;

    /** 施肥时间 (min)，地址 101/105/109。派生字段：=amount/flow×60，设备自动覆盖 */
    private Integer time;

    /** 施肥流量 (kg/h)，地址 102/106/110。范围按当前罐密度动态计算 */
    private Integer flow;

    /** 预设施肥量 (kg)，地址 103/107/111。null=跳过该罐，0=完整下发零量参数并校验设备回读 */
    private Integer quantity;

    /** 密度只读展示，不再下发；保留字段兼容旧请求 */
    private BigDecimal density;

    /** 兼容旧客户端字段：设备无搅拌功能，服务端忽略，不下发 */
    private Integer mixMode;

    /** 兼容旧客户端字段：设备无搅拌功能，服务端忽略，不下发 */
    private Integer mixTime;

    /** 总施肥量修正 (t)，地址 121/122/123 */
    private BigDecimal totalFertilizer;
}
