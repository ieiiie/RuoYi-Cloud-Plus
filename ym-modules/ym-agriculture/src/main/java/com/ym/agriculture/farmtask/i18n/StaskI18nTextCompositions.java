package com.ym.agriculture.farmtask.i18n;

import cn.hutool.core.util.StrUtil;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;

import java.util.List;
import java.util.stream.Collectors;

/**
 * stask 本地化组合文本规则。
 */
public final class StaskI18nTextCompositions {

    private StaskI18nTextCompositions() {
    }

    /**
     * 组合拆分工单标题。
     *
     * @param greenhouseName 大棚名称
     * @param workItemName 农事项名称
     * @return 组合标题
     */
    public static String splitTitle(String greenhouseName, String workItemName) {
        if (StrUtil.isBlank(greenhouseName)) {
            return workItemName;
        }
        if (StrUtil.isBlank(workItemName)) {
            return greenhouseName;
        }
        return greenhouseName + " · " + workItemName;
    }

    /**
     * 使用已本地化的种植批次名称组合大棚作物展示值。
     *
     * @param batches 种植批次
     * @param separator 当前语言使用的列表分隔符
     * @return 作物展示值；没有名称时返回 {@code null}
     */
    public static String cropNames(List<SfPlantingBatchVo> batches, String separator) {
        if (batches == null || batches.isEmpty()) {
            return null;
        }
        String cropName = batches.stream()
            .filter(batch -> batch != null)
            .map(batch -> StrUtil.blankToDefault(batch.getSpeciesName(), batch.getVarietyName()))
            .map(StrUtil::trim)
            .filter(StrUtil::isNotBlank)
            .distinct()
            .collect(Collectors.joining(separator));
        return StrUtil.emptyToDefault(cropName, null);
    }
}
