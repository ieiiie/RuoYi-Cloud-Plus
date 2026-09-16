package com.ym.agriculture.farming.satellite.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskResultMapper;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTaskResult;
import com.ym.agriculture.farming.satellite.service.ISatelliteResultImageTransferService;
import com.ym.agriculture.farming.satellite.service.ISatelliteResultImageTransferService.MigrateOutcome;
import com.ym.agriculture.farming.satellite.service.ISatelliteResultImageTransferService.RepairOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 供 Snail Job 手工调用：批量将 {@code sf_satellite_task_result} 中上游 gis-oss 影像转存本地 MinIO。
 * <p>
 * 调用目标示例：{@code sfSatelliteResultImageMigrateTask.migratePendingResults(50)}。
 */
@Slf4j
@Component("sfSatelliteResultImageMigrateTask")
@RequiredArgsConstructor
@JobExecutor(name = "sfSatelliteImageMigrateJob", method = "migratePendingResults")
public class SfSatelliteResultImageMigrateTask {

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final SfSatelliteTaskResultMapper taskResultMapper;
    private final ISatelliteResultImageTransferService imageTransferService;
    private final DomainJobExecutorSupport executorSupport;

    /**
     * 无参批量迁移，默认每批 50 条。
     */
    public void migratePendingResults(JobArgs args) {
        executorSupport.executeGlobal(() -> migratePendingResults(DEFAULT_BATCH_SIZE));
    }

    /**
     * 批量迁移待转存记录；可重复执行，已转存（{@code object_key1} 以 {@code ymsf/} 或 legacy {@code satellite/} 开头）自动跳过。
     *
     * @param batchSize 单批最大处理条数，1~500
     */
    public void migratePendingResults(int batchSize) {
        int limit = normalizeBatchSize(batchSize);
        List<SfSatelliteTaskResult> pending = taskResultMapper.selectPendingImageMigrate(limit);
            if (pending.isEmpty()) {
                log.info("遥感影像历史迁移：无待处理记录");
                return;
            }
            int migrated = 0;
            int urlRefreshed = 0;
            int failed = 0;
            int skipped = 0;
            for (SfSatelliteTaskResult result : pending) {
                switch (imageTransferService.migrateHistoricalResult(result)) {
                    case MIGRATED -> migrated++;
                    case URL_REFRESHED -> urlRefreshed++;
                    case FAILED -> failed++;
                    case SKIPPED -> skipped++;
                }
            }
            log.info("遥感影像历史迁移完成 batchSize={} fetched={} migrated={} urlRefreshed={} failed={} skipped={}",
                limit, pending.size(), migrated, urlRefreshed, failed, skipped);
    }

    /**
     * 无参批量预览修复，默认每批 50 条。
     */
    public void repairPreviewableResults() {
        repairPreviewableResults(DEFAULT_BATCH_SIZE);
    }

    /**
     * 修复 legacy {@code satellite/...} 转存记录的浏览器预览问题。
     * <p>
     * 调用目标示例：{@code sfSatelliteResultImageMigrateTask.repairPreviewableResults(50)}。
     *
     * @param batchSize 单批最大处理条数，1~500
     */
    public void repairPreviewableResults(int batchSize) {
        int limit = normalizeBatchSize(batchSize);
        List<SfSatelliteTaskResult> pending = taskResultMapper.selectPendingPreviewRepair(limit);
            if (pending.isEmpty()) {
                log.info("遥感影像预览修复：无待处理记录");
                return;
            }
            int repaired = 0;
            int failed = 0;
            int skipped = 0;
            for (SfSatelliteTaskResult result : pending) {
                switch (imageTransferService.repairPreviewableResult(result)) {
                    case REPAIRED -> repaired++;
                    case FAILED -> failed++;
                    case SKIPPED -> skipped++;
                }
            }
            log.info("遥感影像预览修复完成 batchSize={} fetched={} repaired={} failed={} skipped={}",
                limit, pending.size(), repaired, failed, skipped);
    }

    private static int normalizeBatchSize(int batchSize) {
        if (batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }
        return Math.min(batchSize, 500);
    }
}
