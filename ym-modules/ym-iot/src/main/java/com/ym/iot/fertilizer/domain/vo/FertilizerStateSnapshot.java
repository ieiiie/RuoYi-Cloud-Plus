package com.ym.iot.fertilizer.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.iot.fertilizer.enums.FertilizerDeviceState;
import com.ym.iot.fertilizer.enums.FertilizerRiskLevel;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 施肥机设备状态快照。
 *
 * <p>由 {@link FertilizerStateService#onRealtimeFrame} 和
 * {@link FertilizerStateService#onSettingsFrame} 生成。
 * 缓存于内存中，供前端查询和控制服务做前置条件判断。
 *
 * <h3>数据来源</h3>
 * <ul>
 *   <li>0x0B 实时帧 → 主状态/子阶段/模式/风险/报警说明/状态字解析 Map（status1~4Flags）/
 *       总体测点（流量、压力、电压、运行时间、剩余延时、泵频率、三罐液位、三罐已施肥量）/ 三罐作业参数；原始 status 整型仅内存使用</li>
 *   <li>0x0C 设置帧 → softwareVersion / 三罐密度/搅拌；不更新状态字解析与总体测点（沿用上次实时帧）</li>
 * </ul>
 *
 * @author ym-cloud
 */
@Data
@Builder(toBuilder = true)
public class FertilizerStateSnapshot {

    /** 设备主键（iot_device.id） */
    private Long deviceId;

    /** 设备编号（如 yx250110） */
    private String deviceCode;

    /** 设备主状态：OFFLINE/IDLE/READY/RUNNING/STOPPING/FAULT/EMERGENCY */
    private FertilizerDeviceState state;

    /**
     * 运行子阶段，仅 state==RUNNING 有值。
     * PRE_WATER / FERT_1 / FERT_2 / FERT_3 / WASH / PRIME / TANK_CLEAN
     */
    private String subStage;

    /** 工作模式。AUTO=自动（施肥运行模式），MANUAL=手动（现场检修模式） */
    private String workMode;

    /** 风险等级：NORMAL / WARNING / CRITICAL */
    private FertilizerRiskLevel riskLevel;

    /** 当前活跃的施肥罐号 1/2/3，无活跃罐为 null */
    private Integer activeTank;

    /** 三罐标签映射，键为 Tank1/Tank2/Tank3，值为肥料名称。 */
    private Map<String, String> tankTags;

    /** 三罐肥料含义，供前端和移动端直接展示。 */
    private List<TankMaterial> tankMaterials;

    /**
     * 状态1 按位解析结果，键为 {@link com.ym.iot.fertilizer.enums.FertilizerStatus1Bit} 枚举名小写。
     */
    private Map<String, Boolean> status1Flags;

    /**
     * 状态2 按位解析结果，键为 {@link com.ym.iot.fertilizer.enums.FertilizerStatus2Bit} 枚举名小写。
     */
    private Map<String, Boolean> status2Flags;

    /**
     * 状态3 按位解析结果，键为 {@link com.ym.iot.fertilizer.enums.FertilizerStatus3Bit} 枚举名小写。
     */
    private Map<String, Boolean> status3Flags;

    /**
     * 状态4 按位解析结果，键为 {@link com.ym.iot.fertilizer.enums.FertilizerStatus4Bit} 枚举名小写。
     */
    private Map<String, Boolean> status4Flags;

    /**
     * 管路<b>实测</b>瞬时流量 (kg/h)，来自 0x0B 帧 float 字段；停泵或无流时可为 0。
     * 与 {@code t1Flow}/设定类字段不同：后者为三罐「施肥流量设置」，不是流量计当前读数。
     * 精度与 {@link com.ym.iot.fertilizer.adapter.FertilizerRealtimeTelemetryAdapter} 的 {@code flow} 测点一致。
     */
    private Float flow;

    /** 压力 (bar) */
    private Float pressure;

    /** 电压 (V) */
    private Float voltage;

    /** 施肥运行时间 (min) */
    private Integer runTime;

    /** 施肥剩余延时时间 (min) */
    private Integer remainDelayTime;

    /** 泵频率 (Hz) */
    private Integer pumpFreq;

    /**
     * 1 号罐液位 (m)，与遥测 {@code liquid_level1} 同源；保留 3 位小数，与
     * {@link com.ym.iot.fertilizer.adapter.FertilizerRealtimeTelemetryAdapter} 一致。
     */
    private Float liquidLevel1;

    /** 2 号罐液位 (m)，对应 {@code liquid_level2} */
    private Float liquidLevel2;

    /** 3 号罐液位 (m)，对应 {@code liquid_level3} */
    private Float liquidLevel3;

    /** 1号罐已施肥量 (kg)，来自 0x0B 实时帧 */
    private Integer doneFert1Kg;

    /** 2号罐已施肥量 (kg)，来自 0x0B 实时帧 */
    private Integer doneFert2Kg;

    /** 3号罐已施肥量 (kg)，来自 0x0B 实时帧 */
    private Integer doneFert3Kg;

    /**
     * 「状态1 / 报警1」原始 uint16，仅服务层使用；REST JSON 不输出。
     *
     * @see #status1Flags
     */
    @JsonIgnore
    @ToString.Exclude
    private int rawS1;

    @JsonIgnore
    @ToString.Exclude
    private int rawS2;

    @JsonIgnore
    @ToString.Exclude
    private int rawS3;

    @JsonIgnore
    @ToString.Exclude
    private int rawS4;

    /** 已展开的中文报警名，逗号分隔。无报警为 null */
    private String alarmFlags;

    /** 本次状态推导的理由，用于排障 */
    private String stageReason;

    /** 快照生成时间 */
    private Date snapshotTime;

    /** 0x0B 实时帧写入缓存的服务端时间，仅用于控制闭环判定 */
    @JsonIgnore
    @ToString.Exclude
    private Date realtimeUpdatedAt;

    /** 0x0C 设置帧写入缓存的服务端时间，仅用于参数读取判定 */
    @JsonIgnore
    @ToString.Exclude
    private Date settingsUpdatedAt;

    // ======================== 0x0C 设置帧参数 ========================

    /** 设备软件版本号 */
    private Integer softwareVersion;

    /** 1号罐密度 */
    private Float t1Density;
    /** 1号罐搅拌模式：0=不搅拌 1=施肥前 2=施肥中 */
    private Integer t1MixMode;
    /** 1号罐搅拌时间 (min) */
    private Integer t1MixTime;

    /** 2号罐密度 */
    private Float t2Density;
    /** 2号罐搅拌模式 */
    private Integer t2MixMode;
    /** 2号罐搅拌时间 (min) */
    private Integer t2MixTime;

    /** 3号罐密度 */
    private Float t3Density;
    /** 3号罐搅拌模式 */
    private Integer t3MixMode;
    /** 3号罐搅拌时间 (min) */
    private Integer t3MixTime;

    // ======================== 0x0B 实时帧未完成施肥量 ========================

    /** 1号罐未完成施肥量 (kg) */
    private Integer pendingFert1Kg;
    /** 2号罐未完成施肥量 (kg) */
    private Integer pendingFert2Kg;
    /** 3号罐未完成施肥量 (kg) */
    private Integer pendingFert3Kg;

    // ======================== 0x0B 实时帧三罐作业参数 (set1/set2/set3) ========================

    /** 1号罐延时施肥时间 (min) */
    private Integer t1Delay;
    /** 1号罐施肥时间 (min)，设备派生 ≈ amount/该罐设定流量×60 */
    private Integer t1Time;
    /**
     * 1号罐<b>施肥流量设置</b> (kg/h)，对应协议「施肥流量设置 1」，与遥测 {@code fert_flow_set1} 同源。
     * 为设备内目标/下发参数，可与 {@link #flow}（实测瞬时流量）同时存在且数值无关。某罐 {@code tNAmount}=0 表示本轮跳过该罐时，该罐的设定流量仍可能为历史上报值。
     */
    private Integer t1Flow;
    /** 1号罐预设施肥量 (kg)，0=业务上跳过该罐；下发时仍要求设备保存并回读 0 */
    private Integer t1Amount;

    /** 2号罐延时施肥时间 (min) */
    private Integer t2Delay;
    /** 2号罐施肥时间 (min) */
    private Integer t2Time;
    /** 2号罐施肥流量<b>设置</b> (kg/h)，对应 {@code fert_flow_set2}，含义同 {@link #t1Flow} */
    private Integer t2Flow;
    /** 2号罐预设施肥量 (kg)，0=业务上跳过该罐；下发时仍要求设备保存并回读 0 */
    private Integer t2Amount;

    /** 3号罐延时施肥时间 (min) */
    private Integer t3Delay;
    /** 3号罐施肥时间 (min) */
    private Integer t3Time;
    /** 3号罐施肥流量<b>设置</b> (kg/h)，对应 {@code fert_flow_set3}，含义同 {@link #t1Flow} */
    private Integer t3Flow;
    /** 3号罐预设施肥量 (kg)，0=业务上跳过该罐；下发时仍要求设备保存并回读 0 */
    private Integer t3Amount;

    @Data
    @Builder
    public static class TankMaterial {

        /** 罐号，1-3。 */
        private Integer tankNo;

        /** 标签键，固定为 Tank1/Tank2/Tank3。 */
        private String tagKey;

        /** 肥料名称，如 磷肥/钾肥/氮肥。 */
        private String materialName;

        /** 展示名，如 1号罐磷肥。 */
        private String displayName;
    }
}
