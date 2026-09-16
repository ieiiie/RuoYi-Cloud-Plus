package com.ym.iot.alarm.dubbo;

import com.ym.common.core.exception.ServiceException;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.ownership.mapper.DeviceOwnershipMapper;
import com.ym.jetlinks.rpc.*;
import com.ym.resource.api.RemoteInboxService;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Receives native notification decisions; performs no alarm evaluation or channel scheduling. */
@Service
@ConditionalOnJetLinks
@DubboService(group = "jetlinks-iot-business", version = "2.0.0", retries = 0)
public class NativeAlarmInboxReceiver implements IotNotificationInboxRpcService {
    private final DeviceOwnershipMapper ownershipMapper;

    @DubboReference(version = "2.0.0", retries = 0, timeout = 10000, check = false)
    private RemoteInboxService inbox;

    public NativeAlarmInboxReceiver(DeviceOwnershipMapper ownershipMapper) {
        this.ownershipMapper = ownershipMapper;
    }

    @Override
    public CompletableFuture<Boolean> receive(RequestContext context, RecordDto notification) {
        try {
            if (context == null
                    || !"jetlinks-native-notifier".equals(context.caller())
                    || notification == null
                    || notification.id() == null
                    || !notification.id().matches("[a-f0-9]{64}")
                    || !notification.id().equals(context.requestId())
                    || notification.data() == null) throw new ServiceException("无效的JetLinks通知身份");
            Map<String, Object> data = notification.data();
            String scope = required(data, "scopeKey", 128);
            if (!scope.startsWith("business:")
                    || !scope.substring(9).matches("[A-Za-z0-9_-]{1,20}"))
                throw new ServiceException("通知业务范围无效");
            String tenant = scope.substring(9), device = required(data, "targetId", 64);
            if (!device.matches("[1-9][0-9]{0,18}")) throw new ServiceException("通知设备ID无效");
            long occurred = Long.parseLong(required(data, "alarmTime", 20));
            if (occurred < 0 || occurred > System.currentTimeMillis() + 300000)
                throw new ServiceException("通知发生时间无效");
            String phase = required(data, "phase", 16);
            if (!Set.of("trigger", "recovery").contains(phase))
                throw new ServiceException("通知阶段无效");
            Long owners =
                    ownershipMapper.countVisibleInterval(
                            device, tenant, new Timestamp(occurred), new Timestamp(occurred));
            if (owners == null || owners != 1) throw new ServiceException("通知不属于设备发生时的业务归属");
            if (!(data.get("recipientIds") instanceof Collection<?> values)
                    || values.isEmpty()
                    || values.size() > 100) throw new ServiceException("必须配置1至100个收件人");
            List<Long> recipients =
                    values.stream()
                            .map(v -> Long.valueOf(Objects.toString(v, "")))
                            .distinct()
                            .sorted()
                            .toList();
            if (recipients.stream().anyMatch(id -> id <= 0)) throw new ServiceException("收件人ID无效");
            Map<String, Object> metadata = new LinkedHashMap<>();
            for (String key : List.of("notifierId", "templateId", "alarmConfigId"))
                metadata.put(key, required(data, key, 128));
            metadata.put("targetId", device);
            metadata.put("alarmTime", occurred);
            metadata.put("phase", phase);
            return CompletableFuture.completedFuture(
                    inbox.receive(
                            notification.id(),
                            tenant,
                            recipients,
                            required(data, "title", 100),
                            required(data, "content", 16000),
                            metadata));
        } catch (Exception error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    private static String required(Map<String, Object> data, String key, int limit) {
        String value = Objects.toString(data.get(key), "");
        if (value.isBlank() || value.length() > limit) throw new ServiceException("通知字段无效: " + key);
        return value;
    }
}
