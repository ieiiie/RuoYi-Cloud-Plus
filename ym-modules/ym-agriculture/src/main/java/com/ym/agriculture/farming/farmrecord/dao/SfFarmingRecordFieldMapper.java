package com.ym.agriculture.farming.farmrecord.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecordField;

import java.util.Collection;
import java.util.List;

/**
 * 农事记录地块明细 Mapper。
 *
 * @author ym-cloud
 */
public interface SfFarmingRecordFieldMapper extends BaseMapper<SfFarmingRecordField> {

    /**
     * 按记录查询地块明细。
     */
    default List<SfFarmingRecordField> selectByRecordId(String tenantId, Long recordId) {
        return selectList(Wrappers.<SfFarmingRecordField>lambdaQuery()
            .eq(SfFarmingRecordField::getTenantId, tenantId)
            .eq(SfFarmingRecordField::getRecordId, recordId)
            .orderByAsc(SfFarmingRecordField::getSortOrder)
            .orderByAsc(SfFarmingRecordField::getId));
    }

    /**
     * 按记录批量查询地块明细。
     */
    default List<SfFarmingRecordField> selectByRecordIds(String tenantId, Collection<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmingRecordField>lambdaQuery()
            .eq(SfFarmingRecordField::getTenantId, tenantId)
            .in(SfFarmingRecordField::getRecordId, recordIds)
            .orderByAsc(SfFarmingRecordField::getSortOrder)
            .orderByAsc(SfFarmingRecordField::getId));
    }

    /**
     * 按地块查询关联记录 ID。
     */
    default List<Long> selectRecordIdsByFieldId(String tenantId, Long fieldId) {
        if (fieldId == null) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmingRecordField>lambdaQuery()
                .select(SfFarmingRecordField::getRecordId)
                .eq(SfFarmingRecordField::getTenantId, tenantId)
                .eq(SfFarmingRecordField::getFieldId, fieldId))
            .stream()
            .map(SfFarmingRecordField::getRecordId)
            .distinct()
            .toList();
    }

    /**
     * 按地块集合查询关联记录 ID。
     */
    default List<Long> selectRecordIdsByFieldIds(String tenantId, Collection<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmingRecordField>lambdaQuery()
                .select(SfFarmingRecordField::getRecordId)
                .eq(SfFarmingRecordField::getTenantId, tenantId)
                .in(SfFarmingRecordField::getFieldId, fieldIds))
            .stream()
            .map(SfFarmingRecordField::getRecordId)
            .distinct()
            .toList();
    }

    /**
     * 删除某条记录下全部地块明细。
     */
    default int deleteByRecordId(String tenantId, Long recordId) {
        return delete(Wrappers.<SfFarmingRecordField>lambdaQuery()
            .eq(SfFarmingRecordField::getTenantId, tenantId)
            .eq(SfFarmingRecordField::getRecordId, recordId));
    }
}
