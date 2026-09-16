package com.ym.agriculture.farmtask.clocklocation.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farmtask.clocklocation.model.entity.SfStaskClockLocation;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 打卡地点 Mapper。
 */
public interface SfStaskClockLocationMapper
    extends BaseMapperPlus<SfStaskClockLocation, SfStaskClockLocation> {

    default SfStaskClockLocation selectByTenantId(String tenantId) {
        return selectOne(Wrappers.<SfStaskClockLocation>lambdaQuery()
            .eq(SfStaskClockLocation::getTenantId, tenantId));
    }
}
