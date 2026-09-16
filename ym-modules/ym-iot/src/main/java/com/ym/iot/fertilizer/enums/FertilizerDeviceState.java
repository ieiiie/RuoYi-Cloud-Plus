package com.ym.iot.fertilizer.enums;

import com.ym.iot.fertilizer.domain.vo.FertilizerStateSnapshot;

/**
 * 施肥机设备主状态。
 *
 * <p>7 个主状态由实时帧 status1~4 的位推导得出（{@link FertilizerStateService#derive}）。
 * 设备运行中的具体工艺阶段（1号罐施肥中 / 冲洗中等）通过
 * {@link FertilizerStateSnapshot#getSubStage()} 表达，不参与主状态机。
 *
 * <h3>状态速查</h3>
 * <table>
 *   <tr><th>状态</th><th>含义</th><th>可操作</th></tr>
 *   <tr><td>OFFLINE</td><td>30s 无实时帧或 MQTT 断连</td><td>全部禁用</td></tr>
 *   <tr><td>IDLE</td><td>在线待机；status3.bit0=1 时表示需要复位</td><td>复位、设参</td></tr>
 *   <tr><td>READY</td><td>status3.bit0=0，无需复位，可启动</td><td>启动、设参</td></tr>
 *   <tr><td>RUNNING</td><td>运行中（施肥/冲洗/清罐）</td><td>停止、急停</td></tr>
 *   <tr><td>STOPPING</td><td>有序停止中</td><td>急停</td></tr>
 *   <tr><td>FAULT</td><td>故障停机</td><td>复位（需先排障）</td></tr>
 *   <tr><td>EMERGENCY</td><td>紧急停止</td><td>复位（需人工现场确认）</td></tr>
 * </table>
 *
 * @author ym-cloud
 */
public enum FertilizerDeviceState {

    /** 30s 无实时帧或 MQTT 断连，不可操作 */
    OFFLINE,

    /** 在线、无报警、待机 */
    IDLE,

    /** 无需复位（status3.bit0=0 且无关键报警），可启动施肥 */
    READY,

    /** 运行中（status3.bit1=1 或施肥阀/清罐/引水活跃），子阶段见 {@code FertilizerStateSnapshot.subStage} */
    RUNNING,

    /** 有序停止中（收到停止命令后、设备完成冲洗进入待机前的过渡态） */
    STOPPING,

    /** 故障停机（关键报警位或 status4 故障停机事件触发），需人工排查后复位 */
    FAULT,

    /** 紧急停止（未复位前维持此状态），必须人工现场确认安全后复位才能重新启动 */
    EMERGENCY
}
