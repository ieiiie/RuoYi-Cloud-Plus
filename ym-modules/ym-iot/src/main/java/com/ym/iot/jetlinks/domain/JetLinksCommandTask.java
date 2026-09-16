package com.ym.iot.jetlinks.domain;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

import java.util.Date;

/** 持久化业务记录；字段严格对应现有表，主键来自业务事实，不自动重建记录。 */
@Data
@TableName("iot_jetlinks_command_task")
public class JetLinksCommandTask {
    @TableId(type = IdType.INPUT)
    private String requestId;

    private String commandId;
    private Long deviceId;
    private String tenantId;
    private Long assignmentVersion;
    private String functionId;
    private String state;
    private String inputsJson;
    private String resultJson;
    private String errorMessage;
    private Integer priority;
    private Date createdAt;
    private Date updatedAt;
}
