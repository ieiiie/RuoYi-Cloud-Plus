package com.ym.agriculture.farming.crop.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 物种生长阶段配置 JSON 工具。
 */
public final class GrowthStageConfigSupport {

    private GrowthStageConfigSupport() {
    }

    public static String normalizeConfigJson(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return null;
        }
        JSONObject input = JSON.parseObject(JSON.toJSONString(config));
        JSONArray rawStages = input.getJSONArray("stages");
        if (rawStages == null || rawStages.isEmpty()) {
            return null;
        }

        List<JSONObject> stages = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        int defaultCount = 0;
        for (int i = 0; i < rawStages.size(); i++) {
            JSONObject raw = rawStages.getJSONObject(i);
            if (raw == null) {
                throw new ServiceException("生长阶段配置存在空行");
            }
            String code = trimToNull(raw.getString("code"));
            String name = trimToNull(raw.getString("name"));
            if (StringUtils.isBlank(code)) {
                throw new ServiceException("生长阶段编码不能为空");
            }
            if (StringUtils.isBlank(name)) {
                throw new ServiceException("生长阶段名称不能为空");
            }
            if (!codes.add(code)) {
                throw new ServiceException("生长阶段编码重复：" + code);
            }
            int sortOrder = raw.getInteger("sortOrder") == null ? i + 1 : raw.getInteger("sortOrder");
            boolean isDefault = raw.getBooleanValue("isDefault");
            if (isDefault) {
                defaultCount++;
            }
            JSONObject stage = new JSONObject();
            stage.put("code", code);
            stage.put("name", name);
            stage.put("sortOrder", sortOrder);
            stage.put("isDefault", isDefault);
            stages.add(stage);
        }
        if (defaultCount != 1) {
            throw new ServiceException("生长阶段必须且只能设置一个默认项");
        }
        stages.sort(Comparator
            .comparingInt((JSONObject stage) -> stage.getIntValue("sortOrder"))
            .thenComparing(stage -> stage.getString("code")));

        JSONObject out = new JSONObject();
        out.put("version", 1);
        out.put("stages", stages);
        return JSON.toJSONString(out);
    }

    public static Object parseJson(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JSON.parse(json);
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, Object> findDefaultStage(String configJson) {
        JSONObject config = parseConfig(configJson);
        if (config == null) {
            return Map.of();
        }
        JSONArray stages = config.getJSONArray("stages");
        if (stages == null || stages.isEmpty()) {
            return Map.of();
        }
        for (int i = 0; i < stages.size(); i++) {
            JSONObject stage = stages.getJSONObject(i);
            if (stage != null && stage.getBooleanValue("isDefault")) {
                return stagePayload(stage);
            }
        }
        return Map.of();
    }

    public static List<Map<String, Object>> listStages(String configJson) {
        JSONObject config = parseConfig(configJson);
        if (config == null) {
            return List.of();
        }
        JSONArray stages = config.getJSONArray("stages");
        if (stages == null || stages.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < stages.size(); i++) {
            JSONObject stage = stages.getJSONObject(i);
            if (stage != null) {
                out.add(stagePayload(stage));
            }
        }
        return out;
    }

    /**
     * 严格读取已经持久化的生长阶段配置。
     *
     * <p>该方法用于生成翻译资源等不能接受“部分成功”的写链路。空配置仍表示没有阶段；
     * 非空配置若格式非法、包含空阶段或重复/空编码，则直接拒绝，避免业务数据已经更新但
     * 翻译资源只登记了一部分。</p>
     *
     * @param configJson 生长阶段配置
     * @return 阶段列表
     */
    public static List<Map<String, Object>> listStagesStrict(String configJson) {
        if (StringUtils.isBlank(configJson)) {
            return List.of();
        }
        final JSONObject config;
        try {
            config = JSON.parseObject(configJson);
        } catch (Exception e) {
            throw new ServiceException("生长阶段配置格式不正确");
        }
        if (config == null) {
            throw new ServiceException("生长阶段配置格式不正确");
        }
        final JSONArray stages;
        try {
            stages = config.getJSONArray("stages");
        } catch (Exception e) {
            throw new ServiceException("生长阶段配置格式不正确");
        }
        if (stages == null || stages.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>(stages.size());
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < stages.size(); i++) {
            JSONObject stage;
            try {
                stage = stages.getJSONObject(i);
            } catch (Exception e) {
                throw new ServiceException("生长阶段配置格式不正确");
            }
            if (stage == null) {
                throw new ServiceException("生长阶段配置存在空行");
            }
            String code = trimToNull(stage.getString("code"));
            if (StringUtils.isBlank(code)) {
                throw new ServiceException("生长阶段编码不能为空");
            }
            if (!codes.add(code)) {
                throw new ServiceException("生长阶段编码重复：" + code);
            }
            Map<String, Object> payload = stagePayload(stage);
            payload.put("code", code);
            out.add(payload);
        }
        return out;
    }

    public static Map<String, Object> findStageByCode(String configJson, String code) {
        if (StringUtils.isBlank(code)) {
            return Map.of();
        }
        JSONObject config = parseConfig(configJson);
        if (config == null) {
            return Map.of();
        }
        JSONArray stages = config.getJSONArray("stages");
        if (stages == null || stages.isEmpty()) {
            return Map.of();
        }
        for (int i = 0; i < stages.size(); i++) {
            JSONObject stage = stages.getJSONObject(i);
            if (stage != null && code.equals(stage.getString("code"))) {
                return stagePayload(stage);
            }
        }
        return Map.of();
    }

    private static JSONObject parseConfig(String configJson) {
        if (StringUtils.isBlank(configJson)) {
            return null;
        }
        try {
            return JSON.parseObject(configJson);
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Object> stagePayload(JSONObject stage) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("code", stage.getString("code"));
        out.put("name", stage.getString("name"));
        out.put("sortOrder", stage.getIntValue("sortOrder"));
        out.put("isDefault", stage.getBooleanValue("isDefault"));
        return out;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
