package com.ym.iot.fertilizer.support;

import com.ym.iot.fertilizer.domain.vo.FertilizerStateSnapshot;
import com.ym.iot.fertilizer.enums.FertilizerDeviceState;
import com.ym.iot.fertilizer.enums.FertilizerRiskLevel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** 将 JetLinks 已解析的遥测转换为业务状态，缺失数据不补零。 这是无状态转换工具，不连接厂家服务、不缓存设备状态，也不发送指令。 */
public final class FertilizerStateProjector {
    private FertilizerStateProjector() {}

    // ======================== status3 位掩码（mqtt数据表.md 表格4） ========================

    /** 复位标志，bit0。1=需要复位；0=无需复位、可直接启动 */
    static final int S3_RESET = 1 << 0;

    /** status3.bit0=1 表示存在复位标志，须先下发复位命令 */
    static boolean needsReset(int s3) {
        return (s3 & S3_RESET) != 0;
    }

    /** 启动状态，bit1。置位表示设备已启动（进入运行流程） */
    static final int S3_START = 1 << 1;

    /** 加引水状态，bit2。置位表示正在执行加引水 */
    static final int S3_PRIMING = 1 << 2;

    /** 清罐状态，bit3。置位表示正在执行清罐循环 */
    static final int S3_CLEANING = 1 << 3;

    /** 施肥阀1，bit7。置位表示1号罐施肥阀打开 */
    static final int S3_VALVE_1 = 1 << 7;

    /** 施肥阀2，bit8。置位表示2号罐施肥阀打开 */
    static final int S3_VALVE_2 = 1 << 8;

    /** 施肥阀3，bit9。置位表示3号罐施肥阀打开 */
    static final int S3_VALVE_3 = 1 << 9;

    /** 清洗阀，bit10。置位表示执行冲洗 */
    static final int S3_WASH = 1 << 10;

    /** 施肥泵，bit11。置位表示施肥泵运行 */
    static final int S3_PUMP = 1 << 11;

    /** 工作模式，bit12。0=自动（施肥运行模式），1=手动（现场检修/调试） */
    static final int S3_MANUAL = 1 << 12;

    // ======================== status4 位掩码（mqtt数据表.md 表格5） ========================

    /** 故障停机事件位：bit5 泵故障停机 | bit6 流量低停机 | bit7 液位低停机 */
    static final int S4_FAULT_STOP = (1 << 5) | (1 << 6) | (1 << 7);

    /** 指令操作成功，bit9。设备报告命令执行成功 */
    static final int S4_CMD_OK = 1 << 9;

    /** 指令操作失败，bit10。设备报告命令执行失败 */
    static final int S4_CMD_FAIL = 1 << 10;

    // ======================== 关键报警掩码 ========================

    /**
     * status1 关键报警位（mqtt数据表.md 表格2 "关键报警"列）。
     *
     * <p>仅这些位触发 FAULT_STOPPED 或阻止启动。非关键位（如流量高报、压力高报） 不影响状态判定。
     *
     * <p>包含：flow_down(0) flow_low(2) pressure_down(3) pressure_low(5) voltage_down(6)
     * voltage_low(8) pump_fault(9) daq_fault(10)
     */
    static final int CRITICAL_S1 =
            (1 << 0) | (1 << 2) | (1 << 3) | (1 << 5) | (1 << 6) | (1 << 8) | (1 << 9) | (1 << 10);

    /**
     * status2 关键报警位（mqtt数据表.md 表格3 "关键报警"列）。
     *
     * <p>仅液位低报为关键：level_low_1(2) level_low_2(6) level_low_3(10)
     */
    static final int CRITICAL_S2 = (1 << 2) | (1 << 6) | (1 << 10);

