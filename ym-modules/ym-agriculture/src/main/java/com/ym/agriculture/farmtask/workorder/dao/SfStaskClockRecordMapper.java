package com.ym.agriculture.farmtask.workorder.dao;

import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskClockRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * stask 到岗打卡记录 Mapper。
 */
@Mapper
public interface SfStaskClockRecordMapper extends BaseMapperPlus<SfStaskClockRecord, SfStaskClockRecord> {

    /**
     * 批量查询工单打卡记录（按工单取 clock_time 最新一条需在 Service 聚合）。
     *
     * @param tenantId 租户编号
     * @param orderIds 工单 ID 集合
     * @return 打卡记录列表
     */
    default List<SfStaskClockRecord> selectByOrderIds(String tenantId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskClockRecord>lambdaQuery()
            .eq(SfStaskClockRecord::getTenantId, tenantId)
            .in(SfStaskClockRecord::getOrderId, orderIds)
            .orderByDesc(SfStaskClockRecord::getClockTime));
    }
}
