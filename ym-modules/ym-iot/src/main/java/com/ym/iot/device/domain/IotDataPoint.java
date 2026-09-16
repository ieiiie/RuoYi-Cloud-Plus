package com.ym.iot.device.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 设备遥测数据点。
 */
@Data
@TableName("iot_data_point")
public class IotDataPoint implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("point_id")
    private Long pointId;
    private String tenantId;
    private Long deviceId;
    private String metricCode;
    private String propertyIdentifier;
    private BigDecimal metricValue;
    private String valueText;
    /** Transport-only typed value; never a legacy MySQL column. */
    @com.baomidou.mybatisplus.annotation.TableField(exist=false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private transient Object typedValue;
    private String metricUnit;
    private Date collectTime;
    private Date receivedTime;
    private String rawPayload;
}