    public static FertilizerStateSnapshot fromCoreTelemetry(
            Long deviceId, String deviceCode, Map<String, Object> values, Date sourceTime) {
        FertilizerStateSnapshot snapshot =
                FertilizerStateSnapshot.builder()
                        .deviceId(deviceId)
                        .deviceCode(deviceCode)
                        .snapshotTime(sourceTime)
                        .build();
        Map<String, Object> bean = new java.util.LinkedHashMap<>(values);
        Map<String, String> mapping = new java.util.LinkedHashMap<>();
        mapping.put("run_time", "runTime");
        mapping.put("remain_delay_time", "remainDelayTime");
        mapping.put("pump_freq", "pumpFreq");
        for (int tank = 1; tank <= 3; tank++) {
            mapping.put("liquid_level" + tank, "liquidLevel" + tank);
            mapping.put("done_fertilizer" + tank, "doneFert" + tank + "Kg");
            mapping.put("pending_fertilizer" + tank, "pendingFert" + tank + "Kg");
            mapping.put("fert_delay_set" + tank, "t" + tank + "Delay");
            mapping.put("fert_time_set" + tank, "t" + tank + "Time");
            mapping.put("fert_flow_set" + tank, "t" + tank + "Flow");
            mapping.put("fert_quantity_set" + tank, "t" + tank + "Amount");
            mapping.put("density" + tank, "t" + tank + "Density");
            mapping.put("mix_mode" + tank, "t" + tank + "MixMode");
            mapping.put("mix_time" + tank, "t" + tank + "MixTime");
        }
        mapping.forEach(
                (key, target) -> {
                    if (values.containsKey(key)) bean.put(target, values.get(key));
                });
        cn.hutool.core.bean.BeanUtil.fillBeanWithMap(bean, snapshot, false);
        if (values.get("status1") instanceof Number n1
                && values.get("status2") instanceof Number n2
                && values.get("status3") instanceof Number n3
                && values.get("status4") instanceof Number n4) {
            int s1 = n1.intValue(), s2 = n2.intValue(), s3 = n3.intValue(), s4 = n4.intValue();
            StateResult result = derive(s1, s2, s3, s4);
            snapshot.setState(result.state());
            snapshot.setRiskLevel(result.risk());
            snapshot.setStageReason(result.reason());
            snapshot.setWorkMode((s3 & S3_MANUAL) != 0 ? "MANUAL" : "AUTO");
            snapshot.setSubStage(
                    result.state() == FertilizerDeviceState.RUNNING ? deriveSubStage(s3) : null);
            snapshot.setActiveTank(activeTank(s3));
            snapshot.setRawS1(s1);
            snapshot.setRawS2(s2);
            snapshot.setRawS3(s3);
            snapshot.setRawS4(s4);
            snapshot.setStatus1Flags(FertilizerStatusWordDecode.decodeS1(s1));
            snapshot.setStatus2Flags(FertilizerStatusWordDecode.decodeS2(s2));
            snapshot.setStatus3Flags(FertilizerStatusWordDecode.decodeS3(s3));
            snapshot.setStatus4Flags(FertilizerStatusWordDecode.decodeS4(s4));
            snapshot.setAlarmFlags(alarmFlags(s1, s2, s4));
        }
        return snapshot;
    }

    static StateResult derive(int s1, int s2, int s3, int s4) {
        // P1: 故障停机 — status4 故障停机位（泵故障停/流量低停/液位低停）或泵故障报警，且设备不在运行态
        if (((s4 & S4_FAULT_STOP) != 0 || (s1 & (1 << 9)) != 0) && (s3 & S3_START) == 0) {
            return new StateResult(
                    FertilizerDeviceState.FAULT, FertilizerRiskLevel.CRITICAL, "故障停机位或泵故障存在且不在运行态");
        }
        // P2: 清罐中 — 虽然属于运行，但风险等级为 WARNING
        if ((s3 & S3_CLEANING) != 0) {
            return new StateResult(
                    FertilizerDeviceState.RUNNING, FertilizerRiskLevel.WARNING, "清罐状态下正在运行");
        }
        // P3: 启动状态位 — 设备正在运行（具体阶段由 subStage 表达）
        if ((s3 & S3_START) != 0) {
            return new StateResult(
                    FertilizerDeviceState.RUNNING, FertilizerRiskLevel.NORMAL, "启动态存在，设备运行中");
        }
        // P4: 无复位标志（bit0=0）且无关键报警 → READY（可直接启动）
        if (!needsReset(s3) && !hasCritical(s1, s2)) {
            return new StateResult(
                    FertilizerDeviceState.READY, FertilizerRiskLevel.NORMAL, "无需复位，可启动");
        }
        // P5: 需要复位且有关键报警 → FAULT
        if (needsReset(s3) && hasCritical(s1, s2)) {
            return new StateResult(
                    FertilizerDeviceState.FAULT, FertilizerRiskLevel.CRITICAL, "需要复位且有关键报警");
        }
        // P6: 有关键报警 → FAULT
        if (hasCritical(s1, s2)) {
            return new StateResult(
                    FertilizerDeviceState.FAULT, FertilizerRiskLevel.CRITICAL, "关键报警存在");
        }
        // P7: 需要复位（bit0=1）且无关键报警 → IDLE（待复位）
        if (needsReset(s3)) {
            return new StateResult(FertilizerDeviceState.IDLE, FertilizerRiskLevel.NORMAL, "需要复位");
        }
        // P8: 默认 → IDLE
        return new StateResult(
                FertilizerDeviceState.IDLE, FertilizerRiskLevel.NORMAL, "在线且无报警、无运行");
    }

