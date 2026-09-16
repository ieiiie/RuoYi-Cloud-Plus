package com.ym.agriculture.farming.uav.dao;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.uav.model.enums.WaylineJobStatusEnum;
import com.ym.agriculture.farming.uav.model.bo.SfUavFlightTaskQueryBo;
import com.ym.agriculture.farming.uav.model.entity.SfUavFlightTask;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code sf_uav_flight_task}。
 */
public interface SfUavFlightTaskMapper extends BaseMapper<SfUavFlightTask> {

    /**
     * 查询最近一段时间内需要刷新的任务（默认按创建时间过滤，只要存在飞控 jobId 即认为可刷新）。已经完成3  失败5 不进行同步
     */
    default List<SfUavFlightTask> selectRecentNeedRefresh(Date sinceTime) {
        return selectList(Wrappers.<SfUavFlightTask>lambdaQuery()
                .ne(SfUavFlightTask::getStatus, 3)
                .ne(SfUavFlightTask::getStatus, 5)
            .ge(SfUavFlightTask::getCreateTime, sinceTime)
            .isNotNull(SfUavFlightTask::getUavJobId));
    }

    /**
     * SUCCESS 且上传进度可能未落齐：供轮询刷新 {@code media_count}/{@code uploaded_count}/{@code uploading}。
     */
    default List<SfUavFlightTask> selectSuccessNeedUploadProgressRefresh(Date sinceTime) {
        return selectList(Wrappers.<SfUavFlightTask>lambdaQuery()
            .eq(SfUavFlightTask::getStatus, WaylineJobStatusEnum.SUCCESS.getVal())
            .ge(SfUavFlightTask::getCreateTime, sinceTime)
            .isNotNull(SfUavFlightTask::getUavJobId)
            .ne(SfUavFlightTask::getUavJobId, "")
            .isNotNull(SfUavFlightTask::getWorkspaceId)
            .ne(SfUavFlightTask::getWorkspaceId, "")
            .and(w -> w.eq(SfUavFlightTask::getUploading, 1)
                .or().isNull(SfUavFlightTask::getMediaCount)
                .or().isNull(SfUavFlightTask::getUploadedCount)
                .or().apply("media_count > 0 AND uploaded_count < media_count")));
    }

    /**
     * 大屏最近已完成航拍：SUCCESS 且含飞控 jobId，按完成时间倒序取 1 条。
     */
    default SfUavFlightTask selectLatestSuccessForBigscreen() {
        return selectOne(Wrappers.<SfUavFlightTask>lambdaQuery()
            .eq(SfUavFlightTask::getStatus, WaylineJobStatusEnum.SUCCESS.getVal())
            .isNotNull(SfUavFlightTask::getUavJobId)
            .ne(SfUavFlightTask::getUavJobId, "")
            .orderByDesc(SfUavFlightTask::getCompletedTime)
            .orderByDesc(SfUavFlightTask::getEndTime)
            .orderByDesc(SfUavFlightTask::getCreateTime)
            .last("LIMIT 1"));
    }

    /**
     * 本地任务分页条件；{@code allowedFieldIds} 为 {@code null} 表示超管不限制。
     */
    default LambdaQueryWrapper<SfUavFlightTask> buildLocalPageWrapper(SfUavFlightTaskQueryBo bo, List<Long> allowedFieldIds) {
        LambdaQueryWrapper<SfUavFlightTask> w = Wrappers.lambdaQuery();
        if (allowedFieldIds != null) {
            w.in(SfUavFlightTask::getFieldId, allowedFieldIds);
        }
        if (bo == null) {
            return w;
        }
        if (ObjectUtil.isNotNull(bo.getFieldId())) {
            w.eq(SfUavFlightTask::getFieldId, bo.getFieldId());
        }
        if (StringUtils.isNotBlank(bo.getFieldName())) {
            w.like(SfUavFlightTask::getFieldName, bo.getFieldName().trim());
        }
        if (StringUtils.isNotBlank(bo.getSpeciesName())) {
            w.like(SfUavFlightTask::getSpeciesName, bo.getSpeciesName().trim());
        }
        if (StringUtils.isNotBlank(bo.getVarietyName())) {
            w.like(SfUavFlightTask::getVarietyName, bo.getVarietyName().trim());
        }
        if (StringUtils.isNotBlank(bo.getPlantingBatchName())) {
            w.like(SfUavFlightTask::getPlantingBatchName, bo.getPlantingBatchName().trim());
        }
        if (ObjectUtil.isNotNull(bo.getStatus())) {
            w.eq(SfUavFlightTask::getStatus, bo.getStatus());
        }
        if (StringUtils.isNotBlank(bo.getJobId())) {
            w.like(SfUavFlightTask::getJobId, bo.getJobId().trim());
        }
        if (StringUtils.isNotBlank(bo.getUavJobId())) {
            w.like(SfUavFlightTask::getUavJobId, bo.getUavJobId().trim());
        }
        if (StringUtils.isNotBlank(bo.getTaskName())) {
            w.like(SfUavFlightTask::getTaskName, bo.getTaskName().trim());
        }
        if (bo.getCreateTimeBegin() != null && bo.getCreateTimeEnd() != null) {
            w.between(SfUavFlightTask::getCreateTime, bo.getCreateTimeBegin(), bo.getCreateTimeEnd());
        } else if (bo.getCreateTimeBegin() != null) {
            w.ge(SfUavFlightTask::getCreateTime, bo.getCreateTimeBegin());
        } else if (bo.getCreateTimeEnd() != null) {
            w.le(SfUavFlightTask::getCreateTime, bo.getCreateTimeEnd());
        }
        return w;
    }

