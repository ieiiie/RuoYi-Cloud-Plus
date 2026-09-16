package com.ym.iot.motorvalve.domain.bo;

import lombok.Data;

/**
 * 电动阀开阀会话查询条件。
 */
@Data
public class ValveSessionBo {

    /** 设备ID */
    private Long deviceId;

    /** 设备编号（模糊查询） */
    private String deviceCode;

    /** 阀门编号，从1开始 */
    private Integer valveNo;

    /** 状态：OPEN/CLOSED/ABORTED */
    private String status;

    /** 开阀时间-起始（yyyy-MM-dd HH:mm:ss） */
    private String beginTime;

    /** 开阀时间-结束（yyyy-MM-dd HH:mm:ss） */
    private String endTime;
}
