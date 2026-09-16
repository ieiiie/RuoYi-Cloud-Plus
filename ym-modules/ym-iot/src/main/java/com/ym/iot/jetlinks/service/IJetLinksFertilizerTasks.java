package com.ym.iot.jetlinks.service;

import com.ym.iot.fertilizer.domain.vo.TaskInfo;

import java.util.List;
import java.util.Map;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksFertilizerTasks {
    TaskInfo enqueue(Long id, String label, List<Map<String, Object>> plan);

    TaskInfo enqueue(Long id, String label, List<Map<String, Object>> plan, String reason);

    TaskInfo getTask(String id);

    TaskInfo active(Long id);
}
