package com.ym.agriculture.farmtask.workorder.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * stask 完工记录 Mapper。
 */
@Mapper
public interface SfStaskCompletionMapper extends BaseMapperPlus<SfStaskCompletion, SfStaskCompletion> {

    /**
     * 查询工单最新完工记录。
     *
     * @param tenantId 租户编号
     * @param orderId  工单ID
     * @return 最新完工记录
     */
    default SfStaskCompletion selectLatestByOrderId(String tenantId, Long orderId) {
        return selectOne(Wrappers.<SfStaskCompletion>lambdaQuery()
            .eq(SfStaskCompletion::getTenantId, tenantId)
            .eq(SfStaskCompletion::getOrderId, orderId)
            .orderByDesc(SfStaskCompletion::getCompletedAt, SfStaskCompletion::getCompletionId)
            .last("LIMIT 1"));
    }

    /**
     * 批量查询工单完工记录（按工单取 completed_at 最新一条需在 Service 聚合）。
     *
     * @param tenantId 租户编号
     * @param orderIds 工单 ID 集合
     * @return 完工记录列表
     */
    default List<SfStaskCompletion> selectByOrderIds(String tenantId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskCompletion>lambdaQuery()
            .eq(SfStaskCompletion::getTenantId, tenantId)
            .in(SfStaskCompletion::getOrderId, orderIds)
            .orderByDesc(SfStaskCompletion::getCompletedAt, SfStaskCompletion::getCompletionId));
    }
}
