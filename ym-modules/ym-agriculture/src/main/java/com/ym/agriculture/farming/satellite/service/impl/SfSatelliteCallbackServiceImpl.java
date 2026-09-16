package com.ym.agriculture.farming.satellite.service.impl;

import cn.hutool.json.JSONUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskMapper;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskResultMapper;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteTaskStatus;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteCallbackDto;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTask;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTaskResult;
import com.ym.agriculture.farming.satellite.service.ISatelliteResultImageTransferService;
import com.ym.agriculture.farming.satellite.service.ISfSatelliteTaskService;
import com.ym.agriculture.farming.satellite.support.SatelliteTaskTypeNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 遥感结果回调落库（ym-gis 迁入）。一期产品约定：结果写入 {@code sf_satellite_task_result} 后仅持久化，不在前端展示；界面仅使用任务主表状态。
 * <p>
 * {@code task_type} 为 {@code rgb} 时，成功与否以回调 {@code object_key1} 非空为准；
 * {@code cloudcover} 为数值结果，成功与否以 {@code area} 存在为准；其余类型仍以上游 {@code success==true} 为准。
 * <p>
 * 回调含 {@code object_key1} 时，由 {@code object_key1} 重新生成 gis-oss URL 下载并转存本地 MinIO；
 * 转存失败则 {@code oss_url} 留空，仅保留上游 {@code object_key1}，由迁移任务补跑 MinIO 转存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfSatelliteCallbackServiceImpl {

    private final ISfSatelliteTaskService satelliteTaskService;
    private final SfSatelliteTaskMapper satelliteTaskMapper;
    private final SfSatelliteTaskResultMapper taskResultMapper;
    private final ISatelliteResultImageTransferService imageTransferService;

    @Transactional(rollbackFor = Exception.class)
    public void processCallback(SatelliteCallbackDto plotData) {
        if (plotData == null || StringUtils.isBlank(plotData.getDkId())) {
            log.warn("遥感回调数据为空或缺少 dk_id");
            return;
        }
        plotData.setTaskType(SatelliteTaskTypeNormalizer.normalize(plotData.getTaskType()));
        log.info("处理遥感回调 dkId={} taskType={} imageDate={}", plotData.getDkId(), plotData.getTaskType(), plotData.getImageDate());
        try {
            SfSatelliteTask task = satelliteTaskMapper.selectByDkId(plotData.getDkId());
            String tenantId = task != null ? task.getTenantId() : null;
            Long plantingBatchId = task != null ? task.getPlantingBatchId() : null;

            String upstreamObjectKey1 = plotData.getObjectKey1();
            String objectKey1 = upstreamObjectKey1;
            String ossUrl = null;
            if (StringUtils.isNotBlank(upstreamObjectKey1)) {
                Optional<ISatelliteResultImageTransferService.TransferResult> transferred =
                    imageTransferService.tryTransfer(tenantId, plotData.getDkId(), plotData.getTaskType(),
                        plotData.getImageDate(), upstreamObjectKey1, null);
                if (transferred.isPresent()) {
                    objectKey1 = transferred.get().localObjectKey();
                    ossUrl = transferred.get().localUrl();
                    log.info("遥感回调影像已转存 MinIO dkId={} localKey={}", plotData.getDkId(), objectKey1);
                } else {
                    log.warn("遥感回调影像转存 MinIO 失败，oss_url 留空待迁移任务补跑 dkId={} objectKey1={}",
                        plotData.getDkId(), upstreamObjectKey1);
                }
            }

            boolean effectiveSuccess = resolveCallbackSuccess(plotData);
            saveCallbackResult(plotData, objectKey1, ossUrl, effectiveSuccess, tenantId, plantingBatchId);
            updateTaskStatus(plotData, effectiveSuccess);
        } catch (Exception e) {
            log.error("遥感回调处理失败 dkId={}", plotData.getDkId(), e);
            throw new RuntimeException("处理回调数据失败: " + e.getMessage(), e);
        }
    }

    static boolean resolveCallbackSuccess(SatelliteCallbackDto plotData) {
        if (plotData == null) {
            return false;
        }
        if (isRgbTaskType(plotData.getTaskType())) {
            return StringUtils.isNotBlank(plotData.getObjectKey1());
        }
        if (isCloudcoverTaskType(plotData.getTaskType())) {
            return hasArea(plotData.getArea());
        }
        return Boolean.TRUE.equals(plotData.getSuccess());
    }

    private static boolean isRgbTaskType(String taskType) {
        if (StringUtils.isBlank(taskType)) {
            return false;
        }
        return "rgb".equalsIgnoreCase(taskType.trim());
    }

    private static boolean isCloudcoverTaskType(String taskType) {
        if (StringUtils.isBlank(taskType)) {
            return false;
        }
        return "cloudcover".equalsIgnoreCase(taskType.trim());
    }

    private static boolean hasArea(List<Double> area) {
        return area != null && !area.isEmpty();
    }

    private void saveCallbackResult(SatelliteCallbackDto plotData, String objectKey1, String ossUrl,
                                    boolean effectiveSuccess, String tenantId, Long plantingBatchId) {
        SfSatelliteTaskResult result = new SfSatelliteTaskResult();
        result.setTenantId(StringUtils.isNotBlank(tenantId) ? tenantId : "000000");
        result.setDkId(plotData.getDkId());
        result.setPlantingBatchId(plantingBatchId);
        result.setTaskType(plotData.getTaskType());
        if (plotData.getDkBounds() != null) {
            result.setDkBounds(JSONUtil.toJsonStr(plotData.getDkBounds()));
        }
        if (plotData.getBreakValue() != null) {
            result.setBreakValue(JSONUtil.toJsonStr(plotData.getBreakValue()));
        }
        if (plotData.getArea() != null) {
            result.setArea(JSONUtil.toJsonStr(plotData.getArea()));
        }
        result.setObjectKey1(objectKey1);
        result.setObjectKey2(plotData.getObjectKey2());
        result.setImagePixel(plotData.getImagePixel());
        result.setSuccess(effectiveSuccess);
        result.setImageDate(plotData.getImageDate());
        result.setBucket(plotData.getBucket());
        result.setOssUrl(ossUrl);
        taskResultMapper.insert(result);
        log.info("保存遥感回调结果成功 dkId={}", plotData.getDkId());
    }

    private void updateTaskStatus(SatelliteCallbackDto plotData, boolean effectiveSuccess) {
        SfSatelliteTask task = satelliteTaskMapper.selectByDkId(plotData.getDkId());
        if (task == null) {
            log.warn("遥感任务不存在 dkId={}", plotData.getDkId());
            return;
        }
        if (effectiveSuccess) {
            satelliteTaskService.updateTaskStatusForCallback(plotData.getDkId(), SatelliteTaskStatus.SUCCESS, "处理成功");
        } else {
            String message = "处理失败";
            if (plotData.getBucket() != null && plotData.getBucket().contains("error")) {
                message = plotData.getBucket();
            }
            satelliteTaskService.updateTaskStatusForCallback(plotData.getDkId(), SatelliteTaskStatus.FAILED, message);
        }
    }
}
