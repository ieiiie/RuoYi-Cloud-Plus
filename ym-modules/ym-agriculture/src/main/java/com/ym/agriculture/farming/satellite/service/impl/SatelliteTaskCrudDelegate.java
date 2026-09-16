package com.ym.agriculture.farming.satellite.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.crop.support.SfTaskCropSnapshot;
import com.ym.agriculture.farming.crop.support.SfTaskCropSnapshotFiller;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskMapper;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteTaskStatus;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteTaskCreateDto;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteSchedule;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 遥感任务写路径：创建、状态更新（供立即提交、手工批量补偿与 HTTP 回调编排调用）。
 */
@Slf4j
@RequiredArgsConstructor
final class SatelliteTaskCrudDelegate {

    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SfSatelliteTaskMapper taskMapper;

    private final SfPlantingBatchMapper plantingBatchMapper;

    private final SfTaskCropSnapshotFiller cropSnapshotFiller;

    String createTask(SatelliteTaskCreateDto dto) {
        try {
            String dkId = IdUtil.getSnowflakeNextIdStr();
            SfSatelliteTask task = new SfSatelliteTask();
            task.setDkId(dkId);
            task.setFieldId(dto.getFieldId());
            if (dto.getFieldId() != null) {
                task.setPlantingBatchId(plantingBatchMapper.selectActiveBatchIdByFieldId(dto.getFieldId()));
            }
            task.setDkGeom(JSONUtil.toJsonStr(dto.getDkGeom()));
            task.setCodeCroptype(dto.getCodeCroptype());
            if (toLocalDate(dto.getEndDate()).isBefore(toLocalDate(dto.getStartDate()))) {
                throw new ServiceException("endDate不能早于startDate");
            }
            task.setStartDate(toYmd(dto.getStartDate()));
            task.setEndDate(toYmd(dto.getEndDate()));
            task.setTaskType(dto.getTaskType());
            task.setPixelImage(dto.getPixelImage() != null ? dto.getPixelImage() : 10);
            task.setStatus(SatelliteTaskStatus.PENDING_SUBMIT);
            task.setSubmitCount(0);
            task.setMessage("任务已创建，等待提交");
            task.setDelFlag(SystemConstants.NORMAL);
            if (LoginHelper.getLoginUser() != null && StringUtils.isNotBlank(LoginHelper.getLoginUser().getTenantId())) {
                task.setTenantId(LoginHelper.getLoginUser().getTenantId());
            }
            SfTaskCropSnapshot snap = cropSnapshotFiller.resolve(
                task.getFieldId(), task.getPlantingBatchId(), task.getTenantId(), task.getCodeCroptype());
            task.setFieldName(snap.getFieldName());
            task.setSpeciesId(snap.getSpeciesId());
            task.setSpeciesName(snap.getSpeciesName());
            task.setVarietyId(snap.getVarietyId());
            task.setVarietyName(snap.getVarietyName());
            task.setPlantingBatchName(snap.getPlantingBatchName());
            taskMapper.insert(task);
            log.info("创建遥感任务成功 dkId={}", dkId);
            return dkId;
        } catch (Exception e) {
            log.error("创建遥感任务异常", e);
            throw new ServiceException("任务创建异常：" + e.getMessage());
        }
    }

    /**
     * 由周期计划创建子任务（定时任务上下文，无 LoginHelper）。
     *
     * @return dkId
     */
    String createTaskFromSchedule(SfSatelliteSchedule schedule, String taskType, LocalDate startDate, LocalDate endDate) {
        try {
            String dkId = IdUtil.getSnowflakeNextIdStr();
            SfSatelliteTask task = new SfSatelliteTask();
            task.setDkId(dkId);
            task.setScheduleId(schedule.getScheduleId());
            task.setFieldId(schedule.getFieldId());
            task.setPlantingBatchId(schedule.getPlantingBatchId());
            task.setDkGeom(schedule.getDkGeom());
            task.setCodeCroptype(schedule.getCodeCroptype());
            task.setStartDate(startDate.format(YMD));
            task.setEndDate(endDate.format(YMD));
            task.setTaskType(taskType);
            task.setPixelImage(schedule.getPixelImage() != null ? schedule.getPixelImage() : 10);
            task.setStatus(SatelliteTaskStatus.PENDING_SUBMIT);
            task.setSubmitCount(0);
            task.setMessage("由周期计划自动创建，等待提交");
            task.setDelFlag(SystemConstants.NORMAL);
            task.setTenantId(schedule.getTenantId());

            SfTaskCropSnapshot snap = cropSnapshotFiller.resolve(
                schedule.getFieldId(), schedule.getPlantingBatchId(),
                schedule.getTenantId(), schedule.getCodeCroptype());
            task.setFieldName(snap.getFieldName());
            task.setSpeciesId(snap.getSpeciesId());
            task.setSpeciesName(snap.getSpeciesName());
            task.setVarietyId(snap.getVarietyId());
            task.setVarietyName(snap.getVarietyName());
            task.setPlantingBatchName(snap.getPlantingBatchName());

            taskMapper.insert(task);
            log.info("周期计划创建遥感子任务成功 dkId={}, scheduleId={}", dkId, schedule.getScheduleId());
            return dkId;
        } catch (Exception e) {
            log.error("周期计划创建遥感子任务异常 scheduleId={}", schedule.getScheduleId(), e);
            throw new ServiceException("周期任务创建异常：" + e.getMessage());
        }
    }

    void updateTaskStatus(String dkId, Integer status, String message) {
        updateTaskStatus(dkId, status, message, null);
    }

    void updateTaskStatus(String dkId, Integer status, String message, Integer submitCount) {
        taskMapper.updateTaskStatusByDkId(dkId, status, message, new Date(), submitCount);
    }

    void updateTaskStatusForCallback(String dkId, Integer status, String message) {
        taskMapper.updateTaskStatusForCallbackByDkId(dkId, status, message, new Date());
    }

    private static java.time.LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(CN).toLocalDate();
    }

    private static String toYmd(Date d) {
        return toLocalDate(d).format(YMD);
    }
}
