package com.ym.iot.ownership.domain;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 持久化业务记录；字段严格对应现有表，主键来自业务事实，不自动重建记录。 */
@Data
@TableName("iot_device_ownership")
public class IotDeviceOwnership {
    @TableId(type = IdType.INPUT)
    private Long deviceId;

    private String tenantId;
    private Long assignmentVersion;
    private java.time.Instant effectiveFrom;
    private String fenceStatus;
}
