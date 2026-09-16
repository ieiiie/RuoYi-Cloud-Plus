package com.ym.iot.jetlinks.domain;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

import java.util.Date;

/** 持久化业务记录；字段严格对应现有表，主键来自业务事实，不自动重建记录。 */
@Data
@TableName("iot_jetlinks_business_event")
public class JetLinksBusinessEvent {
    @TableId(type = IdType.INPUT)
    private String eventId;

    private String eventType;
    private String deviceId;
    private Long sourceTime;
    private String payloadJson;
    private Date createdAt;
}