    private static boolean hasCritical(int s1, int s2) {
        return (s1 & CRITICAL_S1) != 0 || (s2 & CRITICAL_S2) != 0;
    }

    static String deriveSubStage(int s3) {
        if ((s3 & S3_VALVE_1) != 0) return "FERT_1"; // 1号罐施肥
        if ((s3 & S3_VALVE_2) != 0) return "FERT_2"; // 2号罐施肥
        if ((s3 & S3_VALVE_3) != 0) return "FERT_3"; // 3号罐施肥
        if ((s3 & S3_PRIMING) != 0) return "PRIME"; // 加引水
        if ((s3 & S3_CLEANING) != 0) return "TANK_CLEAN"; // 清罐
        if ((s3 & S3_WASH) != 0) return "WASH"; // 冲洗
        if ((s3 & S3_START) != 0 && (s3 & S3_PUMP) != 0) return "PRE_WATER"; // 泵运行 → 预灌溉
        if ((s3 & S3_START) != 0) return "RUNNING"; // 仅启动位 → 运行中(阶段未明细)
        return null;
    }

    static Integer activeTank(int s3) {
        if ((s3 & S3_VALVE_1) != 0) return 1;
        if ((s3 & S3_VALVE_2) != 0) return 2;
        if ((s3 & S3_VALVE_3) != 0) return 3;
        return null;
    }

    private static String alarmFlags(int s1, int s2, int s4) {
        List<String> flags = new ArrayList<>();
        // status1（表格2）
        if ((s1 & (1 << 0)) != 0) flags.add("流量断线");
        if ((s1 & (1 << 1)) != 0) flags.add("流量高报");
        if ((s1 & (1 << 2)) != 0) flags.add("流量低报");
        if ((s1 & (1 << 3)) != 0) flags.add("压力断线");
        if ((s1 & (1 << 4)) != 0) flags.add("压力高报");
        if ((s1 & (1 << 5)) != 0) flags.add("压力低报");
        if ((s1 & (1 << 6)) != 0) flags.add("电压断线");
        if ((s1 & (1 << 7)) != 0) flags.add("电压高报");
        if ((s1 & (1 << 8)) != 0) flags.add("电压低报");
        if ((s1 & (1 << 9)) != 0) flags.add("泵故障");
        if ((s1 & (1 << 10)) != 0) flags.add("数据采集故障");
        // status2（表格3）
        if ((s2 & (1 << 0)) != 0) flags.add("液位断线1");
        if ((s2 & (1 << 2)) != 0) flags.add("液位低报1");
        if ((s2 & (1 << 4)) != 0) flags.add("液位断线2");
        if ((s2 & (1 << 6)) != 0) flags.add("液位低报2");
        if ((s2 & (1 << 8)) != 0) flags.add("液位断线3");
        if ((s2 & (1 << 10)) != 0) flags.add("液位低报3");
        // status4 故障停机事件位（表格5）
        if ((s4 & (1 << 5)) != 0) flags.add("泵故障停机");
        if ((s4 & (1 << 6)) != 0) flags.add("流量低停机");
        if ((s4 & (1 << 7)) != 0) flags.add("液位低停机");
        return flags.isEmpty() ? null : String.join(",", flags);
    }

    record StateResult(FertilizerDeviceState state, FertilizerRiskLevel risk, String reason) {}
}
