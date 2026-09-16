package com.ym.iot.jetlinks.domain.dto;

import java.util.Date;

/** 命令任务的只读快照；未收到应答时命令号和结果可以为空。 */
public record JetLinksTaskSnapshot(
        String requestId,
        String commandId,
        Long deviceId,
        String tenantId,
        Long assignmentVersion,
        String functionId,
        String state,
        String resultJson,
        Date createdAt,
        Date updatedAt) {}
