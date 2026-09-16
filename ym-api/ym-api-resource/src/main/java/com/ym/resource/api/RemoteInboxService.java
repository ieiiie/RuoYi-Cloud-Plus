package com.ym.resource.api;

import java.util.List;
import java.util.Map;

/** Durable internal inbox delivery. A repeated ID with different content is rejected. */
public interface RemoteInboxService {
    boolean receive(String deliveryId, String tenantId, List<Long> recipientIds,
                    String title, String content, Map<String,Object> metadata);
}
