package com.ym.iot.fertilizer.enums;

import lombok.Getter;

/**
 * 0x0b 实时帧「状态3 / 设备状态1」(uint16) 位定义，对应 docs/mqtt数据表.md 表格4。
 *
 * <p>除 {@link #MANUAL_MODE} 外，备注多为 {@code 0=关 1=开}；{@link #MANUAL_MODE} 为 {@code 0=自动 1=手动}。
 * 不含文档中的「备用」位 14~15。
 *
 * @author ym-cloud
 */
@Getter
public enum FertilizerStatus3Bit {

    /** bit0：1=需要复位标志存在；0=无需复位 */
    RESET(0, 0x0001, "复位标志"),
    START(1, 0x0002, "启动状态"),
    PRIMING(2, 0x0004, "加引水状态"),
    CLEANING(3, 0x0008, "清罐状态"),
    MIX_SWITCH_1(4, 0x0010, "搅拌开关状态1"),
    MIX_SWITCH_2(5, 0x0020, "搅拌开关状态2"),
    MIX_SWITCH_3(6, 0x0040, "搅拌开关状态3"),
    FERT_VALVE_1(7, 0x0080, "施肥阀开关状态1"),
    FERT_VALVE_2(8, 0x0100, "施肥阀开关状态2"),
    FERT_VALVE_3(9, 0x0200, "施肥阀开关状态3"),
    WASH_VALVE(10, 0x0400, "清洗阀开关状态"),
    FERT_PUMP(11, 0x0800, "施肥泵开关状态"),
    MANUAL_MODE(12, 0x1000, "工作模式状态"),
    DEVICE_LOCKED(13, 0x2000, "设备锁定");

    private final int bitIndex;
    private final int mask;
    private final String label;

    FertilizerStatus3Bit(int bitIndex, int mask, String label) {
        this.bitIndex = bitIndex;
        this.mask = mask;
        this.label = label;
    }
}
