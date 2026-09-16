package com.ym.agriculture.farming.satellite.service.impl;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.field.support.SfFieldMasterDictAccessor;
import com.ym.system.api.domain.vo.RemoteDictDataVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 遥感任务类型编码与中文描述映射工具。
 * 优先从系统字典 {@code sat_task_type} 加载，字典不可用时回退到硬编码。
 */
@Slf4j
@Component
public class SatelliteTaskTypeDict {

    private static final String DICT_TYPE = "sat_task_type";
    private static final Map<String, String> DEFAULT_TYPE_LABELS = defaultTypeLabels();

    private final SfFieldMasterDictAccessor dictAccessor;
    private volatile Map<String, String> cachedTypeMap;

    SatelliteTaskTypeDict(SfFieldMasterDictAccessor dictAccessor) {
        this.dictAccessor = dictAccessor;
    }

    private Map<String, String> getTypeMap() {
        if (cachedTypeMap != null) {
            return cachedTypeMap;
        }
        try {
            Map<String, String> dictMap = dictAccessor.getDictLabelMap(DICT_TYPE);
            if (dictMap != null && !dictMap.isEmpty()) {
                cachedTypeMap = new ConcurrentHashMap<>(dictMap);
                return cachedTypeMap;
            }
        } catch (Exception e) {
            log.warn("加载字典 {} 失败", DICT_TYPE, e);
        }
        cachedTypeMap = Map.of();
        return cachedTypeMap;
    }

    /**
     * 清除缓存，下次调用时重新从字典加载。
     */
    void refreshCache() {
        cachedTypeMap = null;
    }

    public Map<String, String> listTaskTypes() {
        try {
            List<RemoteDictDataVo> dictData = dictAccessor.getDictDataList(DICT_TYPE);
            if (dictData != null && !dictData.isEmpty()) {
                LinkedHashMap<String, String> result = new LinkedHashMap<>();
                for (RemoteDictDataVo item : dictData) {
                    if (StringUtils.isBlank(item.getDictValue())) {
                        continue;
                    }
                    result.put(item.getDictValue().trim(), item.getDictLabel());
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("加载字典 {} 列表失败", DICT_TYPE, e);
        }
        return DEFAULT_TYPE_LABELS;
    }

    public String describeTaskType(String taskType) {
        if (StringUtils.isBlank(taskType)) {
            return "";
        }
        String normalizedType = taskType.trim();
        return getTypeMap().getOrDefault(normalizedType,
            DEFAULT_TYPE_LABELS.getOrDefault(normalizedType, normalizedType));
    }

    public  String describeMultiTaskTypes(String taskType) {
        if (StringUtils.isBlank(taskType)) {
            return "";
        }
        List<String> types = splitTaskTypes(taskType);
        if (types.size() == 1) {
            return describeTaskType(types.get(0));
        }
        return describeTaskType(types.get(0)) + "等" + types.size() + "项";
    }

    public  static List<String> splitTaskTypes(String taskType) {
        if (StringUtils.isBlank(taskType)) {
            return List.of();
        }
        return Arrays.stream(taskType.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    private static Map<String, String> defaultTypeLabels() {
        LinkedHashMap<String, String> defaults = new LinkedHashMap<>();
        defaults.put("growth", "长势监测");
        defaults.put("chlorophyll", "叶绿素");
        defaults.put("nitrogen", "氮含量差异");
        defaults.put("droughtlevel", "干旱程度");
        defaults.put("soilmoisture", "土壤墒情");
        defaults.put("health", "健康度");
        defaults.put("seedlinggrowth", "苗情");
        defaults.put("rgb", "真彩色");
        defaults.put("cloudcover", "云量");
        defaults.put("bollopening", "吐絮率");
        return defaults;
    }
}
