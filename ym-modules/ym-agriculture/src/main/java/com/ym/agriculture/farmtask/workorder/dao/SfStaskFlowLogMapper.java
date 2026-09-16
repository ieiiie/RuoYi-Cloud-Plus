package com.ym.agriculture.farmtask.workorder.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * stask 工单流转日志 Mapper。
 */
@Mapper
public interface SfStaskFlowLogMapper extends BaseMapperPlus<SfStaskFlowLog, SfStaskFlowLog> {

    /**
     * 查询工单流转日志。
     *
     * @param tenantId 租户编号
     * @param orderId  工单ID
     * @return 流转日志
     */
    default List<SfStaskFlowLog> selectByOrderId(String tenantId, Long orderId) {
        return selectList(Wrappers.<SfStaskFlowLog>lambdaQuery()
            .eq(SfStaskFlowLog::getTenantId, tenantId)
            .eq(SfStaskFlowLog::getOrderId, orderId)
            .orderByAsc(SfStaskFlowLog::getCreateTime));
    }

    /**
     * 查询工单指定事件的最近一条流转日志。
     *
     * @param tenantId 租户编号
     * @param orderId  工单 ID
     * @param event    流转事件编码
     * @return 最近流转日志，无则 null
     */
    default SfStaskFlowLog selectLatestByOrderIdAndEvent(String tenantId, Long orderId, String event) {
        return selectOne(Wrappers.<SfStaskFlowLog>lambdaQuery()
            .eq(SfStaskFlowLog::getTenantId, tenantId)
            .eq(SfStaskFlowLog::getOrderId, orderId)
            .eq(SfStaskFlowLog::getEvent, event)
            .orderByDesc(SfStaskFlowLog::getCreateTime)
            .last("LIMIT 1"));
    }

    /**
     * 批量查询工单流转日志。
     *
     * @param tenantId 租户编号
     * @param orderIds 工单 ID 集合
     * @return 流转日志列表
     */
    default List<SfStaskFlowLog> selectByOrderIds(String tenantId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskFlowLog>lambdaQuery()
            .eq(SfStaskFlowLog::getTenantId, tenantId)
            .in(SfStaskFlowLog::getOrderId, orderIds)
            .orderByDesc(SfStaskFlowLog::getCreateTime));
    }
}
