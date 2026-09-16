package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.await;

import com.alibaba.fastjson2.JSON;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.mapper.JetLinksEventMapper;
import com.ym.iot.jetlinks.service.IJetLinksBusinessEvents;
import com.ym.jetlinks.rpc.*;

import org.springframework.stereotype.Service;

import java.util.*;

/** Durable lease/commit/ack boundary. Raw MQTT handlers are never invoked by this consumer. */
@Service
@ConditionalOnJetLinks
public class JetLinksBusinessEventsImpl implements IJetLinksBusinessEvents {
    private final JetLinksRpcClient rpc;
    private final JetLinksEventMapper eventMapper;
    private final JetLinksEventProjectionService projection;

    public JetLinksBusinessEventsImpl(
            JetLinksRpcClient rpc,
            JetLinksEventMapper eventMapper,
            JetLinksEventProjectionService projection) {
        this.rpc = rpc;
        this.eventMapper = eventMapper;
        this.projection = projection;
    }

    /** 仅在数据库提交成功后返回；重复投递由事件唯一键保证幂等。 */
    public void receive(String consumerId, ChangeEventDto event) {
        try {
            projection.apply(event);
            eventMapper.resolveFailure(event.eventId());
        } catch (RuntimeException failure) {
            recordFailure(consumerId, event, failure);
            throw failure;
        }
    }

    // 保留手动回放/兼容入口，生产投递由 JetLinks 主动推送。
    public void apply(ChangeEventDto event) {
        projection.apply(event);
    }

    public int consume(String consumerId, int limit) {
        if (consumerId == null || consumerId.isBlank())
            throw new ServiceException("durable consumerId required");
        List<ChangeEventDto> events =
                await(rpc.getChange().pending(consumerId, Math.min(500, Math.max(1, limit)), 120));
        if (events == null) throw new ServiceException("JetLinks未返回事件列表");
        int handled = 0;
        RuntimeException failed = null;
        for (ChangeEventDto event : events) {
            try {
                projection.apply(event);
                Integer ack =
                        await(rpc.getChange().acknowledge(consumerId, List.of(event.eventId())));
                if (ack == null || ack != 1)
                    throw new ServiceException(
                            "JetLinks event committed but ack pending; lease redelivery is safe");
                eventMapper.resolveFailure(event.eventId());
                handled++;
            } catch (RuntimeException ex) {
                try {
                    recordFailure(consumerId, event, ex);
                } catch (RuntimeException storage) {
                    ex.addSuppressed(storage);
                }
                if (failed == null) failed = ex;
                else failed.addSuppressed(ex);
            }
        }
        if (failed != null) throw failed;
        return handled;
    }

    private void recordFailure(String consumer, ChangeEventDto event, RuntimeException failure) {
        String error = Objects.toString(failure.getMessage(), failure.getClass().getSimpleName());
        if (error.length() > 4000) error = error.substring(0, 4000);
        eventMapper.recordFailure(
                event.eventId(), consumer, event.type(), JSON.toJSONString(event), error);
    }

    /**
     * Reapply the original immutable event. Provider lease redelivery performs any outstanding ack.
     */
    public void replay(String eventId, String operator) {
        if (eventId == null || eventId.isBlank() || operator == null || operator.isBlank())
            throw new ServiceException("event/operator required");
        var rows = eventMapper.selectFailure(eventId);
        if (rows.size() != 1) throw new ServiceException("失败事件不存在");
        var row = rows.getFirst();
        ChangeEventDto event =
                JSON.parseObject(row.get("payload_json").toString(), ChangeEventDto.class);
        try {
            projection.replay(event, operator);
        } catch (RuntimeException failure) {
            recordFailure(row.get("consumer_id").toString(), event, failure);
            throw failure;
        }
    }

    public List<Map<String, Object>> failures(int limit) {
        return new ArrayList<>(
                eventMapper.selectUnresolvedFailures(Math.max(1, Math.min(500, limit))));
    }
}
