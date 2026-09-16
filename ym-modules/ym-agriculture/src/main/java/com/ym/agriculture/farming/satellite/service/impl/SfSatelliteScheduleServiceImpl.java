package com.ym.agriculture.farming.satellite.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.agriculture.farming.crop.support.SfTaskCropSnapshot;
import com.ym.agriculture.farming.crop.support.SfTaskCropSnapshotFiller;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteScheduleMapper;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskMapper;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskResultMapper;
import com.ym.agriculture.farming.satellite.model.bo.SatelliteScheduleQueryBo;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteScheduleStatus;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteTaskStatus;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteScheduleCreateDto;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteScheduleCreateResult;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteSchedule;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTask;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTaskResult;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteScheduleDetailVo;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteScheduleVo;
import com.ym.agriculture.farming.satellite.model.vo.ScheduleTaskExecutionVo;
import com.ym.agriculture.farming.satellite.service.ISfSatelliteScheduleService;
import com.ym.agriculture.farming.satellite.service.ISfSatelliteTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 卫星遥感监测计划服务。
 * <p>
 * 当前逻辑不再按本地周期拆分子任务，而是在创建计划时按整段监测时间一次性创建任务，
 * 后续由卫星服务持续回调历史与未来数据。
 */
@Slf4j
@Service
public class SfSatelliteScheduleServiceImpl
    implements ISfSatelliteScheduleService {

    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");

    private final SfSatelliteScheduleMapper scheduleMapper;
    private final SfSatelliteTaskMapper taskMapper;
    private final SfSatelliteTaskResultMapper taskResultMapper;
    private final SfTaskCropSnapshotFiller cropSnapshotFiller;
    private final SatelliteTaskTypeDict taskTypeDict;
    private final SfFieldMapper fieldMapper;
    private final SfPlantingBatchMapper plantingBatchMapper;
    private final SatelliteTaskCrudDelegate taskCrudDelegate;
    private final ISfSatelliteTaskService satelliteTaskService;

    public SfSatelliteScheduleServiceImpl(
        SfSatelliteScheduleMapper scheduleMapper,
        SfSatelliteTaskMapper taskMapper,
        SfSatelliteTaskResultMapper taskResultMapper,
        SfTaskCropSnapshotFiller cropSnapshotFiller,
        SatelliteTaskTypeDict taskTypeDict,
        SfFieldMapper fieldMapper,
        SfPlantingBatchMapper plantingBatchMapper,
        ISfSatelliteTaskService satelliteTaskService
    ) {
        this.scheduleMapper = scheduleMapper;
        this.taskMapper = taskMapper;
        this.taskResultMapper = taskResultMapper;
        this.cropSnapshotFiller = cropSnapshotFiller;
        this.taskTypeDict = taskTypeDict;
        this.fieldMapper = fieldMapper;
        this.plantingBatchMapper = plantingBatchMapper;
        this.taskCrudDelegate = new SatelliteTaskCrudDelegate(taskMapper, plantingBatchMapper, cropSnapshotFiller);
        this.satelliteTaskService = satelliteTaskService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> createSchedule(SatelliteScheduleCreateDto dto) {
        SatelliteScheduleCreateResult result = createScheduleInternal(dto);
        return List.of(result.getScheduleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SatelliteScheduleCreateResult createScheduleWithTasks(SatelliteScheduleCreateDto dto) {
        return createScheduleInternal(dto);
    }

    private SatelliteScheduleCreateResult createScheduleInternal(SatelliteScheduleCreateDto dto) {
        LocalDate start = toLocalDate(dto.getDetectStart());
        LocalDate end = toLocalDate(dto.getDetectEnd());
        if (end.isBefore(start)) {
            throw new ServiceException("detectEnd不能早于detectStart");
        }

        List<String> normalizedTaskTypes = normalizeTaskTypes(dto.getTaskTypes());
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId) && LoginHelper.getLoginUser() != null) {
            tenantId = LoginHelper.getLoginUser().getTenantId();
        }
        String taskType = String.join(",", normalizedTaskTypes);
        assertFieldSupportsSatellite(resolveScheduleFieldId(dto));

        SfTaskCropSnapshot snap = cropSnapshotFiller.resolve(
            dto.getFieldId(), dto.getPlantingBatchId(), tenantId, null);
        Integer remoteSensingCode = snap.getRemoteSensingCode();
        String codeCroptype = String.valueOf(remoteSensingCode != null ? remoteSensingCode : 0);

        SfSatelliteSchedule schedule = new SfSatelliteSchedule();
        schedule.setPlantingBatchId(dto.getPlantingBatchId());
        schedule.setFieldId(dto.getFieldId());
        schedule.setDkGeom(JSONUtil.toJsonStr(dto.getDkGeom()));
        schedule.setCodeCroptype(codeCroptype);
        schedule.setTaskType(taskType);
        schedule.setPixelImage(dto.getPixelImage() != null ? dto.getPixelImage() : 10);
        schedule.setDetectStart(start);
        schedule.setDetectEnd(end);
        // Legacy table columns are still NOT NULL, keep compatibility values only.
        schedule.setCycleDays(1);
        schedule.setNextRunDate(start);
        schedule.setScheduleStatus(SatelliteScheduleStatus.computeStatus(start, end, LocalDate.now(CN)));
        if (tenantId != null) {
            schedule.setTenantId(tenantId);
        }
        schedule.setFieldName(resolveFieldNameOrFromDb(snap.getFieldName(), dto.getFieldId(), dto.getPlantingBatchId()));
        schedule.setSpeciesId(snap.getSpeciesId());
        schedule.setSpeciesName(snap.getSpeciesName());
        schedule.setVarietyId(snap.getVarietyId());
        schedule.setVarietyName(snap.getVarietyName());
        schedule.setPlantingBatchName(snap.getPlantingBatchName());

        scheduleMapper.insert(schedule);
        List<String> createdDkIds = createMonitoringTasks(schedule, normalizedTaskTypes, start, end);
        submitTasksAfterCommit(createdDkIds);

        log.info("创建遥感监测计划成功 scheduleId={}, taskTypes={}, range=[{}, {}]",
            schedule.getScheduleId(), taskType, start, end);
        return SatelliteScheduleCreateResult.builder()
            .scheduleId(schedule.getScheduleId())
            .dkIds(createdDkIds)
            .build();
    }

    @Override
    public SatelliteScheduleDetailVo getScheduleDetail(Long scheduleId) {
        SfSatelliteSchedule entity = scheduleMapper.selectById(scheduleId);
        if (entity == null) {
            throw new ServiceException("计划不存在");
        }
        return toDetailVo(entity);
    }

    @Override
    public List<SatelliteScheduleVo> listSchedules(Long plantingBatchId, Long fieldId) {
        var wrapper = com.baomidou.mybatisplus.core.toolkit.Wrappers.<SfSatelliteSchedule>lambdaQuery()
            .eq(plantingBatchId != null, SfSatelliteSchedule::getPlantingBatchId, plantingBatchId)
            .eq(fieldId != null, SfSatelliteSchedule::getFieldId, fieldId)
            .orderByDesc(SfSatelliteSchedule::getCreateTime);
        return scheduleMapper.selectList(wrapper).stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }

    @Override
    public PageResult<SatelliteScheduleVo> pageSchedules(SatelliteScheduleQueryBo bo, PageQuery pageQuery) {
        Page<SfSatelliteSchedule> page = pageQuery.build();
        Page<SfSatelliteSchedule> result = scheduleMapper.selectSchedulePage(page, bo);
        List<SatelliteScheduleVo> rows = result.getRecords().stream()
            .map(this::toVo)
            .collect(Collectors.toList());
        fillResultCounts(rows);
        Page<SatelliteScheduleVo> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(rows);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pauseSchedule(Long scheduleId) {
        SfSatelliteSchedule entity = scheduleMapper.selectById(scheduleId);
        if (entity == null) {
            throw new ServiceException("计划不存在");
        }
        int currentStatus = computeRuntimeStatus(entity);
        if (currentStatus != SatelliteScheduleStatus.IN_PROGRESS) {
            throw new ServiceException("只有监测中的计划才能暂停");
        }
        if (taskMapper.existsSubmittedTaskByScheduleId(scheduleId)) {
            throw new ServiceException("监测任务已提交至卫星服务，当前不支持暂停");
        }
        entity.setScheduleStatus(SatelliteScheduleStatus.PAUSED);
        scheduleMapper.updateById(entity);
        log.info("暂停遥感监测计划 scheduleId={}", scheduleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeSchedule(Long scheduleId) {
        SfSatelliteSchedule entity = scheduleMapper.selectById(scheduleId);
        if (entity == null) {
            throw new ServiceException("计划不存在");
        }
        if (entity.getScheduleStatus() != SatelliteScheduleStatus.PAUSED) {
            throw new ServiceException("只有已暂停的计划才能恢复");
        }
        LocalDate today = LocalDate.now(CN);
        if (today.isAfter(entity.getDetectEnd())) {
            throw new ServiceException("当前日期已超过检测结束时间，无法恢复");
        }
        entity.setScheduleStatus(SatelliteScheduleStatus.computeStatus(entity.getDetectStart(), entity.getDetectEnd(), today));
        scheduleMapper.updateById(entity);
        log.info("恢复遥感监测计划 scheduleId={}", scheduleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long scheduleId) {
        SfSatelliteSchedule entity = scheduleMapper.selectById(scheduleId);
        if (entity == null) {
            throw new ServiceException("计划不存在或已删除");
        }
        if (taskMapper.existsSubmittedTaskByScheduleId(scheduleId)) {
            throw new ServiceException("监测任务已提交至卫星服务，无法删除");
        }
        int currentStatus = computeRuntimeStatus(entity);
        if (currentStatus != SatelliteScheduleStatus.NOT_STARTED && currentStatus != SatelliteScheduleStatus.PAUSED) {
            throw new ServiceException("只有未开始或已暂停且未提交的计划才能删除");
        }
        int rows = scheduleMapper.deleteById(scheduleId);
        if (rows == 0) {
            throw new ServiceException("计划不存在或已删除");
        }
        int deleted = taskMapper.logicalDeletePendingByScheduleId(scheduleId);
        log.info("删除遥感监测计划 scheduleId={}, 删除待提交任务 {} 条", scheduleId, deleted);
    }

    @Override
    public void executeScheduledTasks() {
        log.info("executeScheduledTasks 已停用：当前改为创建计划时直接生成整段监测任务");
    }

    private String resolveFieldNameOrFromDb(String storedOrSnapFieldName, Long fieldId, Long plantingBatchId) {
        if (StringUtils.isNotBlank(storedOrSnapFieldName)) {
            return storedOrSnapFieldName;
        }
        SfPlantingBatch batch = null;
        if (plantingBatchId != null) {
            SfPlantingBatch b = plantingBatchMapper.selectById(plantingBatchId);
            if (b != null && SystemConstants.NORMAL.equals(b.getDelFlag())) {
                batch = b;
            }
        }
        Long first = fieldId != null ? fieldId : (batch != null ? batch.getFieldId() : null);
        SfField field = selectNormalFieldById(first);
        if (field != null) {
            return field.getFieldName();
        }
        if (batch != null && batch.getFieldId() != null && !Objects.equals(first, batch.getFieldId())) {
            field = selectNormalFieldById(batch.getFieldId());
            if (field != null) {
                return field.getFieldName();
            }
        }
        return null;
    }

    private Long resolveScheduleFieldId(SatelliteScheduleCreateDto dto) {
        if (dto.getFieldId() != null) {
            return dto.getFieldId();
        }
        SfPlantingBatch batch = plantingBatchMapper.selectById(dto.getPlantingBatchId());
        return batch != null ? batch.getFieldId() : null;
    }

    private void assertFieldSupportsSatellite(Long fieldId) {
        SfField field = selectNormalFieldById(fieldId);
        if (field == null) {
            return;
        }
        if (FieldType.isGreenhouse(FieldType.normalizeOrDefault(field.getFieldType()))) {
            throw new ServiceException("大棚顶棚遮挡地表，卫星遥感数据不适用");
        }
    }

    private SfField selectNormalFieldById(Long id) {
        if (id == null) {
            return null;
        }
        SfField field = fieldMapper.selectById(id);
        if (field != null && SystemConstants.NORMAL.equals(field.getDelFlag())) {
            return field;
        }
        return null;
    }

    private SatelliteScheduleVo toVo(SfSatelliteSchedule entity) {
        SatelliteScheduleVo vo = new SatelliteScheduleVo();
        fillBaseVoFields(vo, entity);
        return vo;
    }

    private void fillResultCounts(List<SatelliteScheduleVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (SatelliteScheduleVo row : rows) {
            row.setTaskCount(0);
            row.setImageDataCount(0);
            row.setResultSuccessCount(0);
            row.setResultFailCount(0);
        }
        List<Long> scheduleIds = rows.stream()
            .map(SatelliteScheduleVo::getScheduleId)
            .filter(Objects::nonNull)
            .toList();
        if (scheduleIds.isEmpty()) {
            return;
        }
        List<SfSatelliteTask> tasks = taskMapper.selectList(
            com.baomidou.mybatisplus.core.toolkit.Wrappers.<SfSatelliteTask>lambdaQuery()
                .select(SfSatelliteTask::getScheduleId, SfSatelliteTask::getDkId, SfSatelliteTask::getTaskType)
                .in(SfSatelliteTask::getScheduleId, scheduleIds));
        Map<Long, Long> taskCounts = tasks.stream()
            .filter(task -> task.getScheduleId() != null)
            .collect(Collectors.groupingBy(SfSatelliteTask::getScheduleId, Collectors.counting()));
        Map<Long, List<SfSatelliteTask>> tasksByScheduleId = tasks.stream()
            .filter(task -> task.getScheduleId() != null)
            .collect(Collectors.groupingBy(SfSatelliteTask::getScheduleId));
        Map<String, Long> scheduleIdByDkId = tasks.stream()
            .filter(t -> StringUtils.isNotBlank(t.getDkId()) && t.getScheduleId() != null)
            .collect(Collectors.toMap(SfSatelliteTask::getDkId, SfSatelliteTask::getScheduleId, (a, b) -> a));
        Map<Long, ResultCounter> counters = new java.util.HashMap<>();
        if (!scheduleIdByDkId.isEmpty()) {
            for (SfSatelliteTaskResult result : taskResultMapper.selectByDkIds(scheduleIdByDkId.keySet())) {
                Long scheduleId = scheduleIdByDkId.get(result.getDkId());
                if (scheduleId == null) {
                    continue;
                }
                ResultCounter counter = counters.computeIfAbsent(scheduleId, k -> new ResultCounter());
                counter.total++;
                if (Boolean.TRUE.equals(result.getSuccess())) {
                    counter.success++;
                } else if (Boolean.FALSE.equals(result.getSuccess())) {
                    counter.fail++;
                }
            }
        }
        for (SatelliteScheduleVo row : rows) {
            row.setTaskCount(Math.toIntExact(taskCounts.getOrDefault(row.getScheduleId(), 0L)));
            fillActiveTaskTypes(row, tasksByScheduleId.getOrDefault(row.getScheduleId(), List.of()));
            ResultCounter counter = counters.get(row.getScheduleId());
            if (counter != null) {
                row.setImageDataCount(counter.total);
                row.setResultSuccessCount(counter.success);
                row.setResultFailCount(counter.fail);
            }
        }
    }

    /**
     * 计划配置保留创建时的类型快照，但列表和详情展示当前仍有效的子任务类型。
     */
    private void fillActiveTaskTypes(SatelliteScheduleVo vo, List<SfSatelliteTask> tasks) {
        Set<String> activeTaskTypes = tasks.stream()
            .map(SfSatelliteTask::getTaskType)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> effectiveTaskTypes = SatelliteTaskTypeDict.splitTaskTypes(vo.getTaskType()).stream()
            .filter(activeTaskTypes::contains)
            .collect(Collectors.toCollection(ArrayList::new));
        activeTaskTypes.stream()
            .filter(taskType -> !effectiveTaskTypes.contains(taskType))
            .forEach(effectiveTaskTypes::add);

        String taskType = String.join(",", effectiveTaskTypes);
        vo.setTaskType(taskType);
        vo.setTaskTypeList(effectiveTaskTypes);
        vo.setTaskTypeCount(effectiveTaskTypes.size());
        vo.setTaskTypeDisplay(taskTypeDict.describeMultiTaskTypes(taskType));
        vo.setSubmitCountDisplay(vo.getSubmitCount() + "*" + effectiveTaskTypes.size());
    }

    private void fillBaseVoFields(SatelliteScheduleVo vo, SfSatelliteSchedule entity) {
        vo.setScheduleId(entity.getScheduleId());
        vo.setPlantingBatchId(entity.getPlantingBatchId());
        vo.setPlantingBatchName(entity.getPlantingBatchName());
        vo.setFieldId(entity.getFieldId());
        vo.setFieldName(resolveFieldNameOrFromDb(entity.getFieldName(), entity.getFieldId(), entity.getPlantingBatchId()));
        vo.setSpeciesId(entity.getSpeciesId());
        vo.setSpeciesName(entity.getSpeciesName());
        vo.setVarietyId(entity.getVarietyId());
        vo.setVarietyName(entity.getVarietyName());
        vo.setCodeCroptype(entity.getCodeCroptype());
        vo.setTaskType(entity.getTaskType());
        vo.setPixelImage(entity.getPixelImage());
        vo.setDetectStart(entity.getDetectStart());
        vo.setDetectEnd(entity.getDetectEnd());
        vo.setCycleDays(null);
        vo.setNextRunDate(null);
        vo.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(entity.getCreateTime()));

        List<String> typeList = SatelliteTaskTypeDict.splitTaskTypes(entity.getTaskType());
        vo.setTaskTypeList(typeList);
        vo.setTaskTypeCount(typeList.size());
        vo.setTaskTypeDisplay(taskTypeDict.describeMultiTaskTypes(entity.getTaskType()));

        int runtimeStatus = computeRuntimeStatus(entity);
        vo.setScheduleStatus(runtimeStatus);
        vo.setStatusDesc(SatelliteScheduleStatus.describe(runtimeStatus));

        int submitCycles = taskMapper.countSubmittedCyclesByScheduleId(entity.getScheduleId());
        vo.setSubmitCount(submitCycles);
        vo.setSubmitCountDisplay(submitCycles + "*" + typeList.size());
    }

    private static final class ResultCounter {
        private int fail;
        private int success;
        private int total;
    }

    private SatelliteScheduleDetailVo toDetailVo(SfSatelliteSchedule entity) {
        SatelliteScheduleDetailVo vo = new SatelliteScheduleDetailVo();
        fillBaseVoFields(vo, entity);

        List<SfSatelliteTask> tasks = taskMapper.selectByScheduleId(entity.getScheduleId());
        vo.setTaskCount(tasks.size());
        fillActiveTaskTypes(vo, tasks);
        vo.setExecutions(tasks.stream().map(this::toExecutionVo).collect(Collectors.toList()));

        List<String> dkIds = tasks.stream().map(SfSatelliteTask::getDkId).collect(Collectors.toList());
        List<SfSatelliteTaskResult> resultEntities = taskResultMapper.selectByDkIds(dkIds);
        Map<String, SfSatelliteTask> taskByDkId = tasks.stream()
            .collect(Collectors.toMap(SfSatelliteTask::getDkId, Function.identity(), (a, b) -> a));

        List<SatelliteScheduleDetailVo.ResultItem> results = new ArrayList<>();
        for (SfSatelliteTaskResult result : resultEntities) {
            SatelliteScheduleDetailVo.ResultItem item = new SatelliteScheduleDetailVo.ResultItem();
            item.setDkId(result.getDkId());
            item.setTaskType(result.getTaskType());
            item.setTaskTypeDesc(taskTypeDict.describeTaskType(result.getTaskType()));
            item.setImageDate(result.getImageDate());
            item.setOssUrl(result.getOssUrl());
            item.setObjectKey1(result.getObjectKey1());
            item.setObjectKey2(result.getObjectKey2());
            item.setArea(result.getArea());
            item.setSuccess(result.getSuccess());

            SfSatelliteTask task = taskByDkId.get(result.getDkId());
            if (task != null) {
                item.setStartDate(task.getStartDate());
                item.setEndDate(task.getEndDate());
            }
            results.add(item);
        }
        vo.setResults(results);
        return vo;
    }

    private ScheduleTaskExecutionVo toExecutionVo(SfSatelliteTask task) {
        ScheduleTaskExecutionVo vo = new ScheduleTaskExecutionVo();
        vo.setDkId(task.getDkId());
        vo.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(task.getCreateTime()));
        vo.setTaskType(task.getTaskType());
        vo.setTaskTypeDesc(taskTypeDict.describeTaskType(task.getTaskType()));
        vo.setStartDate(task.getStartDate());
        vo.setEndDate(task.getEndDate());
        vo.setStatus(task.getStatus());
        vo.setStatusDesc(taskStatusDesc(task.getStatus()));
        vo.setSuccess(task.getStatus() != null && task.getStatus() == SatelliteTaskStatus.SUCCESS);
        vo.setMessage(task.getMessage());
        return vo;
    }

    private static String taskStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case SatelliteTaskStatus.PENDING_SUBMIT -> "待提交";
            case SatelliteTaskStatus.PROCESSING -> "处理中";
            case SatelliteTaskStatus.SUCCESS -> "处理成功";
            case SatelliteTaskStatus.FAILED -> "处理失败";
            default -> "未知";
        };
    }

    private int computeRuntimeStatus(SfSatelliteSchedule entity) {
        if (entity.getScheduleStatus() == SatelliteScheduleStatus.PAUSED) {
            return SatelliteScheduleStatus.PAUSED;
        }
        return SatelliteScheduleStatus.computeStatus(entity.getDetectStart(), entity.getDetectEnd(), LocalDate.now(CN));
    }

    private List<String> createMonitoringTasks(SfSatelliteSchedule schedule, List<String> taskTypes, LocalDate start, LocalDate end) {
        List<String> dkIds = new ArrayList<>(taskTypes.size());
        for (String taskType : taskTypes) {
            dkIds.add(taskCrudDelegate.createTaskFromSchedule(schedule, taskType, start, end));
        }
        return dkIds;
    }

    private void submitTasksAfterCommit(List<String> dkIds) {
        if (dkIds == null || dkIds.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    satelliteTaskService.submitTasksNow(dkIds);
                }
            });
            return;
        }
        satelliteTaskService.submitTasksNow(dkIds);
    }

    private static List<String> normalizeTaskTypes(List<String> taskTypes) {
        List<String> normalized = taskTypes.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
        if (normalized.isEmpty()) {
            throw new ServiceException("taskTypes不能为空");
        }
        return normalized;
    }

    private static LocalDate toLocalDate(Date date) {
        return java.time.Instant.ofEpochMilli(date.getTime()).atZone(CN).toLocalDate();
    }
}