    /**
     * 批量查询存在未执行任务（status 非 3=完成、5=失败）的种植批次 ID 集合。
     */
    default Set<Long> selectBatchIdsWithPendingTask(Collection<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return Set.of();
        }
        return selectList(Wrappers.<SfUavFlightTask>lambdaQuery()
                .select(SfUavFlightTask::getPlantingBatchId)
                .in(SfUavFlightTask::getPlantingBatchId, batchIds)
                .and(w -> w.isNull(SfUavFlightTask::getStatus)
                    .or().notIn(SfUavFlightTask::getStatus,
                        WaylineJobStatusEnum.SUCCESS.getVal(),
                        WaylineJobStatusEnum.CANCEL.getVal(),
                        WaylineJobStatusEnum.FAILED.getVal(),
                        WaylineJobStatusEnum.UNKNOWN.getVal())))
            .stream()
            .map(SfUavFlightTask::getPlantingBatchId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * 按种植批次统计未执行的飞行任务数量。
     */
    default long countPendingByPlantingBatchId(Long plantingBatchId) {
        if (plantingBatchId == null) {
            return 0L;
        }
        return selectCount(Wrappers.<SfUavFlightTask>lambdaQuery()
            .eq(SfUavFlightTask::getPlantingBatchId, plantingBatchId)
            .and(w -> w.isNull(SfUavFlightTask::getStatus)
                .or().notIn(SfUavFlightTask::getStatus,
                    WaylineJobStatusEnum.SUCCESS.getVal(),
                    WaylineJobStatusEnum.CANCEL.getVal(),
                    WaylineJobStatusEnum.FAILED.getVal(),
                    WaylineJobStatusEnum.UNKNOWN.getVal())));
    }

    /**
     * 按地块统计未完成飞行任务数量。
     */
    default long countPendingByFieldId(Long fieldId) {
        if (fieldId == null) {
            return 0L;
        }
        return selectCount(Wrappers.<SfUavFlightTask>lambdaQuery()
            .eq(SfUavFlightTask::getFieldId, fieldId)
            .and(w -> w.isNull(SfUavFlightTask::getStatus)
                .or().notIn(SfUavFlightTask::getStatus,
                    WaylineJobStatusEnum.SUCCESS.getVal(),
                    WaylineJobStatusEnum.CANCEL.getVal(),
                    WaylineJobStatusEnum.FAILED.getVal(),
                    WaylineJobStatusEnum.UNKNOWN.getVal())));
    }

    /**
     * 将地块下未完成飞行任务标记为取消。
     */
    default int cancelPendingByFieldId(Long fieldId) {
        if (fieldId == null) {
            return 0;
        }
        return update(null, Wrappers.<SfUavFlightTask>lambdaUpdate()
            .eq(SfUavFlightTask::getFieldId, fieldId)
            .and(w -> w.isNull(SfUavFlightTask::getStatus)
                .or().notIn(SfUavFlightTask::getStatus,
                    WaylineJobStatusEnum.SUCCESS.getVal(),
                    WaylineJobStatusEnum.CANCEL.getVal(),
                    WaylineJobStatusEnum.FAILED.getVal(),
                    WaylineJobStatusEnum.UNKNOWN.getVal()))
            .set(SfUavFlightTask::getStatus, WaylineJobStatusEnum.CANCEL.getVal()));
    }

    default SfUavFlightTask selectOneByFieldIdAndJobId(Long fieldId, String jobId) {
        if (jobId == null) {
            return null;
        }
        String j = jobId.trim();
        if (j.isEmpty()) {
            return null;
        }
        return selectOne(Wrappers.<SfUavFlightTask>lambdaQuery()
            .eq(SfUavFlightTask::getFieldId, fieldId)
            .eq(SfUavFlightTask::getJobId, j)
            .last("LIMIT 1"));
    }
}
