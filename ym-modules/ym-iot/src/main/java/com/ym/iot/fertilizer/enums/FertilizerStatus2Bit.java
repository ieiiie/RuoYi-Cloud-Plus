package com.ym.iot.fertilizer.enums;

import lombok.Getter;

/**
 * 0x0b 实时帧「状态2/报警2」(uint16) 位定义，对应 docs/mqtt数据表.md §表格3。
 *
 * <p>语义统一为 {@code 0=正常 1=报警}；位 12~15 为厂家保留。
 *
 * <p>位级告警在 {@code FertilizerAlarmEdgeService} 内做 0->1 边沿差分，
 * 命中后新增一条 {@code iot_alert_record} ({@code alert_type='FERTILIZER_BIT'},
 * {@code property_identifier='status2.<name>'} )；1->0 不自动恢复，需人工处理。
 *
 * @author ym-cloud
 */
@Getter
public enum FertilizerStatus2Bit {

    LIQUID1_OFFLINE(0,  0x0001, "液位1 断线"),
    LIQUID1_HIGH(1,     0x0002, "液位1 高报"),
    LIQUID1_LOW(2,      0x0004, "液位1 低报"),
    LIQUID1_FAULT(3,    0x0008, "液位1 故障"),
    LIQUID2_OFFLINE(4,  0x0010, "液位2 断线"),
    LIQUID2_HIGH(5,     0x0020, "液位2 高报"),
    LIQUID2_LOW(6,      0x0040, "液位2 低报"),
    LIQUID2_FAULT(7,    0x0080, "液位2 故障"),
    LIQUID3_OFFLINE(8,  0x0100, "液位3 断线"),
    LIQUID3_HIGH(9,     0x0200, "液位3 高报"),
    LIQUID3_LOW(10,     0x0400, "液位3 低报"),
    LIQUID3_FAULT(11,   0x0800, "液位3 故障");

    private final int bitIndex;
    private final int mask;
    private final String label;

    FertilizerStatus2Bit(int bitIndex, int mask, String label) {
        this.bitIndex = bitIndex;
        this.mask = mask;
        this.label = label;
    }

    public String identifier() {
        return "status2." + name().toLowerCase();
    }
}
