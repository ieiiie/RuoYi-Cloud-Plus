package com.ym.agriculture.farmtask.inspection.support;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ym.agriculture.shared.i18n.StaskErrorCodes;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** 抽检照片 JSON 解析、数量和当前租户 OSS 归属校验。 */
@Component
@RequiredArgsConstructor
public class StaskInspectionPhotoReferenceNormalizer {

    private static final int MAX_PHOTO_COUNT = 6;

    private final StaskInspectionMasterOssAccessor ossAccessor;
    private final StaskMessageResolver messages;

    /**
     * 校验并保存为顺序稳定的 OSS ID 数组，避免把短期签名 URL 写入业务表。
     */
    public String normalize(String tenantId, String source, String fieldName) {
        JSONArray array = parseArray(source, fieldName);
        if (array.isEmpty()) {
            return "[]";
        }
        if (array.size() > MAX_PHOTO_COUNT) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PHOTO_LIMIT_EXCEEDED, 400,
                StaskMessageKeys.ERROR_INSPECTION_PHOTO_LIMIT_EXCEEDED, fieldName);
        }
        List<Long> ids = new ArrayList<>(array.size());
        for (Object item : array) {
            ids.add(resolveOssId(tenantId, item, fieldName));
        }
        Collection<Long> distinctIds = new LinkedHashSet<>(ids);
        Map<Long, RemoteFile> existing = new LinkedHashMap<>();
        for (RemoteFile row : ossAccessor.listByIds(tenantId, distinctIds)) {
            if (row != null && row.getOssId() != null) {
                existing.put(row.getOssId(), row);
            }
        }
        if (existing.size() != distinctIds.size()) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_PHOTO_INVALID, fieldName);
        }
        return JSON.toJSONString(ids.stream().map(String::valueOf).toList());
    }

    /** 读取已规范化的照片数量；兼容旧记录中的数组结构。 */
    public int count(String source) {
        try {
            return parseArray(source, null).size();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    /** 将持久化的 OSS ID 数组转换成前端可展示的 JSON 对象数组。 */
    public String render(String tenantId, String source) {
        List<Long> ids = parseIds(source);
        if (ids.isEmpty()) {
            return "[]";
        }
        Map<Long, RemoteFile> byId = new LinkedHashMap<>();
        for (RemoteFile row : ossAccessor.listByIds(tenantId, new LinkedHashSet<>(ids))) {
            if (row != null && row.getOssId() != null) {
                byId.put(row.getOssId(), row);
            }
        }
        JSONArray result = new JSONArray();
        for (Long id : ids) {
            RemoteFile row = byId.get(id);
            JSONObject item = new JSONObject();
            item.put("ossId", String.valueOf(id));
            if (row != null) {
                item.put("url", row.getUrl());
                item.put("originalName", row.getOriginalName());
            }
            result.add(item);
        }
        return result.toJSONString();
    }

    private JSONArray parseArray(String source, String fieldName) {
        if (source == null || source.isBlank()) {
            return new JSONArray();
        }
        Object parsed;
        try {
            parsed = JSON.parse(source);
        } catch (RuntimeException exception) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_PHOTO_INVALID, fieldName);
        }
        if (!(parsed instanceof JSONArray array)) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_PHOTO_INVALID, fieldName);
        }
        return array;
    }

    private Long resolveOssId(String tenantId, Object item, String fieldName) {
        Long id = null;
        String url = null;
        if (item instanceof JSONObject object) {
            id = firstLong(object, "ossId", "id", "fileId", "photoId");
            url = firstString(object, "url", "fileUrl");
        } else if (item != null) {
            String text = String.valueOf(item).trim();
            id = positiveLong(text);
            url = id == null ? text : null;
        }
        if (id != null && id > 0) {
            return id;
        }
        if (url != null && !url.isBlank()) {
            RemoteFile row = ossAccessor.findByUrl(tenantId, url);
            if (row != null && row.getOssId() != null) {
                return row.getOssId();
            }
        }
        throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
            StaskMessageKeys.ERROR_INSPECTION_PHOTO_INVALID, fieldName);
    }

    private Long firstLong(JSONObject object, String... names) {
        for (String name : names) {
            Long parsed = positiveLong(object.get(name));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private String firstString(JSONObject object, String... names) {
        for (String name : names) {
            Object value = object.get(name);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private List<Long> parseIds(String source) {
        JSONArray array = parseArray(source, null);
        List<Long> ids = new ArrayList<>(array.size());
        for (Object item : array) {
            if (item instanceof JSONObject object) {
                Long id = firstLong(object, "ossId", "id", "fileId", "photoId");
                if (id != null) {
                    ids.add(id);
                }
            } else {
                Long id = positiveLong(item);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private Long positiveLong(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || text.chars().anyMatch(character -> character < '0' || character > '9')) {
            return null;
        }
        try {
            long parsed = Long.parseLong(text);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
