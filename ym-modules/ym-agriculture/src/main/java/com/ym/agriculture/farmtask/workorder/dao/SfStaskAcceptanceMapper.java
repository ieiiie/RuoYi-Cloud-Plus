package com.ym.agriculture.farmtask.workorder.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * stask 验收记录 Mapper。
 */
@Mapper
public interface SfStaskAcceptanceMapper extends BaseMapperPlus<SfStaskAcceptance, SfStaskAcceptance> {

    /**
     * 查询工单最新验收记录。
     *
     * @param tenantId 租户编号
     * @param orderId  工单ID
     * @return 最新验收记录
     */
    default SfStaskAcceptance selectLatestByOrderId(String tenantId, Long orderId) {
        return selectOne(Wrappers.<SfStaskAcceptance>lambdaQuery()
            .eq(SfStaskAcceptance::getTenantId, tenantId)
            .eq(SfStaskAcceptance::getOrderId, orderId)
            .orderByDesc(SfStaskAcceptance::getAcceptedAt)
            .last("LIMIT 1"));
    }

    /**
     * 批量查询工单验收记录。
     *
     * @param tenantId 租户编号
     * @param orderIds 工单 ID 集合
     * @return 验收记录列表
     */
    default List<SfStaskAcceptance> selectByOrderIds(String tenantId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskAcceptance>lambdaQuery()
            .eq(SfStaskAcceptance::getTenantId, tenantId)
            .in(SfStaskAcceptance::getOrderId, orderIds)
            .orderByDesc(SfStaskAcceptance::getAcceptedAt));
    }
}
