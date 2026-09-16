package com.ym.iot.fertilizer.enums;

import lombok.Getter;

/**
 * 0x0b 实时帧「状态1/报警1」(uint16) 位定义，对应 docs/mqtt数据表.md §表格2。
 *
 * <p>语义统一为 {@code 0=正常 1=报警}；位 11~15 为厂家保留。
 *
 * <p>位级告警在 {@code FertilizerAlarmEdgeService} 内做 0->1 边沿差分，
 * 命中后新增一条 {@code iot_alert_record} ({@code alert_type='FERTILIZER_BIT'},
 * {@code property_identifier='status1.<name>'} )；1->0 不自动恢复，需人工处理。
 *
 * @author ym-cloud
 */
@Getter
public enum FertilizerStatus1Bit {

    FLOW_OFFLINE(0,        0x0001, "流量断线"),
    FLOW_HIGH(1,           0x0002, "流量高报"),
    FLOW_LOW(2,            0x0004, "流量低报"),
    PRESSURE_OFFLINE(3,    0x0008, "压力断线"),
    PRESSURE_HIGH(4,       0x0010, "压力高报"),
    PRESSURE_LOW(5,        0x0020, "压力低报"),
    VOLTAGE_OFFLINE(6,     0x0040, "电压断线"),
    VOLTAGE_HIGH(7,        0x0080, "电压高报"),
    VOLTAGE_LOW(8,         0x0100, "电压低报"),
    PUMP_FAULT(9,          0x0200, "泵故障"),
    DATA_ACQ_FAULT(10,     0x0400, "数据采集故障");

    private final int bitIndex;
    private final int mask;
    private final String label;

    FertilizerStatus1Bit(int bitIndex, int mask, String label) {
        this.bitIndex = bitIndex;
        this.mask = mask;
        this.label = label;
    }

    /**
     * 位级 identifier，统一前缀 {@code status1.}，便于在 iot_alert_record
     * 的 property_identifier 列查询/分组。
     */
    public String identifier() {
        return "status1." + name().toLowerCase();
    }
}
