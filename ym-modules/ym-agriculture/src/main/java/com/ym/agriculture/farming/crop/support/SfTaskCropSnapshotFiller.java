package com.ym.agriculture.farming.crop.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.dao.SfCropVarietyMapper;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.entity.SfField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 按地块、种植批次与可选品种编码解析冗余展示字段。
 */
@Component
@RequiredArgsConstructor
public class SfTaskCropSnapshotFiller {

    private final SfFieldMapper fieldMapper;
    private final SfPlantingBatchMapper plantingBatchMapper;
    private final SfCropVarietyMapper varietyMapper;
    private final SfCropSpeciesMapper speciesMapper;

    /**
     * @param fieldId            地块 ID，可空
     * @param plantingBatchId    种植批次 ID，可空
     * @param tenantId           租户，按品种编码反查时必填
     * @param varietyCodeFallback 无批次时按 {@code sf_crop_variety.variety_code} 反查（如遥感 {@code code_croptype}）
     */
    public SfTaskCropSnapshot resolve(Long fieldId, Long plantingBatchId, String tenantId, String varietyCodeFallback) {
        SfTaskCropSnapshot out = new SfTaskCropSnapshot();
        SfPlantingBatch batch = plantingBatchId != null ? plantingBatchMapper.selectById(plantingBatchId) : null;
        boolean batchOk = batch != null && SystemConstants.NORMAL.equals(batch.getDelFlag());
        if (batchOk) {
            out.setPlantingBatchId(batch.getBatchId());
            if (StringUtils.isNotBlank(batch.getBatchCode())) {
                out.setPlantingBatchName(batch.getBatchCode().trim());
            }
        }
        fillFieldNameFromFieldAndBatch(out, fieldId, batchOk ? batch : null);

        Long varietyId = null;
        if (batchOk) {
            varietyId = batch.getVarietyId();
        }
        if (varietyId == null && StringUtils.isNotBlank(varietyCodeFallback) && StringUtils.isNotBlank(tenantId)) {
            SfCropVariety byCode = varietyMapper.selectOne(Wrappers.<SfCropVariety>lambdaQuery()
                .eq(SfCropVariety::getTenantId, tenantId.trim())
                .eq(SfCropVariety::getVarietyCode, varietyCodeFallback.trim())
                .eq(SfCropVariety::getDelFlag, SystemConstants.NORMAL)
                .last("LIMIT 1"));
            if (byCode != null) {
                varietyId = byCode.getVarietyId();
            }
        }
        if (varietyId != null) {
            SfCropVariety variety = varietyMapper.selectById(varietyId);
            if (variety != null && SystemConstants.NORMAL.equals(variety.getDelFlag())) {
                out.setVarietyId(variety.getVarietyId());
                out.setVarietyName(variety.getVarietyName());
                if (variety.getSpeciesId() != null) {
                    SfCropSpecies species = speciesMapper.selectById(variety.getSpeciesId());
                    if (species != null && SystemConstants.NORMAL.equals(species.getDelFlag())) {
                        out.setSpeciesId(species.getSpeciesId());
                        out.setSpeciesName(species.getSpeciesName());
                        out.setRemoteSensingCode(species.getRemoteSensingCode());
                    }
                }
            }
        }
        return out;
    }

    /**
     * 先按入参 fieldId，否则按批次 fieldId；查不到时再按批次关联地块重试（修正入参与批次不一致的情况）。
     */
    private void fillFieldNameFromFieldAndBatch(SfTaskCropSnapshot out, Long fieldId, SfPlantingBatch batch) {
        Long first = fieldId != null ? fieldId : (batch != null ? batch.getFieldId() : null);
        SfField field = selectNormalFieldById(first);
        if (field != null) {
            out.setFieldName(field.getFieldName());
            return;
        }
        if (batch != null && batch.getFieldId() != null && !Objects.equals(first, batch.getFieldId())) {
            field = selectNormalFieldById(batch.getFieldId());
            if (field != null) {
                out.setFieldName(field.getFieldName());
            }
        }
    }

    private SfField selectNormalFieldById(Long id) {
        if (id == null) {
            return null;
        }
        SfField f = fieldMapper.selectById(id);
        if (f != null && SystemConstants.NORMAL.equals(f.getDelFlag())) {
            return f;
        }
        return null;
    }
}
