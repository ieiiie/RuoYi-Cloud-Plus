package com.ym.iot.motorvalve.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 电动阀控制日志查询条件。
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ValveControlLogBo {

    /** 设备ID */
    private Long deviceId;

    /** 设备编号（模糊查询） */
    private String deviceCode;

    /** 命令类型：CONTROL/READ_DATA/READ_STATUS/NTP_SYNC */
    private String commandType;

    /** 状态：SENT/ACK/SUCCESS/FAILED/REJECTED */
    private String status;

    /** 创建时间-起始（yyyy-MM-dd HH:mm:ss） */
    private String beginTime;

    /** 创建时间-结束（yyyy-MM-dd HH:mm:ss） */
    private String endTime;
}
