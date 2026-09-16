package com.ym.agriculture.farming.satellite.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTaskResult;
import com.ym.agriculture.farming.satellite.support.SatelliteResultOssUrlSupport;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 遥感结果 Mapper。
 */
@Mapper
public interface SfSatelliteTaskResultMapper extends BaseMapper<SfSatelliteTaskResult> {

    /**
     * 按 dkId 查询结果列表，按影像日期倒序。
     */
    default List<SfSatelliteTaskResult> selectByDkIdOrderByImageDateDesc(String dkId) {
        return selectList(
            Wrappers.<SfSatelliteTaskResult>lambdaQuery()
                .eq(SfSatelliteTaskResult::getDkId, dkId)
                .orderByDesc(SfSatelliteTaskResult::getImageDate));
    }

    /**
     * 根据多个 dkId 批量查询回调结果，按影像日期倒序。
     */
    default List<SfSatelliteTaskResult> selectByDkIds(Collection<String> dkIds) {
        if (dkIds == null || dkIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(
            Wrappers.<SfSatelliteTaskResult>lambdaQuery()
                .in(SfSatelliteTaskResult::getDkId, dkIds)
                .orderByDesc(SfSatelliteTaskResult::getImageDate));
    }

    /**
     * 按种植批次 ID 查询最近 N 条遥感结果，按影像日期倒序。
     */
    default List<SfSatelliteTaskResult> selectRecentByPlantingBatchId(Long plantingBatchId, int limit) {
        if (plantingBatchId == null) {
            return Collections.emptyList();
        }
        return selectList(
            Wrappers.<SfSatelliteTaskResult>lambdaQuery()
                .eq(SfSatelliteTaskResult::getPlantingBatchId, plantingBatchId)
                .orderByDesc(SfSatelliteTaskResult::getImageDate)
                .last("LIMIT " + limit));
    }

    /**
     * 查询待转存 MinIO 的遥感结果（object_key1 仍为上游 gis 键）。
     */
    default List<SfSatelliteTaskResult> selectPendingImageMigrate(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return selectList(
            Wrappers.<SfSatelliteTaskResult>lambdaQuery()
                .isNotNull(SfSatelliteTaskResult::getObjectKey1)
                .ne(SfSatelliteTaskResult::getObjectKey1, "")
                .notLikeRight(SfSatelliteTaskResult::getObjectKey1, SatelliteResultOssUrlSupport.LOCAL_MINIO_OBJECT_KEY_PREFIX)
                .notLikeRight(SfSatelliteTaskResult::getObjectKey1, SatelliteResultOssUrlSupport.LOCAL_OBJECT_KEY_PREFIX)
                .eq(SfSatelliteTaskResult::getSuccess, Boolean.TRUE)
                .orderByAsc(SfSatelliteTaskResult::getResultId)
                .last("LIMIT " + safeLimit));
    }

    /**
     * 查询待 Web 预览修复的记录（legacy {@code object_key1} 以 {@code satellite/} 开头且 {@code oss_url} 为本地 MinIO）。
     */
    default List<SfSatelliteTaskResult> selectPendingPreviewRepair(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return selectList(
            Wrappers.<SfSatelliteTaskResult>lambdaQuery()
                .likeRight(SfSatelliteTaskResult::getObjectKey1, SatelliteResultOssUrlSupport.LOCAL_OBJECT_KEY_PREFIX)
                .isNotNull(SfSatelliteTaskResult::getOssUrl)
                .ne(SfSatelliteTaskResult::getOssUrl, "")
                .notLike(SfSatelliteTaskResult::getOssUrl, "%aliyuncs.com%")
                .eq(SfSatelliteTaskResult::getSuccess, Boolean.TRUE)
                .orderByAsc(SfSatelliteTaskResult::getResultId)
                .last("LIMIT " + safeLimit));
    }
}
