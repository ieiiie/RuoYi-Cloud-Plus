package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.*;

import com.alibaba.fastjson2.JSON;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksCommandService;
import com.ym.iot.jetlinks.service.support.JetLinksBusinessStore;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.jetlinks.rpc.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksCommandServiceImpl implements IJetLinksCommandService {
    private final JetLinksRpcClient rpc;
    private final JetLinksAccess access;
    private final JetLinksBusinessStore store;

    public CommandDto submit(Long id, String function, Map<String, Object> inputs) {
        var owner = access.snapshot(id);
        String requestId = UUID.randomUUID().toString();
        // Persist before initiating RPC. A lost response remains UNKNOWN and is never auto-retried.
        store.createTask(
                requestId, id, owner.getTenantId(), owner.getAssignmentVersion(), function, inputs);
        var dispatched = new java.util.concurrent.atomic.AtomicBoolean(false);
        try {
            CommandDto command =
                    access.write(
                            id,
                            () -> {
                                access.requireVersion(id, owner.getAssignmentVersion());
                                RequestContext context =
                                        rpc.deviceContext(requestId, owner.getAssignmentVersion());
                                dispatched.set(true);
                                return await(
                                        rpc.getCommand()
                                                .submit(context, id.toString(), function, inputs));
                            });
            if (command == null || !id.toString().equals(command.deviceId()))
                throw new ServiceException("JetLinks未返回有效设备命令");
            store.updateCommand(requestId, command);
            return command;
        } catch (RuntimeException ex) {
            if (dispatched.get()) store.markUnknown(requestId, ex.getMessage());
            else store.markFailed(requestId, ex.getMessage());
            throw ex;
        }
    }

    public CommandDto get(String commandId) {
        var task = store.task(commandId);
        if (task == null) throw new ServiceException("任务不存在");
        access.requireVersion(task.deviceId(), task.assignmentVersion());
        CommandDto command =
                await(
                        rpc.getCommand()
                                .command(
                                        task.commandId() == null
                                                ? task.requestId()
                                                : task.commandId()));
        if (command == null) throw new ServiceException("命令状态暂不可确认，请保留原taskId查询，不要重新提交");
        if (!task.deviceId().toString().equals(command.deviceId()))
            throw new ServiceException("命令设备不匹配");
        store.updateCommand(task.requestId(), command);
        return command;
    }

    public RecordDto capabilities(Long id) {
        access.require(id);
        return await(rpc.getCommand().capabilities(id.toString()));
    }

    public static Map<String, Object> result(CommandDto command) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (command.result() != null)
            result.putAll(JetLinksBusinessProjection.protocol(command.result()));
        return result;
    }

    /**
     * Preserve the old HTTP command text while the core worker materializes the one queued intent.
     */
    public CommandDto awaitPayload(CommandDto initial) {
        CommandDto current = initial;
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        while (!result(current).containsKey("payload")
                && !result(current).containsKey("commandText")) {
            if (Set.of("FAILED", "TIMED_OUT", "CANCELLED").contains(current.state()))
                throw new ServiceException(
                        "核心命令失败，commandId="
                                + current.commandId()
                                + ": "
                                + JSON.toJSONString(current.result()));
            if (System.nanoTime() >= deadline)
                throw new ServiceException(
                        "核心命令已排队，尚未返回指令原文；commandId=" + current.commandId() + "，请查询原命令，不要重复提交");
            try {
                Thread.sleep(150);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ServiceException("等待命令被中断；commandId=" + current.commandId());
            }
            current = get(initial.commandId());
        }
        return current;
    }

    public static String commandText(CommandDto command) {
        Map<String, Object> result = result(command);
        Object text = result.get("commandText");
        if (text == null) text = result.get("payload");
        if (text == null)
            throw new ServiceException(
                    "JetLinks命令已受理，provider缺少兼容commandText；commandId="
                            + command.commandId()
                            + "，请勿重发");
        return text.toString();
    }
}
