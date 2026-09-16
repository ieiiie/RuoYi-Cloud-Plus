package com.ym.iot.jetlinks.domain.dto;

/** 只携带持久任务 ID；执行器重新读取任务及归属，不信任异步线程中的租户上下文。 */
public record FertilizerTaskReady(String requestId) {}
