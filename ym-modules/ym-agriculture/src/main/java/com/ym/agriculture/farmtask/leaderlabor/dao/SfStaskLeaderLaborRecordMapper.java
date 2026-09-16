package com.ym.agriculture.farmtask.leaderlabor.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.leaderlabor.model.entity.SfStaskLeaderLaborRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** 组长日用工记录数据访问层。 */
@Mapper
public interface SfStaskLeaderLaborRecordMapper extends BaseMapperPlus<SfStaskLeaderLaborRecord, SfStaskLeaderLaborRecord> {

    /** 汇总某个计划日的组长日用工记录。 */
    @Select("SELECT COALESCE(SUM(labor_count), 0) FROM sf_stask_leader_labor_record WHERE tenant_id = #{tenantId} AND plan_date = #{planDate}")
    BigDecimal sumByPlanDate(@Param("tenantId") String tenantId, @Param("planDate") LocalDate planDate);

    /** 汇总租户内全部组长日用工记录。 */
    @Select("SELECT COALESCE(SUM(labor_count), 0) FROM sf_stask_leader_labor_record WHERE tenant_id = #{tenantId}")
    BigDecimal sumAll(@Param("tenantId") String tenantId);

    default SfStaskLeaderLaborRecord selectByLeaderAndPlanDateForUpdate(String tenantId, Long leaderId,
        LocalDate planDate) {
        return selectOne(Wrappers.<SfStaskLeaderLaborRecord>lambdaQuery()
            .eq(SfStaskLeaderLaborRecord::getTenantId, tenantId)
            .eq(SfStaskLeaderLaborRecord::getLeaderEmployeeId, leaderId)
            .eq(SfStaskLeaderLaborRecord::getPlanDate, planDate)
            .last("FOR UPDATE"));
    }

    default SfStaskLeaderLaborRecord selectByLeaderAndPlanDate(String tenantId, Long leaderId, LocalDate planDate) {
        return selectOne(Wrappers.<SfStaskLeaderLaborRecord>lambdaQuery()
            .eq(SfStaskLeaderLaborRecord::getTenantId, tenantId)
            .eq(SfStaskLeaderLaborRecord::getLeaderEmployeeId, leaderId)
            .eq(SfStaskLeaderLaborRecord::getPlanDate, planDate));
    }

    default List<SfStaskLeaderLaborRecord> selectByLeadersAndPlanDates(String tenantId,
        Collection<Long> leaderIds, Collection<LocalDate> planDates) {
        if (leaderIds == null || leaderIds.isEmpty() || planDates == null || planDates.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskLeaderLaborRecord>lambdaQuery()
            .eq(SfStaskLeaderLaborRecord::getTenantId, tenantId)
            .in(SfStaskLeaderLaborRecord::getLeaderEmployeeId, leaderIds)
            .in(SfStaskLeaderLaborRecord::getPlanDate, planDates));
    }
}
