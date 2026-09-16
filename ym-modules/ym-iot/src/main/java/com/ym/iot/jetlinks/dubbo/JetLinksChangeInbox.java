package com.ym.iot.jetlinks.dubbo;

import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksBusinessEvents;
import com.ym.jetlinks.rpc.ChangeEventDto;
import com.ym.jetlinks.rpc.IotChangeInboxRpcService;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.CompletableFuture;

/** 仅供内部服务调用，先提交数据库回执及投影，再确认投递。 */
@ConditionalOnJetLinks
@DubboService(group = "jetlinks-iot-business", version = "1.0.0", retries = 0, token = "true")
public class JetLinksChangeInbox implements IotChangeInboxRpcService {
    private final IJetLinksBusinessEvents events;
    private final String consumer;

    public JetLinksChangeInbox(
            IJetLinksBusinessEvents events,
            @Value("${ym.iot.jetlinks.consumer-id:ym-iot-business-v1}") String consumer) {
        this.events = events;
        this.consumer = consumer;
    }

    @Override
    public CompletableFuture<Boolean> receive(String consumerId, ChangeEventDto event) {
        if (!consumer.equals(consumerId)
                || event == null
                || event.eventId() == null
                || event.type() == null
                || event.data() == null) {
            return CompletableFuture.completedFuture(false);
        }
        try {
            events.receive(consumerId, event);
            return CompletableFuture.completedFuture(true);
        } catch (RuntimeException failure) {
            // Do not serialize JDBC/driver exceptions across Dubbo's strict class allowlist.
            return CompletableFuture.completedFuture(false);
        }
    }
}
