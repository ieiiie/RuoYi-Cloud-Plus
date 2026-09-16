package com.ym.iot.jetlinks.service.impl;

import com.alibaba.fastjson2.*;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.iot.fertilizer.domain.vo.TaskInfo;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.domain.dto.JetLinksTaskSnapshot;
import com.ym.iot.jetlinks.mapper.JetLinksCommandTaskMapper;
import com.ym.iot.jetlinks.service.IJetLinksFertilizerTasks;
import com.ym.iot.jetlinks.service.support.JetLinksBusinessStore;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.ownership.service.IDeviceAccessService;
import com.ym.jetlinks.rpc.*;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Each step is durable and waits for provider confirmation. No delayed old MQTT sends or automatic
 * retries.
 */
@Service
@ConditionalOnJetLinks
public class JetLinksFertilizerTasksImpl implements IJetLinksFertilizerTasks {
    private final JetLinksAccess access;
    private final IDeviceAccessService ownership;
    private final JetLinksRpcClient rpc;
    private final JetLinksBusinessStore store;
    private final JetLinksCommandTaskMapper taskMapper;
    private final org.springframework.context.ApplicationEventPublisher publisher;

    public JetLinksFertilizerTasksImpl(
            JetLinksAccess access,
            IDeviceAccessService ownership,
            JetLinksRpcClient rpc,
            JetLinksBusinessStore store,
            JetLinksCommandTaskMapper taskMapper,
            org.springframework.context.ApplicationEventPublisher publisher) {
        this.access = access;
        this.ownership = ownership;
        this.rpc = rpc;
        this.store = store;
        this.taskMapper = taskMapper;
        this.publisher = publisher;
    }

    public TaskInfo enqueue(Long id, String label, List<Map<String, Object>> plan) {
        return enqueue(id, label, plan, null);
    }

    public TaskInfo enqueue(Long id, String label, List<Map<String, Object>> plan, String reason) {
        if (plan == null || plan.isEmpty()) throw new ServiceException("任务计划不能为空");
        var owner = access.snapshot(id);
        String request = UUID.randomUUID().toString();
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("plan", plan);
        inputs.put("label", label);
        inputs.put("operatorId", LoginHelper.getUserIdStr());
        if (reason != null) inputs.put("reason", reason);
        boolean emergency =
                plan.stream()
                        .anyMatch(
                                step ->
                                        "EMERGENCY_STOP"
                                                .equals(
                                                        JetLinksBusinessProjection.map(
                                                                        step.get("inputs"))
                                                                .get("command")));
        access.write(
                id,
                () -> {
                    access.requireVersion(id, owner.getAssignmentVersion());
                    if (!emergency
                            && store.activeTask(
                                            id, owner.getTenantId(), owner.getAssignmentVersion())
                                    != null) throw new ServiceException("设备正有其他控制任务进行中");
                    // Same physical datasource and transaction as withAccess: task creation is
                    // visible before transfer can proceed.
                    taskMapper.createPriorityTask(
                            request,
                            id,
                            owner.getTenantId(),
                            owner.getAssignmentVersion(),
                            "fertilizer.task",
                            JSON.toJSONString(inputs),
                            emergency ? 10 : 0);
                    publisher.publishEvent(
                            new com.ym.iot.jetlinks.domain.dto.FertilizerTaskReady(request));
                    return true;
                });
        return getTask(request);
    }

    public TaskInfo getTask(String id) {
        var task = store.task(id);
        if (task == null) throw new ServiceException("任务不存在");
        if (!JetLinksHistoryServiceImpl.tenant().equals(task.tenantId()))
            throw new ServiceException("无权查询其他租户任务");
        return view(task);
    }

    public TaskInfo active(Long id) {
        var owner = access.snapshot(id);
        var task = store.activeTask(id, owner.getTenantId(), owner.getAssignmentVersion());
        return task == null ? null : view(task);
    }

    private TaskInfo view(JetLinksTaskSnapshot task) {
        TaskInfo vo = new TaskInfo();
        vo.setTaskId(task.requestId());
        vo.setDeviceId(task.deviceId());
        vo.setCreatedAt(task.createdAt());
        JSONObject input = JSON.parseObject(taskMapper.selectInputs(task.requestId()));
        vo.setLabel(input.getString("label"));
        String state = task.state();
        boolean done = Set.of("SUCCEEDED", "FAILED", "TIMED_OUT").contains(state);
        vo.setStatus(
                "SUCCEEDED".equals(state)
                        ? "SUCCEEDED"
                        : done ? "FAILED" : "QUEUED".equals(state) ? "QUEUED" : "RUNNING");
        JSONObject progress =
                task.resultJson() == null ? new JSONObject() : JSON.parseObject(task.resultJson());
        int completed = progress.getIntValue("stepIndex"),
                total = input.getJSONArray("plan").size();
        vo.setStep(
                "UNKNOWN".equals(state)
                        ? "命令结果未确认，请勿重复提交"
                        : done
                                ? "SUCCEEDED".equals(state) ? "执行完成" : "执行失败"
                                : "已确认 " + completed + "/" + total + " 步，等待设备确认");
        vo.setProgress(total == 0 ? 0 : Math.min(99, completed * 100 / total));
        if ("SUCCEEDED".equals(state)) vo.setResult("执行完成");
        if (done) {
            vo.setFinishedAt(task.updatedAt());
            vo.setProgress(100);
        }
        if ("FAILED".equals(vo.getStatus())) vo.setError(taskMapper.selectError(task.requestId()));
        return vo;
    }
}
