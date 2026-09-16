package com.ym.iot.alarm.service;

import com.ym.common.core.domain.PageResult;
import com.ym.jetlinks.rpc.RecordDto;

import java.util.List;
import java.util.Map;

/** 原生告警业务入口，保留租户范围、版本冲突与幂等校验。 */
public interface INativeAlarmService {
    PageResult<Map<String, Object>> configurations(Map<String, String> parameters);

    Map<String, Object> configuration(String id);

    Map<String, Object> save(String pathId, RecordDto input, String requestId);

    Map<String, Object> settings(String id);

    Map<String, Object> updateSettings(String id, Map<String, Object> input, String requestId);

    boolean delete(String id, long version, String requestId);

    Map<String, Object> enabled(String id, boolean enabled, long version, String requestId);

    PageResult<Map<String, Object>> records(Map<String, String> parameters);

    Map<String, Object> record(String id);

    PageResult<Map<String, Object>> history(Map<String, String> parameters);

    PageResult<Map<String, Object>> handlingHistory(Map<String, String> parameters);

    Map<String, Object> handle(String id, Map<String, Object> input, String requestId);

    List<Map<String, Object>> notificationOptions();

    long warningCount();
}
