package com.ym.agriculture.farming.farmrecord.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecordWorkItem;

import java.util.Collection;
import java.util.List;

/**
 * 农事记录项目明细 Mapper。
 *
 * @author ym-cloud
 */
public interface SfFarmingRecordWorkItemMapper extends BaseMapper<SfFarmingRecordWorkItem> {

    /**
     * 按记录查询农事项目明细。
     */
    default List<SfFarmingRecordWorkItem> selectByRecordId(String tenantId, Long recordId) {
        return selectList(Wrappers.<SfFarmingRecordWorkItem>lambdaQuery()
            .eq(SfFarmingRecordWorkItem::getTenantId, tenantId)
            .eq(SfFarmingRecordWorkItem::getRecordId, recordId)
            .orderByAsc(SfFarmingRecordWorkItem::getSortOrder)
            .orderByAsc(SfFarmingRecordWorkItem::getItemId));
    }

    /**
     * 按记录批量查询农事项目明细。
     */
    default List<SfFarmingRecordWorkItem> selectByRecordIds(String tenantId, Collection<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmingRecordWorkItem>lambdaQuery()
            .eq(SfFarmingRecordWorkItem::getTenantId, tenantId)
            .in(SfFarmingRecordWorkItem::getRecordId, recordIds)
            .orderByAsc(SfFarmingRecordWorkItem::getSortOrder)
            .orderByAsc(SfFarmingRecordWorkItem::getItemId));
    }

    /**
     * 按农事项目查询关联记录 ID。
     */
    default List<Long> selectRecordIdsByWorkItemId(String tenantId, Long workItemId) {
        if (workItemId == null) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmingRecordWorkItem>lambdaQuery()
                .select(SfFarmingRecordWorkItem::getRecordId)
                .eq(SfFarmingRecordWorkItem::getTenantId, tenantId)
                .eq(SfFarmingRecordWorkItem::getWorkItemId, workItemId))
            .stream()
            .map(SfFarmingRecordWorkItem::getRecordId)
            .distinct()
            .toList();
    }

    /**
     * 按农事分类查询关联记录 ID。
     */
    default List<Long> selectRecordIdsByCategoryId(String tenantId, Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmingRecordWorkItem>lambdaQuery()
                .select(SfFarmingRecordWorkItem::getRecordId)
                .eq(SfFarmingRecordWorkItem::getTenantId, tenantId)
                .eq(SfFarmingRecordWorkItem::getCategoryId, categoryId))
            .stream()
            .map(SfFarmingRecordWorkItem::getRecordId)
            .distinct()
            .toList();
    }

    /**
     * 删除某条记录下全部农事项目明细。
     */
    default int deleteByRecordId(String tenantId, Long recordId) {
        return delete(Wrappers.<SfFarmingRecordWorkItem>lambdaQuery()
            .eq(SfFarmingRecordWorkItem::getTenantId, tenantId)
            .eq(SfFarmingRecordWorkItem::getRecordId, recordId));
    }
}
