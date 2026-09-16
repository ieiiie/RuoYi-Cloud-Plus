package com.ym.agriculture.farming.satellite.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.satellite.model.bo.SatelliteTaskQueryBo;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteScheduleStatus;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteTaskStatus;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 遥感任务 Mapper。
 */
@Mapper
public interface SfSatelliteTaskMapper extends BaseMapper<SfSatelliteTask> {

    /**
     * 按 dkId 查询单条任务。
     */
    default SfSatelliteTask selectByDkId(String dkId) {
        return selectOne(Wrappers.<SfSatelliteTask>lambdaQuery().eq(SfSatelliteTask::getDkId, dkId));
    }

    /**
     * 手工批量补偿提交：{@link SatelliteTaskStatus#PENDING_SUBMIT} 或 {@link SatelliteTaskStatus#FAILED}，
     * 且观测窗口已结束（{@code end_date <= 今天}）的最早 50 条。
     * <p>
     * 日期守卫防止周期计划一次性展开的未来任务被提前提交到遥感平台；窗内不提交。
     */
    default List<SfSatelliteTask> selectPendingSubmitFirst50() {
        LambdaQueryWrapper<SfSatelliteTask> q = Wrappers.lambdaQuery();
        q.and(w -> w.eq(SfSatelliteTask::getStatus, SatelliteTaskStatus.PENDING_SUBMIT)
            .or().eq(SfSatelliteTask::getStatus, SatelliteTaskStatus.FAILED));
        q.apply("(schedule_id IS NULL OR schedule_id NOT IN (SELECT schedule_id FROM sf_satellite_schedule WHERE schedule_status = {0} AND del_flag = '0'))",
            SatelliteScheduleStatus.PAUSED);
        q.orderByAsc(SfSatelliteTask::getCreateTime);
        q.last("LIMIT 50");
        return selectList(q);
    }

    /**
     * 按 scheduleId 查询关联的子任务列表，按创建时间倒序。
     */
    default List<SfSatelliteTask> selectByScheduleId(Long scheduleId) {
        return selectList(Wrappers.<SfSatelliteTask>lambdaQuery()
            .eq(SfSatelliteTask::getScheduleId, scheduleId)
            .orderByDesc(SfSatelliteTask::getCreateTime));
    }

    /**
     * 统计周期计划下仍有效的子任务数量。
     * <p>
     * MyBatis-Plus 会自动排除逻辑删除任务，用于删除最后一个子任务后的计划清理。
     */
    default long countActiveByScheduleId(Long scheduleId) {
        if (scheduleId == null) {
            return 0L;
        }
        return selectCount(Wrappers.<SfSatelliteTask>lambdaQuery()
            .eq(SfSatelliteTask::getScheduleId, scheduleId));
    }

