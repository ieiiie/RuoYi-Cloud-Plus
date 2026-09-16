package com.ym.agriculture.farming.i18n.support;

import cn.hutool.crypto.digest.DigestUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import com.ym.agriculture.farming.crop.support.GrowthStageConfigSupport;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 大棚、作物与种植批次的翻译源工厂。
 */
public final class SmartFarmingI18nSourceFactory {

    private static final int FIELD_KEY_MAX_LENGTH = 128;

    private SmartFarmingI18nSourceFactory() {
    }

    /**
     * 提取大棚展示文本；普通大田不在本次预翻译范围。
     *
     * @param row 地块实体
     * @return 可翻译文本
     */
    public static List<I18nTextSource> sources(SfField row) {
        if (row == null || row.getFieldId() == null || !FieldType.isGreenhouse(row.getFieldType())) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.SMART_FARMING_FIELD, row.getFieldId(), "fieldName", row.getFieldName());
        add(sources, I18nResourceType.SMART_FARMING_FIELD, row.getFieldId(), "addressText", row.getAddressText());
        add(sources, I18nResourceType.SMART_FARMING_FIELD, row.getFieldId(), "adminDivisionText",
            row.getAdminDivisionText());
        add(sources, I18nResourceType.SMART_FARMING_FIELD, row.getFieldId(), "remark", row.getRemark());
        return List.copyOf(sources);
    }

    /**
     * 提取物种名称、备注和生长阶段名称。
     *
     * @param row 物种实体
     * @return 可翻译文本
     */
    public static List<I18nTextSource> sources(SfCropSpecies row) {
        if (row == null || row.getSpeciesId() == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.SMART_FARMING_CROP_SPECIES, row.getSpeciesId(), "speciesName",
            row.getSpeciesName());
        add(sources, I18nResourceType.SMART_FARMING_CROP_SPECIES, row.getSpeciesId(), "remark", row.getRemark());
        for (Map<String, Object> stage : GrowthStageConfigSupport.listStagesStrict(row.getGrowthStageConfigJson())) {
            Object codeValue = stage.get("code");
            Object nameValue = stage.get("name");
            String code = codeValue == null ? null : codeValue.toString();
            String name = nameValue == null ? null : nameValue.toString();
            if (StringUtils.isBlank(code)) {
                continue;
            }
            add(sources, I18nResourceType.SMART_FARMING_CROP_SPECIES, row.getSpeciesId(),
                growthStageFieldKey(code), name);
        }
        return List.copyOf(sources);
    }

    /**
     * 提取品种展示文本。
     *
     * @param row 品种实体
     * @return 可翻译文本
     */
    public static List<I18nTextSource> sources(SfCropVariety row) {
        if (row == null || row.getVarietyId() == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.SMART_FARMING_CROP_VARIETY, row.getVarietyId(), "varietyName",
            row.getVarietyName());
        add(sources, I18nResourceType.SMART_FARMING_CROP_VARIETY, row.getVarietyId(), "remark", row.getRemark());
        return List.copyOf(sources);
    }

    /**
     * 提取种植批次自身的用户文本；批次编号保持技术值原样。
     *
     * @param row 种植批次实体
     * @return 可翻译文本
     */
    public static List<I18nTextSource> sources(SfPlantingBatch row) {
        if (row == null || row.getBatchId() == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.SMART_FARMING_PLANTING_BATCH, row.getBatchId(), "remark",
            row.getRemark());
        return List.copyOf(sources);
    }

    private static String growthStageFieldKey(String code) {
        String normalizedCode = code.trim();
        String direct = "growthStageConfigJson.stages[code=" + normalizedCode + "].name";
        if (normalizedCode.matches("[A-Za-z0-9._-]+") && direct.length() <= FIELD_KEY_MAX_LENGTH) {
            return direct;
        }
        return "growthStageConfigJson.stages[codeHash=" + DigestUtil.sha256Hex(normalizedCode) + "].name";
    }

    private static void add(List<I18nTextSource> sources, String resourceType, Long resourceId, String fieldKey,
        String sourceText) {
        if (StringUtils.isNotBlank(sourceText)) {
            // 词条哈希必须基于业务表保存的完整原文，不能在登记阶段做 trim 等规范化。
            sources.add(new I18nTextSource(resourceType, resourceId, fieldKey, sourceText));
        }
    }
}
