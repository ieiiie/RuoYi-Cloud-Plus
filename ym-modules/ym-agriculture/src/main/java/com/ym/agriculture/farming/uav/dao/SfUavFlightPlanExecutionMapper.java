package com.ym.agriculture.farming.uav.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.uav.model.entity.SfUavFlightPlanExecution;

import java.util.Date;
import java.util.List;

public interface SfUavFlightPlanExecutionMapper extends BaseMapper<SfUavFlightPlanExecution> {

    default SfUavFlightPlanExecution selectOneByPlanOccurrence(Long planId, Date occurrenceAt) {
        return selectOne(Wrappers.<SfUavFlightPlanExecution>lambdaQuery()
            .eq(SfUavFlightPlanExecution::getPlanId, planId)
            .eq(SfUavFlightPlanExecution::getOccurrenceAt, occurrenceAt)
            .last("LIMIT 1"));
    }

    default List<SfUavFlightPlanExecution> selectByPlanId(Long planId) {
        return selectList(Wrappers.<SfUavFlightPlanExecution>lambdaQuery()
            .eq(SfUavFlightPlanExecution::getPlanId, planId)
            .orderByDesc(SfUavFlightPlanExecution::getOccurrenceAt));
    }
}
