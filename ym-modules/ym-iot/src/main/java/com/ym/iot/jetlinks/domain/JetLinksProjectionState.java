package com.ym.iot.jetlinks.domain;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 持久化业务记录；字段严格对应现有表，主键来自业务事实，不自动重建记录。 */
@Data
@TableName("iot_jetlinks_projection")
public class JetLinksProjectionState {
    @TableId(type = IdType.INPUT)
    private String projectionKey;

    private Long businessId;
    private Long sourceTime;
    private Long version;
    private String payloadJson;
}
