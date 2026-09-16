package com.ym.agriculture.farming.bigscreen.support;

import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.bigscreen.config.BigscreenCaliber;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenRsAnalysisItemVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenRsAnalysisVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenRsLevelVo;
import com.ym.agriculture.farming.field.support.SfFieldMasterDictAccessor;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskResultMapper;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTaskResult;
import com.ym.agriculture.farming.satellite.service.impl.SatelliteTaskTypeDict;
import com.ym.system.api.domain.vo.RemoteDictDataVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 遥感分析结果组装（移植 mobile SatelliteAreaMetricsParser 逻辑，不依赖 mobile 模块）。
 */
@Component
@RequiredArgsConstructor
public class BigscreenSatelliteAssembler {

    private static final int LEVEL_SIZE = 5;

    private final SfSatelliteTaskResultMapper satelliteTaskResultMapper;
    private final SfFieldMasterDictAccessor dictAccessor;
    private final SatelliteTaskTypeDict taskTypeDict;

    /**
     * 按种植批次组装遥感轮播卡片。
     */
    public SfBigscreenRsAnalysisVo assemble(Long fieldId, String fieldName, Long plantingBatchId,
                                            BigDecimal fieldAreaMu) {
        SfBigscreenRsAnalysisVo vo = new SfBigscreenRsAnalysisVo();
        vo.setFieldId(fieldId);
        vo.setFieldName(fieldName);
        vo.setPlantingBatchId(plantingBatchId);
        if (plantingBatchId == null) {
            return vo;
        }
        List<SfSatelliteTaskResult> recent =
            satelliteTaskResultMapper.selectRecentByPlantingBatchId(plantingBatchId, 200);
        Map<String, SfSatelliteTaskResult> latestByType = pickLatestByTaskType(recent);
        for (String taskType : BigscreenCaliber.RS_TASK_TYPES) {
            SfSatelliteTaskResult result = latestByType.get(taskType);
            if (result == null || !Boolean.TRUE.equals(result.getSuccess())) {
                continue;
            }
            if ("rgb".equals(taskType)) {
                if (StringUtils.isBlank(result.getOssUrl())) {
                    continue;
                }
            } else if (StringUtils.isBlank(result.getArea())) {
                continue;
            }
            vo.getItems().add(buildItem(result, fieldAreaMu));
        }
        return vo;
    }

    private Map<String, SfSatelliteTaskResult> pickLatestByTaskType(List<SfSatelliteTaskResult> recent) {
        Map<String, SfSatelliteTaskResult> map = new HashMap<>();
        if (recent == null) {
            return map;
        }
        for (SfSatelliteTaskResult row : recent) {
            String type = StringUtils.trimToEmpty(row.getTaskType());
            if (type.isEmpty() || map.containsKey(type)) {
                continue;
            }
            map.put(type, row);
        }
        return map;
    }

    private SfBigscreenRsAnalysisItemVo buildItem(SfSatelliteTaskResult result, BigDecimal fieldAreaMu) {
        SfBigscreenRsAnalysisItemVo item = new SfBigscreenRsAnalysisItemVo();
        String taskType = StringUtils.trimToEmpty(result.getTaskType());
        item.setTaskType(taskType);
        item.setTaskTypeDesc(taskTypeDict.describeTaskType(taskType));
        item.setImageDate(result.getImageDate());
        item.setImageUrl(result.getOssUrl());
        List<BigDecimal> baseAreas = parseAreaValues(result.getArea());
        List<String> levelNames = loadLevelNames(taskType);
        List<BigDecimal> scaled = scaleAreas(baseAreas, fieldAreaMu);
        List<SfBigscreenRsLevelVo> levels = new ArrayList<>(LEVEL_SIZE);
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < LEVEL_SIZE; i++) {
            SfBigscreenRsLevelVo level = new SfBigscreenRsLevelVo();
            level.setLevelIndex(i + 1);
            level.setLevelName(levelNames.get(i));
            BigDecimal area = i < scaled.size() ? scaled.get(i) : null;
            level.setArea(area);
            level.setHasData(area != null);
            levels.add(level);
            if (area != null) {
                total = total.add(area);
            }
        }
        item.setLevels(levels);
        item.setTotalArea(total.compareTo(BigDecimal.ZERO) > 0 ? total : fieldAreaMu);
        return item;
    }

    private List<BigDecimal> scaleAreas(List<BigDecimal> baseAreas, BigDecimal fieldAreaMu) {
        if (baseAreas == null || baseAreas.isEmpty()) {
            return List.of();
        }
        BigDecimal sum = baseAreas.stream().filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.ZERO) <= 0 || fieldAreaMu == null
            || fieldAreaMu.compareTo(BigDecimal.ZERO) <= 0) {
            return baseAreas;
        }
        List<BigDecimal> scaled = new ArrayList<>(baseAreas.size());
        for (BigDecimal base : baseAreas) {
            if (base == null) {
                scaled.add(null);
                continue;
            }
            scaled.add(base.multiply(fieldAreaMu).divide(sum, 1, RoundingMode.HALF_UP));
        }
        return scaled;
    }

    private List<String> loadLevelNames(String taskType) {
        List<String> names = new ArrayList<>(LEVEL_SIZE);
        if (StringUtils.isNotBlank(taskType)) {
            try {
                List<RemoteDictDataVo> dictData = dictAccessor.getDictDataList(taskType);
                if (dictData != null && !dictData.isEmpty()) {
                    dictData.stream()
                        .sorted(Comparator.comparing(RemoteDictDataVo::getDictSort,
                            Comparator.nullsLast(Integer::compareTo)))
                        .map(RemoteDictDataVo::getDictLabel)
                        .filter(StringUtils::isNotBlank)
                        .limit(LEVEL_SIZE)
                        .forEach(names::add);
                }
            } catch (Exception ignored) {
                // 字典不可用时使用默认等级名
            }
        }
        for (int i = names.size(); i < LEVEL_SIZE; i++) {
            names.add("等级" + (i + 1));
        }
        return names;
    }

    private List<BigDecimal> parseAreaValues(String areaRaw) {
        List<BigDecimal> values = new ArrayList<>(LEVEL_SIZE);
        if (StringUtils.isBlank(areaRaw)) {
            return values;
        }
        String trimmed = areaRaw.trim();
        try {
            if (trimmed.startsWith("[")) {
                JSONArray arr = JSON.parseArray(trimmed);
                if (arr == null || arr.isEmpty()) {
                    return values;
                }
                int size = Math.min(arr.size(), LEVEL_SIZE);
                for (int i = 0; i < size; i++) {
                    values.add(toDecimal(arr.get(i)));
                }
            }
        } catch (Exception ignored) {
            return values;
        }
        return values;
    }

    private BigDecimal toDecimal(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return NumberUtil.toBigDecimal(String.valueOf(raw));
        } catch (Exception ignored) {
            return null;
        }
    }
}
