package com.ym.iot.motorvalve.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ym.iot.motorvalve.domain.ValveSession;

import io.github.linpeilie.annotations.AutoMapper;

import lombok.Data;

import java.util.Date;

/**
 * 电动阀开阀会话视图对象。
 */
@Data
@AutoMapper(target = ValveSession.class)
public class ValveSessionVo {

    /** 主键 */
    private Long id;

    /** 租户编号 */
    private String tenantId;

    /** 设备ID */
    private Long deviceId;

    /** 设备编号 */
    private String deviceCode;

    /** 阀门编号，从1开始 */
    private Integer valveNo;

    /** 阀门类型编码 */
    private String valveType;

    /** 开阀目标位置（度） */
    private Integer openPosition;

    /** 关阀目标位置（度） */
    private Integer closePosition;

    /** 开阀确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date openTime;

    /** 关阀确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closeTime;

    /** 持续秒数（已关阀） */
    private Integer durationSeconds;

    /** 当前持续秒数（仍开启中时由服务端计算） */
    private Integer currentDurationSeconds;

    /** 状态：OPEN/CLOSED/ABORTED */
    private String status;

    /** 关联开阀 control_log.id */
    private Long openControlLogId;

    /** 关联关阀 control_log.id */
    private Long closeControlLogId;

    /** 开阀操作人 */
    private String openBy;

    /** 关阀操作人 */
    private String closeBy;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
