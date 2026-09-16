package com.ym.agriculture.farming.farmrecord.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecordMedia;

import java.util.Collection;
import java.util.List;

/**
 * 农事媒体，数据源 smart-farming。
 *
 * @author ym-cloud
 */
public interface SfFarmingRecordMediaMapper extends BaseMapper<SfFarmingRecordMedia> {

    default List<SfFarmingRecordMedia> selectByRecordId(String tenantId, Long recordId) {
        return selectList(Wrappers.<SfFarmingRecordMedia>lambdaQuery()
            .eq(SfFarmingRecordMedia::getTenantId, tenantId)
            .eq(SfFarmingRecordMedia::getRecordId, recordId)
            .orderByAsc(SfFarmingRecordMedia::getSeq)
            .orderByAsc(SfFarmingRecordMedia::getMediaId));
    }

    default int deleteByRecordId(String tenantId, Long recordId) {
        return delete(Wrappers.<SfFarmingRecordMedia>lambdaQuery()
            .eq(SfFarmingRecordMedia::getTenantId, tenantId)
            .eq(SfFarmingRecordMedia::getRecordId, recordId));
    }

    default List<SfFarmingRecordMedia> selectByRecordIds(String tenantId, Collection<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmingRecordMedia>lambdaQuery()
            .eq(SfFarmingRecordMedia::getTenantId, tenantId)
            .in(SfFarmingRecordMedia::getRecordId, recordIds)
            .orderByAsc(SfFarmingRecordMedia::getSeq)
            .orderByAsc(SfFarmingRecordMedia::getMediaId));
    }

    default SfFarmingRecordMedia selectByTenantRecordAndMediaId(String tenantId, Long recordId, Long mediaId) {
        return selectOne(Wrappers.<SfFarmingRecordMedia>lambdaQuery()
            .eq(SfFarmingRecordMedia::getTenantId, tenantId)
            .eq(SfFarmingRecordMedia::getRecordId, recordId)
            .eq(SfFarmingRecordMedia::getMediaId, mediaId));
    }
}
