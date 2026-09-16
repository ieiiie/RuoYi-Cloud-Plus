package com.ym.agriculture.farming.satellite.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.satellite.model.bo.SatelliteScheduleQueryBo;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteScheduleStatus;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteSchedule;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 遥感周期计划 Mapper，对应表 {@code sf_satellite_schedule}。
 *
 * @author ym-cloud
 */
@Mapper
public interface SfSatelliteScheduleMapper extends BaseMapper<SfSatelliteSchedule> {

    /**
     * 定时任务扫描入口：查询所有到期且处于周期中的计划。
     * <p>
     * 条件：{@code schedule_status = 1（周期中） AND next_run_date <= today}（{@code del_flag} 由 MyBatis-Plus 逻辑删除自动追加）。
     *
     * @param today 当天日期（Asia/Shanghai 时区）
     * @return 到期的周期中计划列表
     */
    default List<SfSatelliteSchedule> selectActiveSchedulesDueToday(LocalDate today) {
        return selectList(Wrappers.<SfSatelliteSchedule>lambdaQuery()
            .eq(SfSatelliteSchedule::getScheduleStatus, 1)
            .le(SfSatelliteSchedule::getNextRunDate, today));
    }

    /**
     * 按种植批次查询计划列表，按创建时间倒序。
     *
     * @param plantingBatchId 种植批次 ID
     * @return 该批次下的所有计划
     */
    default List<SfSatelliteSchedule> selectByPlantingBatchId(Long plantingBatchId) {
        return selectList(Wrappers.<SfSatelliteSchedule>lambdaQuery()
            .eq(SfSatelliteSchedule::getPlantingBatchId, plantingBatchId)
            .orderByDesc(SfSatelliteSchedule::getCreateTime));
    }

    /**
     * 查询所有未完成且未删除的计划（schedule_status in (0, 1)），供定时任务扫描。
     */
    default List<SfSatelliteSchedule> selectAllActiveOrPendingSchedules() {
        return selectList(Wrappers.<SfSatelliteSchedule>lambdaQuery()
            .in(SfSatelliteSchedule::getScheduleStatus,
                SatelliteScheduleStatus.NOT_STARTED,
                SatelliteScheduleStatus.IN_PROGRESS));
    }

    /**
     * 分页查询计划列表。
     */
    default Page<SfSatelliteSchedule> selectSchedulePage(Page<SfSatelliteSchedule> page,
                                                         SatelliteScheduleQueryBo bo) {
        LambdaQueryWrapper<SfSatelliteSchedule> w = Wrappers.lambdaQuery();
        if (bo != null) {
            if (StringUtils.isNotBlank(bo.getSpeciesName())) {
                w.like(SfSatelliteSchedule::getSpeciesName, bo.getSpeciesName().trim());
            }
            if (StringUtils.isNotBlank(bo.getTaskType())) {
                w.like(SfSatelliteSchedule::getTaskType, bo.getTaskType().trim());
            }
            if (bo.getFieldId() != null) {
                w.eq(SfSatelliteSchedule::getFieldId, bo.getFieldId());
            }
            if (bo.getScheduleStatus() != null) {
                w.eq(SfSatelliteSchedule::getScheduleStatus, bo.getScheduleStatus());
            }
        }
        w.orderByDesc(SfSatelliteSchedule::getCreateTime);
        return selectPage(page, w);
    }

    /**
     * 批量查询存在周期中计划（schedule_status=1）的种植批次 ID 集合。
     */
    default Set<Long> selectBatchIdsWithActiveSchedule(Collection<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return Set.of();
        }
        return selectList(Wrappers.<SfSatelliteSchedule>lambdaQuery()
                .select(SfSatelliteSchedule::getPlantingBatchId)
                .in(SfSatelliteSchedule::getPlantingBatchId, batchIds)
                .eq(SfSatelliteSchedule::getScheduleStatus, 1))
            .stream()
            .map(SfSatelliteSchedule::getPlantingBatchId)
            .collect(Collectors.toSet());
    }

    /**
     * 按地块统计未完成遥感计划数量。
     */
    default long countActiveOrPendingByFieldId(Long fieldId) {
        if (fieldId == null) {
            return 0L;
        }
        return selectCount(Wrappers.<SfSatelliteSchedule>lambdaQuery()
            .eq(SfSatelliteSchedule::getFieldId, fieldId)
            .in(SfSatelliteSchedule::getScheduleStatus,
                SatelliteScheduleStatus.NOT_STARTED,
                SatelliteScheduleStatus.IN_PROGRESS));
    }

    /**
     * 将地块下未完成遥感计划暂停。
     */
    default int pauseActiveOrPendingByFieldId(Long fieldId) {
        if (fieldId == null) {
            return 0;
        }
        return update(null, Wrappers.<SfSatelliteSchedule>lambdaUpdate()
            .eq(SfSatelliteSchedule::getFieldId, fieldId)
            .in(SfSatelliteSchedule::getScheduleStatus,
                SatelliteScheduleStatus.NOT_STARTED,
                SatelliteScheduleStatus.IN_PROGRESS)
            .set(SfSatelliteSchedule::getScheduleStatus, SatelliteScheduleStatus.PAUSED));
    }
}
