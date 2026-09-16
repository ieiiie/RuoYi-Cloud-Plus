package com.ym.iot.motorvalve.domain.vo;

import com.ym.iot.motorvalve.domain.ValveControlLog;

import io.github.linpeilie.annotations.AutoMapper;

import lombok.Data;

import java.util.Date;

/**
 * 电动阀控制操作日志视图对象。
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = ValveControlLog.class)
public class ValveControlLogVo {

    /** 主键 */
    private Long id;

    /** 租户编号 */
    private String tenantId;

    /** 设备ID */
    private Long deviceId;

    /** 设备编号 */
    private String deviceCode;

    /** 产品类型 */
    private String productType;

    /** LoRa阀门地址 */
    private String loraAddr;

    /** 阀门编号 */
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

    /** 命令类型 */
    private String commandType;

    /** 下发指令原文 */
    private String commandText;

    /** 状态 */
    private String status;

    /** 应答原文 */
    private String replyText;

    /** 错误码 */
    private String errorCode;

    /** 操作人 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;
}
