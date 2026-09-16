package com.ym.agriculture.farming.satellite.support;

import cn.hutool.core.util.IdUtil;
import com.ym.common.core.constant.SystemConstants;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.support.SfTaskCropSnapshot;
import com.ym.agriculture.farming.crop.support.SfTaskCropSnapshotFiller;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteScheduleMapper;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskMapper;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteScheduleStatus;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteTaskStatus;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteSchedule;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 遥感周期计划执行：供定时任务与创建计划后立即执行共用。
 * <p>
 * {@code detectStart} 早于「今天」时，{@code next_run_date} 初值为 {@code detectStart}，会在一次处理中按周期
 * 自 {@code next_run_date} 起向前补齐直至超过今天（每轮锚定日为当时的 {@code next_run_date}，推进为
 * {@code cycleDay + cycleDays}），并受 {@link #MAX_CATCH_UP_CYCLES} 限制。
 * 检测窗口整体早于「今天」时，只要 {@code next_run_date} 仍不晚于 {@code detect_end}，仍会补齐子任务后再标记完成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SatelliteScheduleDueCycleRunner {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 单次 processDueCycle 内最多补齐的周期轮数，防止 detectStart 过早导致一次插入过多子任务 */
    private static final int MAX_CATCH_UP_CYCLES = 120;

    private final SfSatelliteScheduleMapper scheduleMapper;
    private final SfSatelliteTaskMapper taskMapper;
    private final SfCropSpeciesMapper speciesMapper;
    private final SfTaskCropSnapshotFiller cropSnapshotFiller;

    /**
     * @return 本次创建的 task 数（0 表示状态更新但无需创建子任务，-1 表示跳过）
     */
    public int processDueCycle(SfSatelliteSchedule schedule, LocalDate today) {
        int computedStatus = SatelliteScheduleStatus.computeStatus(
            schedule.getDetectStart(), schedule.getDetectEnd(), today);

        if (computedStatus == SatelliteScheduleStatus.COMPLETED) {
            if (!isNextRunPendingInDetectWindow(schedule)) {
                if (schedule.getScheduleStatus() != SatelliteScheduleStatus.COMPLETED) {
                    schedule.setScheduleStatus(SatelliteScheduleStatus.COMPLETED);
                    scheduleMapper.updateById(schedule);
                    log.info("遥感计划已完成 scheduleId={}", schedule.getScheduleId());
                }
                return 0;
            }
            // 日历上已过完检测窗，但仍有待展开的周期（如新建计划整窗在过去）：继续走 catch-up
        }

        if (computedStatus == SatelliteScheduleStatus.NOT_STARTED) {
            if (schedule.getScheduleStatus() != SatelliteScheduleStatus.NOT_STARTED) {
                schedule.setScheduleStatus(SatelliteScheduleStatus.NOT_STARTED);
                scheduleMapper.updateById(schedule);
            }
            return -1;
        }

        if (schedule.getScheduleStatus() == SatelliteScheduleStatus.NOT_STARTED) {
            schedule.setScheduleStatus(SatelliteScheduleStatus.IN_PROGRESS);
            scheduleMapper.updateById(schedule);
            log.info("遥感计划进入周期中 scheduleId={}", schedule.getScheduleId());
        }

        if (schedule.getScheduleStatus() != null
            && schedule.getScheduleStatus() == SatelliteScheduleStatus.PAUSED) {
            return -1;
        }

        if (schedule.getNextRunDate() != null && !schedule.getNextRunDate().isAfter(today)) {
            int totalCreated = 0;
            int guard = 0;
            while (schedule.getNextRunDate() != null
                && !schedule.getNextRunDate().isAfter(today)
                && guard < MAX_CATCH_UP_CYCLES) {
                guard++;
                if (Objects.equals(schedule.getScheduleStatus(), SatelliteScheduleStatus.COMPLETED)
                    && !isNextRunPendingInDetectWindow(schedule)) {
                    break;
                }
                LocalDate cycleDay = schedule.getNextRunDate();
                if (cycleDay.isBefore(schedule.getDetectStart())) {
                    cycleDay = schedule.getDetectStart();
                    schedule.setNextRunDate(cycleDay);
                }
                if (cycleDay.isAfter(schedule.getDetectEnd())) {
                    schedule.setScheduleStatus(SatelliteScheduleStatus.COMPLETED);
                    scheduleMapper.updateById(schedule);
                    break;
                }
                int created = createTasksForCurrentCycle(schedule, cycleDay);
                totalCreated += created;
                int cycleLen = cycleLengthDays(schedule);
                LocalDate nextRun = cycleDay.plusDays(cycleLen);
                schedule.setNextRunDate(nextRun);
                if (nextRun.isAfter(schedule.getDetectEnd())) {
                    schedule.setScheduleStatus(SatelliteScheduleStatus.COMPLETED);
                    log.info("遥感计划下次运行日期已超过检测结束日期，标记完成 scheduleId={}", schedule.getScheduleId());
                }
                scheduleMapper.updateById(schedule);
                // 历史窗创建时可能已是 COMPLETED，不能仅因状态为已完成就退出，须继续直到 next_run 超出 detectEnd
                if (Objects.equals(schedule.getScheduleStatus(), SatelliteScheduleStatus.COMPLETED)
                    && !isNextRunPendingInDetectWindow(schedule)) {
                    break;
                }
            }
            if (guard >= MAX_CATCH_UP_CYCLES) {
                log.warn("遥感计划单次 catch-up 达到上限 {} 轮 scheduleId={}", MAX_CATCH_UP_CYCLES, schedule.getScheduleId());
            }
            return totalCreated;
        }

        return -1;
    }

    /**
     * {@code next_run_date} 是否仍落在检测结束日及之前（尚有一轮或若干轮待展开为子任务）。
     */
    private static boolean isNextRunPendingInDetectWindow(SfSatelliteSchedule schedule) {
        LocalDate next = schedule.getNextRunDate();
        LocalDate detectEnd = schedule.getDetectEnd();
        return next != null && detectEnd != null && !next.isAfter(detectEnd);
    }

    /**
     * @param cycleDay 本周期锚定日开始日（通常为 {@code next_run_date}，可与「今天」相同或更早）
     */
    private static int cycleLengthDays(SfSatelliteSchedule schedule) {
        Integer d = schedule.getCycleDays();
        return d != null && d > 0 ? d : 1;
    }

    private int createTasksForCurrentCycle(SfSatelliteSchedule schedule, LocalDate cycleDay) {
        List<String> taskTypes = Arrays.stream(schedule.getTaskType().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

        int cycleLen = cycleLengthDays(schedule);
        LocalDate endDate = cycleDay.plusDays(cycleLen - 1);
        if (endDate.isAfter(schedule.getDetectEnd())) {
            endDate = schedule.getDetectEnd();
        }

        String codeCroptype = resolveCodeCroptype(schedule);

        SfTaskCropSnapshot snap = cropSnapshotFiller.resolve(
            schedule.getFieldId(), schedule.getPlantingBatchId(),
            schedule.getTenantId(), schedule.getCodeCroptype());

        int count = 0;
        for (String taskType : taskTypes) {
            SfSatelliteTask task = new SfSatelliteTask();
            task.setDkId(IdUtil.getSnowflakeNextIdStr());
            task.setScheduleId(schedule.getScheduleId());
            task.setFieldId(schedule.getFieldId());
            task.setPlantingBatchId(schedule.getPlantingBatchId());
            task.setDkGeom(schedule.getDkGeom());
            task.setCodeCroptype(codeCroptype);
            task.setStartDate(cycleDay.format(YMD));
            task.setEndDate(endDate.format(YMD));
            task.setTaskType(taskType);
            task.setPixelImage(schedule.getPixelImage() != null ? schedule.getPixelImage() : 10);
            task.setStatus(SatelliteTaskStatus.PENDING_SUBMIT);
            task.setSubmitCount(0);
            task.setMessage("由周期计划自动创建，等待提交");
            task.setDelFlag(SystemConstants.NORMAL);
            task.setTenantId(schedule.getTenantId());
            task.setFieldName(snap.getFieldName());
            task.setSpeciesId(snap.getSpeciesId());
            task.setSpeciesName(snap.getSpeciesName());
            task.setVarietyId(snap.getVarietyId());
            task.setVarietyName(snap.getVarietyName());
            task.setPlantingBatchName(snap.getPlantingBatchName());

            taskMapper.insert(task);
            count++;
            log.info("周期计划创建子任务 dkId={}, scheduleId={}, taskType={}, window=[{}, {}]",
                task.getDkId(), schedule.getScheduleId(), taskType, cycleDay, endDate);
        }
        return count;
    }

    private String resolveCodeCroptype(SfSatelliteSchedule schedule) {
        if (schedule.getSpeciesId() != null) {
            SfCropSpecies species = speciesMapper.selectById(schedule.getSpeciesId());
            if (species != null && species.getRemoteSensingCode() != null) {
                return String.valueOf(species.getRemoteSensingCode());
            }
        }
        return schedule.getCodeCroptype();
    }
}