    /**
     * 按种植批次 ID 查询关联的全部子任务列表，按创建时间倒序。
     */
    default List<SfSatelliteTask> selectByDkIds(List<String> dkIds) {
        if (dkIds == null || dkIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(Wrappers.<SfSatelliteTask>lambdaQuery()
            .in(SfSatelliteTask::getDkId, dkIds)
            .orderByAsc(SfSatelliteTask::getCreateTime));
    }

    default List<SfSatelliteTask> selectByPlantingBatchId(Long plantingBatchId) {
        if (plantingBatchId == null) {
            return java.util.Collections.emptyList();
        }
        return selectList(Wrappers.<SfSatelliteTask>lambdaQuery()
            .eq(SfSatelliteTask::getPlantingBatchId, plantingBatchId)
            .orderByDesc(SfSatelliteTask::getCreateTime));
    }

    /**
     * 统计某个 schedule 下已提交（非 PENDING_SUBMIT 状态）的不同周期数。
     * 同一周期的多个 taskType 共享相同的 startDate，按 startDate 去重计数。
     */
    default int countSubmittedCyclesByScheduleId(Long scheduleId) {
        LambdaQueryWrapper<SfSatelliteTask> q = Wrappers.lambdaQuery();
        q.eq(SfSatelliteTask::getScheduleId, scheduleId);
        q.ne(SfSatelliteTask::getStatus, SatelliteTaskStatus.PENDING_SUBMIT);
        q.select(SfSatelliteTask::getStartDate);
        q.groupBy(SfSatelliteTask::getStartDate);
        // selectCount + GROUP BY 会生成多行/非标量结果，MP 可能得到 null；按组列表计数等价于 distinct 周期数
        return selectList(q).size();
    }

    /**
     * 分页列表条件（租户过滤由调用方通过 {@code filterByTenant}/{@code tenantId} 传入）。
     */
    default boolean existsSubmittedTaskByScheduleId(Long scheduleId) {
        return selectCount(Wrappers.<SfSatelliteTask>lambdaQuery()
            .eq(SfSatelliteTask::getScheduleId, scheduleId)
            .ne(SfSatelliteTask::getStatus, SatelliteTaskStatus.PENDING_SUBMIT)) > 0;
    }

    default LambdaQueryWrapper<SfSatelliteTask> buildTaskPageWrapper(boolean filterByTenant, String tenantId, SatelliteTaskQueryBo bo) {
        LambdaQueryWrapper<SfSatelliteTask> w = Wrappers.lambdaQuery();
        if (filterByTenant && StringUtils.isNotBlank(tenantId)) {
            w.eq(SfSatelliteTask::getTenantId, tenantId);
        }
        if (bo != null) {
            w.eq(StringUtils.isNotBlank(bo.getTaskType()), SfSatelliteTask::getTaskType, bo.getTaskType());
            w.eq(bo.getStatus() != null, SfSatelliteTask::getStatus, bo.getStatus());
            if (StringUtils.isNotBlank(bo.getKeyword())) {
                String keyword = bo.getKeyword().trim();
                w.and(q -> q.like(SfSatelliteTask::getFieldName, keyword)
                    .or()
                    .like(SfSatelliteTask::getDkId, keyword));
            }
            w.ge(StringUtils.isNotBlank(bo.getStartDate()), SfSatelliteTask::getStartDate, bo.getStartDate());
            w.le(StringUtils.isNotBlank(bo.getEndDate()), SfSatelliteTask::getEndDate, bo.getEndDate());
            w.eq(bo.getFieldId() != null, SfSatelliteTask::getFieldId, bo.getFieldId());
            likeTrimmedIfNotBlank(w, SfSatelliteTask::getFieldName, bo.getFieldName());
            likeTrimmedIfNotBlank(w, SfSatelliteTask::getSpeciesName, bo.getSpeciesName());
            likeTrimmedIfNotBlank(w, SfSatelliteTask::getVarietyName, bo.getVarietyName());
            likeTrimmedIfNotBlank(w, SfSatelliteTask::getPlantingBatchName, bo.getPlantingBatchName());
        }
        w.orderByDesc(SfSatelliteTask::getCreateTime);
        return w;
    }

    /**
     * 任务分页查询。
     */
    default Page<SfSatelliteTask> selectTaskPage(Page<SfSatelliteTask> page, boolean filterByTenant, String tenantId, SatelliteTaskQueryBo bo) {
        return selectPage(page, buildTaskPageWrapper(filterByTenant, tenantId, bo));
    }

    default boolean existsOverlap(String tenantId, Long fieldId, String taskType, String startDate, String endDate) {
        if (StringUtils.isBlank(tenantId) || fieldId == null || StringUtils.isBlank(taskType)
            || StringUtils.isBlank(startDate) || StringUtils.isBlank(endDate)) {
            return false;
        }
        return exists(Wrappers.<SfSatelliteTask>lambdaQuery()
            .eq(SfSatelliteTask::getTenantId, tenantId)
            .eq(SfSatelliteTask::getFieldId, fieldId)
            .eq(SfSatelliteTask::getTaskType, taskType)
            .eq(SfSatelliteTask::getDelFlag, "0")
            .le(SfSatelliteTask::getStartDate, endDate)
            .ge(SfSatelliteTask::getEndDate, startDate));
    }

    /**
     * 批量查询指定分析类型中与目标日期区间重叠的任务类型。
     */
    @Select("""
        <script>
        SELECT DISTINCT task_type
        FROM sf_satellite_task
        WHERE tenant_id = #{tenantId}
          AND field_id = #{fieldId}
          AND task_type IN
          <foreach collection="taskTypes" item="taskType" open="(" separator="," close=")">
            #{taskType}
          </foreach>
          AND (del_flag = '0' OR del_flag IS NULL)
          AND start_date &lt;= #{endDate}
          AND end_date &gt;= #{startDate}
        </script>
        """)
    List<String> selectOverlapTaskTypes(@Param("tenantId") String tenantId,
                                        @Param("fieldId") Long fieldId,
                                        @Param("taskTypes") List<String> taskTypes,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate);

    /**
     * 按 scheduleId 逻辑删除所有待提交的子任务（del_flag 0 → 1）。
     *
     * @return 受影响行数
     */
    default int logicalDeletePendingByScheduleId(Long scheduleId) {
        LambdaUpdateWrapper<SfSatelliteTask> uw = Wrappers.lambdaUpdate();
        uw.eq(SfSatelliteTask::getScheduleId, scheduleId);
        uw.eq(SfSatelliteTask::getStatus, SatelliteTaskStatus.PENDING_SUBMIT);
        uw.set(SfSatelliteTask::getDelFlag, "1");
        return update(null, uw);
    }

    /**
     * 更新任务状态（提交/失败回写等）。
     */
    default int updateTaskStatusByDkId(String dkId, Integer status, String message, Date lastSubmitTime, Integer submitCountOrNull) {
        LambdaUpdateWrapper<SfSatelliteTask> uw = Wrappers.lambdaUpdate();
        uw.eq(SfSatelliteTask::getDkId, dkId);
        uw.set(SfSatelliteTask::getStatus, status);
        uw.set(SfSatelliteTask::getMessage, message);
        uw.set(SfSatelliteTask::getLastSubmitTime, lastSubmitTime);
        if (submitCountOrNull != null) {
            uw.set(SfSatelliteTask::getSubmitCount, submitCountOrNull);
        }
        return update(null, uw);
    }

    /**
     * 回调更新任务状态与更新时间。
     */
    default int updateTaskStatusForCallbackByDkId(String dkId, Integer status, String message, Date updateTime) {
        LambdaUpdateWrapper<SfSatelliteTask> uw = Wrappers.lambdaUpdate();
        uw.eq(SfSatelliteTask::getDkId, dkId);
        uw.set(SfSatelliteTask::getStatus, status);
        uw.set(SfSatelliteTask::getMessage, message);
        uw.set(SfSatelliteTask::getUpdateTime, updateTime);
        return update(null, uw);
    }

    private static void likeTrimmedIfNotBlank(LambdaQueryWrapper<SfSatelliteTask> w,
        SFunction<SfSatelliteTask, String> column,
        String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        w.like(column, value.trim());
    }
}
