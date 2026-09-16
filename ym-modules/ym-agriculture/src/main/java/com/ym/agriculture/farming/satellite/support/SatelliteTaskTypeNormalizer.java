package com.ym.agriculture.farming.satellite.support;

import cn.hutool.core.util.StrUtil;

/** 遥感任务类型规范化，兼容历史上游编码差异。 */
public final class SatelliteTaskTypeNormalizer {

    public static final String SOIL_MOISTURE = "soilmoisture";
    public static final String LEGACY_SOIL_MOISTURE = "soilmoisturel";

    private SatelliteTaskTypeNormalizer() {
    }

    /** 将单个遥感任务类型转换为平台标准编码。 */
    public static String normalize(String taskType) {
        if (StrUtil.isBlank(taskType)) {
            return taskType;
        }
        String normalized = taskType.trim();
        if (LEGACY_SOIL_MOISTURE.equalsIgnoreCase(normalized)) {
            return SOIL_MOISTURE;
        }
        return normalized;
    }
}
