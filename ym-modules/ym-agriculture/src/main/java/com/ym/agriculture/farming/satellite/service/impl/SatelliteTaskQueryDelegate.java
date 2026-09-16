package com.ym.agriculture.farming.satellite.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskMapper;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskResultMapper;
import com.ym.agriculture.farming.satellite.model.bo.SatelliteTaskQueryBo;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteTaskStatus;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTask;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTaskResult;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteTaskDetailVo;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteTaskListVo;
import com.ym.agriculture.farming.satellite.model.vo.ScheduleTaskExecutionVo;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 遥感任务读路径：详情、分页列表及 VO 组装。
 */
@RequiredArgsConstructor
final class SatelliteTaskQueryDelegate {

    private final SfSatelliteTaskMapper taskMapper;

    private final SfSatelliteTaskResultMapper taskResultMapper;

    private final SatelliteTaskTypeDict taskTypeDict;

    SatelliteTaskDetailVo getTaskDetail(String dkId) {
        SfSatelliteTask task = taskMapper.selectByDkId(dkId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        assertTenant(task.getTenantId());

        SatelliteTaskDetailVo vo = new SatelliteTaskDetailVo();
        vo.setDkId(task.getDkId());
        vo.setFieldId(task.getFieldId());
        vo.setPlantingBatchId(task.getPlantingBatchId());
        vo.setPlantingBatchName(task.getPlantingBatchName());
        vo.setFieldName(task.getFieldName());
        vo.setSpeciesId(task.getSpeciesId());
        vo.setSpeciesName(task.getSpeciesName());
        vo.setVarietyId(task.getVarietyId());
        vo.setVarietyName(task.getVarietyName());
        vo.setStatus(task.getStatus());
        vo.setStatusDesc(taskStatusDesc(task.getStatus()));
        vo.setMessage(task.getMessage());
        vo.setSubmitCount(task.getSubmitCount());
        vo.setLastSubmitTime(task.getLastSubmitTime());
        vo.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(task.getCreateTime()));
        vo.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(task.getUpdateTime()));
        vo.setTaskType(task.getTaskType());

        List<SfSatelliteTaskResult> resultList = taskResultMapper.selectByDkIdOrderByImageDateDesc(dkId);
        if (!resultList.isEmpty()) {
            List<SatelliteTaskDetailVo.TaskResultVo> rvs = new ArrayList<>();
            for (SfSatelliteTaskResult r : resultList) {
                SatelliteTaskDetailVo.TaskResultVo rv = new SatelliteTaskDetailVo.TaskResultVo();
                rv.setImageDate(r.getImageDate());
                rv.setObjectKey1(r.getObjectKey1());
                rv.setObjectKey2(r.getObjectKey2());
                rv.setArea(r.getArea());
                rv.setSuccess(r.getSuccess());
                rv.setOssUrl(r.getOssUrl());
                rv.setTaskType(r.getTaskType());
                rv.setPlantingBatchId(r.getPlantingBatchId());
                rvs.add(rv);
            }
            vo.setResults(rvs);
        }
        return vo;
    }

    PageResult<SatelliteTaskListVo> queryPageList(SatelliteTaskQueryBo bo, PageQuery pageQuery) {
        Page<SfSatelliteTask> page = pageQuery.build();
        boolean filterByTenant = !LoginHelper.isSuperAdmin() && LoginHelper.getLoginUser() != null;
        String tenantId = filterByTenant ? LoginHelper.getLoginUser().getTenantId() : null;
        Page<SfSatelliteTask> result = taskMapper.selectTaskPage(page, filterByTenant, tenantId, bo);
        List<SatelliteTaskListVo> rows = result.getRecords().stream().map(this::toListVo).collect(Collectors.toList());
        fillResultCounts(rows);
        Page<SatelliteTaskListVo> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(rows);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(voPage);
    }

    List<ScheduleTaskExecutionVo> listTasksByScheduleId(Long scheduleId) {
        List<SfSatelliteTask> tasks = taskMapper.selectByScheduleId(scheduleId);
        return tasks.stream().map(this::toExecutionVo).collect(Collectors.toList());
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

    private SatelliteTaskListVo toListVo(SfSatelliteTask task) {
        SatelliteTaskListVo vo = new SatelliteTaskListVo();
        vo.setDkId(task.getDkId());
        vo.setFieldId(task.getFieldId());
        vo.setPlantingBatchId(task.getPlantingBatchId());
        vo.setPlantingBatchName(task.getPlantingBatchName());
        vo.setFieldName(task.getFieldName());
        vo.setSpeciesId(task.getSpeciesId());
        vo.setSpeciesName(task.getSpeciesName());
        vo.setVarietyId(task.getVarietyId());
        vo.setVarietyName(task.getVarietyName());
        vo.setCodeCroptype(task.getCodeCroptype());
        vo.setStartDate(task.getStartDate());
        vo.setEndDate(task.getEndDate());
        vo.setTaskType(task.getTaskType());
        vo.setTaskTypeDesc(taskTypeDict.describeTaskType(task.getTaskType()));
        vo.setStatus(task.getStatus());
        vo.setStatusDesc(taskStatusDesc(task.getStatus()));
        vo.setMessage(task.getMessage());
        vo.setSubmitCount(task.getSubmitCount());
        vo.setResultSuccessCount(0);
        vo.setResultFailCount(0);
        vo.setLastSubmitTime(task.getLastSubmitTime());
        vo.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(task.getCreateTime()));
        vo.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(task.getUpdateTime()));
        return vo;
    }

    private void fillResultCounts(List<SatelliteTaskListVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<String> dkIds = rows.stream()
            .map(SatelliteTaskListVo::getDkId)
            .filter(StringUtils::isNotBlank)
            .toList();
        if (dkIds.isEmpty()) {
            return;
        }
        Map<String, ResultCounter> counters = new HashMap<>();
        for (SfSatelliteTaskResult result : taskResultMapper.selectByDkIds(dkIds)) {
            if (result.getDkId() == null) {
                continue;
            }
            ResultCounter counter = counters.computeIfAbsent(result.getDkId(), k -> new ResultCounter());
            if (Boolean.TRUE.equals(result.getSuccess())) {
                counter.success++;
            } else if (Boolean.FALSE.equals(result.getSuccess())) {
                counter.fail++;
            }
        }
        for (SatelliteTaskListVo row : rows) {
            ResultCounter counter = counters.get(row.getDkId());
            row.setResultSuccessCount(counter == null ? 0 : counter.success);
            row.setResultFailCount(counter == null ? 0 : counter.fail);
        }
    }

    private static final class ResultCounter {
        private int fail;
        private int success;
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

    private void assertTenant(String taskTenantId) {
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        if (LoginHelper.getLoginUser() == null) {
            throw new ServiceException("未登录");
        }
        if (!Objects.equals(taskTenantId, LoginHelper.getLoginUser().getTenantId())) {
            throw new ServiceException("无权限查看该任务");
        }
    }

}
