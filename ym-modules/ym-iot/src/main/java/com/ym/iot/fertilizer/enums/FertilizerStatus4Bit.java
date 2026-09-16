package com.ym.iot.fertilizer.enums;

import lombok.Getter;

/**
 * 0x0b 实时帧「状态4 / 设备状态2」(uint16) 位定义，对应 docs/mqtt数据表.md 表格5。
 *
 * <p>含阶段/事件/停机/指令回执等标志；部分位为脉冲型（设备侧可能上传后清零）。不含「备用」位 14~15。
 *
 * @author ym-cloud
 */
@Getter
public enum FertilizerStatus4Bit {

    FERT_START(0, 0x0001, "施肥开始"),
    FERT_END(1, 0x0002, "施肥结束"),
    CLEAN_START(2, 0x0004, "清罐开始"),
    CLEAN_END(3, 0x0008, "清罐结束"),
    PRIME_END(4, 0x0010, "引水结束"),
    PUMP_FAULT_STOP(5, 0x0020, "泵故障停机"),
    FLOW_LOW_STOP(6, 0x0040, "流量低停机"),
    LEVEL_LOW_STOP(7, 0x0080, "液位低停机"),
    LOC_FAIL(8, 0x0100, "定位失败"),
    CMD_OK(9, 0x0200, "指令操作成功"),
    CMD_FAIL(10, 0x0400, "指令操作失败"),
    DEVICE_POWER_ON(11, 0x0800, "设备开机标志"),
    DEVICE_POWER_OFF(12, 0x1000, "设备关机标志"),
    LEVEL_CHANGE(13, 0x2000, "液位变化");

    private final int bitIndex;
    private final int mask;
    private final String label;

    FertilizerStatus4Bit(int bitIndex, int mask, String label) {
        this.bitIndex = bitIndex;
        this.mask = mask;
        this.label = label;
    }
}
