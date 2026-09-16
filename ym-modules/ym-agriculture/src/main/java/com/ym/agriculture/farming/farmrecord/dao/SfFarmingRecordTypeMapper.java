package com.ym.agriculture.farming.farmrecord.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecordType;

import java.util.Collection;
import java.util.List;

/**
 * 农事类型，数据源 smart-farming。
 *
 * @author ym-cloud
 */
public interface SfFarmingRecordTypeMapper extends BaseMapperPlus<SfFarmingRecordType, SfFarmingRecordType> {

    default List<SfFarmingRecordType> selectEnabledSorted(String tenantId) {
        return selectList(Wrappers.<SfFarmingRecordType>lambdaQuery()
            .eq(SfFarmingRecordType::getTenantId, tenantId)
            .eq(SfFarmingRecordType::getStatus, SystemConstants.NORMAL)
            .eq(SfFarmingRecordType::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SfFarmingRecordType::getSortOrder)
            .orderByAsc(SfFarmingRecordType::getTypeId));
    }

    default SfFarmingRecordType selectNormalByTenantAndCode(String tenantId, String typeCode) {
        return selectOne(Wrappers.<SfFarmingRecordType>lambdaQuery()
            .eq(SfFarmingRecordType::getTenantId, tenantId)
            .eq(SfFarmingRecordType::getTypeCode, typeCode)
            .eq(SfFarmingRecordType::getDelFlag, SystemConstants.NORMAL));
    }

    default boolean existsTenantCode(Collection<Long> excludeTypeIds, String tenantId, String typeCode) {
        var w = Wrappers.<SfFarmingRecordType>lambdaQuery()
            .eq(SfFarmingRecordType::getTenantId, tenantId)
            .eq(SfFarmingRecordType::getTypeCode, typeCode)
            .eq(SfFarmingRecordType::getDelFlag, SystemConstants.NORMAL);
        if (excludeTypeIds != null && !excludeTypeIds.isEmpty()) {
            w.notIn(SfFarmingRecordType::getTypeId, excludeTypeIds);
        }
        return exists(w);
    }
}
