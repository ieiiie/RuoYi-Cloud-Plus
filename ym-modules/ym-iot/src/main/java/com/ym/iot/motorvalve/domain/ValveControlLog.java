package com.ym.iot.motorvalve.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.util.Date;

/**
 * 电动阀控制操作日志实体。
 *
 * @author ym-cloud
 */
@Data
@TableName("iot_motorvalve_control_log")
public class ValveControlLog {

    /** 主键（雪花） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户编号 */
    private String tenantId;

    /** 设备ID */
    private Long deviceId;

    /** 设备编号 */
    private String deviceCode;

    /** 产品类型 */
    private String productType;

    /** LoRa阀门地址（4G设备为空） */
    private String loraAddr;

    /** 阀门编号，从1开始 */
    private Integer valveNo;

    /** 目标位置（度） */
    private Integer targetPosition;

    /** 目标百分比（0-100）；仅百分比控制时有值 */
    private Integer targetPercent;

    /** 目标通道（A/B/C/D）；V1.0 百分比控制三通/五通时有值，单通阀为空 */
    private String targetChannel;

    /** @deprecated V1.0 改用 targetChannel。旧的多通道 JSON，保留以兼容历史数据读取 */
    @Deprecated
    private String targetChannels;

    /** 命令类型：CONTROL/READ_DATA/READ_STATUS/NTP_SYNC */
    private String commandType;

    /** 下发指令原文 */
    private String commandText;

    /** 状态：SENT/ACK/SUCCESS/FAILED/REJECTED */
    private String status;

    /** 应答原文 */
    private String replyText;

    /** 错误码 */
    private String errorCode;

    /** 操作人 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;
}
