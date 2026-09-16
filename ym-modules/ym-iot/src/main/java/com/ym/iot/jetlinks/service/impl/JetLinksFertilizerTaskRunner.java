package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.await;

import com.alibaba.fastjson2.*;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.mapper.JetLinksCommandTaskMapper;
import com.ym.iot.ownership.service.IDeviceAccessService;
import com.ym.jetlinks.rpc.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 提交任务或确认回执后推进一步；UNKNOWN 任务始终等待核实，不自动重发。 */
@Service
@ConditionalOnJetLinks
public class JetLinksFertilizerTaskRunner {
    private final IDeviceAccessService ownership;
    private final JetLinksRpcClient rpc;
    private final JetLinksCommandTaskMapper taskMapper;

    public JetLinksFertilizerTaskRunner(
            IDeviceAccessService ownership,
            JetLinksRpcClient rpc,
            JetLinksCommandTaskMapper taskMapper) {
        this.ownership = ownership;
        this.rpc = rpc;
        this.taskMapper = taskMapper;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            timeout = 30,
            rollbackFor = Exception.class)
    public void advance(String id, boolean reconcile) {
        var rows = taskMapper.lockTask(id);
        if (rows.isEmpty()) return;
        var row = rows.getFirst();
        if (!Set.of("QUEUED", "RUNNING").contains(row.get("state"))) return;
        Long device = ((Number) row.get("device_id")).longValue(),
                version = ((Number) row.get("assignment_version")).longValue();
        taskMapper.lockDevice(device);
        try {
            ownership.requireCurrentVersion(device, version);
        } catch (RuntimeException ex) {
            fail(id, "FAILED", ex.getMessage());
            return;
        }
        JSONObject input = JSON.parseObject(row.get("inputs_json").toString());
        JSONArray plan = input.getJSONArray("plan");
        JSONObject progress =
                row.get("result_json") == null
                        ? new JSONObject()
                        : JSON.parseObject(row.get("result_json").toString());
        int index = progress.getIntValue("stepIndex");
        String commandId = (String) row.get("command_id");
        try {
            if (commandId != null) {
                if (!reconcile) return;
                CommandDto command = await(rpc.getCommand().command(commandId));
                if (command == null) {
                    fail(id, "UNKNOWN", "provider未返回命令状态");
                    return;
                }
                if (!device.toString().equals(command.deviceId()))
                    throw new ServiceException("命令设备不匹配");
                if (Set.of("FAILED", "TIMED_OUT").contains(command.state())) {
                    fail(id, command.state(), JSON.toJSONString(command.result()));
                    return;
                }
                if (!"SUCCEEDED".equals(command.state())) return;
                index++;
                progress.put("stepIndex", index);
                progress.put("lastResult", command.result());
                taskMapper.completeStep(
                        progress.toJSONString(),
                        index >= plan.size() ? "SUCCEEDED" : "RUNNING",
                        id);
            }
            if (index >= plan.size()) {
                fail(id, "SUCCEEDED", null);
                return;
            }
            JSONObject step = plan.getJSONObject(index);
            RequestContext context =
                    rpc.deviceContext(id + ":" + index, version, input.getString("operatorId"));
            CommandDto command =
                    await(
                            rpc.getCommand()
                                    .submit(
                                            context,
                                            device.toString(),
                                            step.getString("functionId"),
                                            step.getJSONObject("inputs")));
            if (command == null || !device.toString().equals(command.deviceId()))
                throw new ServiceException("provider未返回有效命令");
            taskMapper.markStepRunning(command.commandId(), id);
        } catch (RuntimeException ex) {
            fail(id, "UNKNOWN", ex.getMessage());
        }
    }

    private void fail(String id, String state, String error) {
        taskMapper.finishTask(state, error, id);
    }
}
