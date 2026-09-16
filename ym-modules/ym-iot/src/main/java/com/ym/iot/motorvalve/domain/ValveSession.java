package com.ym.iot.motorvalve.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.util.Date;

/**
 * 电动阀开阀会话实体。
 */
@Data
@TableName("iot_motorvalve_valve_session")
public class ValveSession {

    /** 主键（雪花） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户编号 */
    private String tenantId;

    /** 设备ID */
    private Long deviceId;

    /** 设备编号（冗余） */
    private String deviceCode;

    /** 阀门编号，从1开始 */
    private Integer valveNo;

    /** 阀门类型编码，见 ValveType */
    private String valveType;

    /** 开阀目标位置（度） */
    private Integer openPosition;

    /** 关阀目标位置（度） */
    private Integer closePosition;

    /** 开阀确认时间（应答成功时刻） */
    private Date openTime;

    /** 关阀确认时间；NULL 表示仍开启中 */
    private Date closeTime;

    /** 持续秒数，关阀后回填 */
    private Integer durationSeconds;

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

    /** 备注（如重复开阀自动补关） */
    private String remark;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
