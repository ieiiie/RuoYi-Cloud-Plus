package com.ym.agriculture.farming.uav.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.uav.model.constants.SfUavFlightPlanConstants;
import com.ym.agriculture.farming.uav.model.entity.SfUavFlightPlan;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface SfUavFlightPlanMapper extends BaseMapper<SfUavFlightPlan> {

    default List<SfUavFlightPlan> selectByPlantingBatchId(Long plantingBatchId) {
        return selectList(Wrappers.<SfUavFlightPlan>lambdaQuery()
            .eq(SfUavFlightPlan::getPlantingBatchId, plantingBatchId)
            .orderByDesc(SfUavFlightPlan::getCreateTime));
    }

    default List<SfUavFlightPlan> selectPlanningWithNextOccurrence() {
        return selectList(Wrappers.<SfUavFlightPlan>lambdaQuery()
            .eq(SfUavFlightPlan::getStatus, SfUavFlightPlanConstants.STATUS_PLANNING)
            .isNotNull(SfUavFlightPlan::getNextOccurrenceAt));
    }

    default long countPendingByPlantingBatchId(Long plantingBatchId) {
        if (plantingBatchId == null) {
            return 0L;
        }
        return selectCount(Wrappers.<SfUavFlightPlan>lambdaQuery()
            .eq(SfUavFlightPlan::getPlantingBatchId, plantingBatchId)
            .eq(SfUavFlightPlan::getStatus, SfUavFlightPlanConstants.STATUS_PLANNING));
    }

    default long countByPlantingBatchId(Long plantingBatchId) {
        if (plantingBatchId == null) {
            return 0L;
        }
        return selectCount(Wrappers.<SfUavFlightPlan>lambdaQuery()
            .eq(SfUavFlightPlan::getPlantingBatchId, plantingBatchId));
    }

    /**
     * 按地块统计未执行飞行计划数量。
     */
    default long countPendingByFieldId(Long fieldId) {
        if (fieldId == null) {
            return 0L;
        }
        return selectCount(Wrappers.<SfUavFlightPlan>lambdaQuery()
            .eq(SfUavFlightPlan::getFieldId, fieldId)
            .eq(SfUavFlightPlan::getStatus, SfUavFlightPlanConstants.STATUS_PLANNING));
    }

    /**
     * 将地块下未执行飞行计划暂停。
     */
    default int pausePendingByFieldId(Long fieldId) {
        if (fieldId == null) {
            return 0;
        }
        return update(null, Wrappers.<SfUavFlightPlan>lambdaUpdate()
            .eq(SfUavFlightPlan::getFieldId, fieldId)
            .eq(SfUavFlightPlan::getStatus, SfUavFlightPlanConstants.STATUS_PLANNING)
            .set(SfUavFlightPlan::getStatus, SfUavFlightPlanConstants.STATUS_PAUSED));
    }

    default Set<Long> selectBatchIdsWithPendingPlan(Collection<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return Set.of();
        }
        return selectList(Wrappers.<SfUavFlightPlan>lambdaQuery()
                .select(SfUavFlightPlan::getPlantingBatchId)
                .in(SfUavFlightPlan::getPlantingBatchId, batchIds)
                .eq(SfUavFlightPlan::getStatus, SfUavFlightPlanConstants.STATUS_PLANNING))
            .stream()
            .map(SfUavFlightPlan::getPlantingBatchId)
            .collect(Collectors.toSet());
    }
}
