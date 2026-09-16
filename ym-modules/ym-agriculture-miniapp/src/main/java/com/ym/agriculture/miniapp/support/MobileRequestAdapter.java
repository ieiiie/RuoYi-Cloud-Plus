package com.ym.agriculture.miniapp.support;

import com.ym.agriculture.api.farming.domain.bo.RemoteAgricultureMobileCommandBo;
import com.ym.agriculture.api.farming.domain.bo.RemoteAgricultureMobileQueryBo;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** BFF 通用参数与幂等号适配。 */
public final class MobileRequestAdapter {

    private MobileRequestAdapter() {
    }

    public static RemoteAgricultureMobileQueryBo query(MultiValueMap<String, String> parameters) {
        RemoteAgricultureMobileQueryBo query = new RemoteAgricultureMobileQueryBo();
        query.setPageNum(parsePositive(first(parameters, "pageNum"), 1));
        query.setPageSize(parsePositive(first(parameters, "pageSize"), 10));
        LinkedHashMap<String, Object> filters = new LinkedHashMap<>();
        if (parameters != null) {
            parameters.forEach((key, values) -> {
                if (!"pageNum".equals(key) && !"pageSize".equals(key) && values != null && !values.isEmpty()) {
                    filters.put(key, values.size() == 1 ? values.get(0) : List.copyOf(values));
                }
            });
        }
        query.setFilters(filters);
        return query;
    }

    public static RemoteAgricultureMobileCommandBo command(String headerRequestId, String action,
                                                             Long businessKey, Map<String, Object> body) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>(body == null ? Map.of() : body);
        String requestId = firstText(headerRequestId, payload.get("requestId"), payload.get("idempotencyKey"));
        if (requestId == null) {
            throw new IllegalArgumentException("写操作必须提供 X-Request-Id 或 idempotencyKey");
        }
        RemoteAgricultureMobileCommandBo command = new RemoteAgricultureMobileCommandBo();
        command.setRequestId(requestId);
        command.setBusinessId(action + ":" + (businessKey == null ? requestId : businessKey));
        command.setPayload(payload);
        return command;
    }

    private static String first(MultiValueMap<String, String> parameters, String key) {
        return parameters == null ? null : parameters.getFirst(key);
    }

    private static int parsePositive(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        int parsed = Integer.parseInt(value);
        return parsed > 0 ? parsed : fallback;
    }

    private static String firstText(String header, Object... candidates) {
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        for (Object candidate : candidates) {
            if (candidate != null && !candidate.toString().isBlank()) {
                return candidate.toString().trim();
            }
        }
        return null;
    }
}
