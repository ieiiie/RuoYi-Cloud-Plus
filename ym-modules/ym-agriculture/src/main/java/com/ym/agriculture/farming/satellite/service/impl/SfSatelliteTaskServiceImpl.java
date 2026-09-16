package com.ym.agriculture.farming.satellite.service.impl;

import com.ym.system.api.model.LoginUser;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.crop.support.SfTaskCropSnapshotFiller;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.satellite.config.SatelliteProperties;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteScheduleMapper;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskMapper;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskResultMapper;
import com.ym.agriculture.farming.satellite.model.bo.SatelliteTaskQueryBo;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteTaskCreateDto;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTask;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteTaskDetailVo;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteTaskListVo;
import com.ym.agriculture.farming.satellite.model.vo.ScheduleTaskExecutionVo;
import com.ym.agriculture.farming.satellite.remote.SatelliteRemoteDeleteClient;
import com.ym.agriculture.farming.satellite.remote.SatelliteRemoteClient;
import com.ym.agriculture.farming.satellite.service.ISfSatelliteTaskService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Objects;

/**
 * 遥感任务：编排 {@link SatelliteTaskCrudDelegate}、{@link SatelliteTaskRemoteSubmitDelegate}、{@link SatelliteTaskQueryDelegate}。
 */
@Service
public class SfSatelliteTaskServiceImpl implements ISfSatelliteTaskService {

    private final SfSatelliteTaskMapper baseMapper;

    private final SatelliteTaskCrudDelegate crudDelegate;

    private final SatelliteTaskRemoteSubmitDelegate remoteSubmitDelegate;

    private final SatelliteTaskRemoteDeleteDelegate remoteDeleteDelegate;

    private final SatelliteTaskQueryDelegate queryDelegate;

    private final SfFieldMapper fieldMapper;

    private final SfSatelliteScheduleMapper scheduleMapper;

    public SfSatelliteTaskServiceImpl(
        SfSatelliteTaskMapper baseMapper,
        SfSatelliteScheduleMapper scheduleMapper,
        SfSatelliteTaskResultMapper taskResultMapper,
        SfPlantingBatchMapper plantingBatchMapper,
        SfTaskCropSnapshotFiller cropSnapshotFiller,
        SfFieldMapper fieldMapper,
        SatelliteProperties satelliteProperties,
        ObjectProvider<SatelliteRemoteClient> satelliteRemoteClientProvider,
        ObjectProvider<SatelliteRemoteDeleteClient> satelliteRemoteDeleteClientProvider,
        SatelliteTaskTypeDict taskTypeDict
    ) {
        this.baseMapper = baseMapper;
        this.scheduleMapper = scheduleMapper;
        this.crudDelegate = new SatelliteTaskCrudDelegate(baseMapper, plantingBatchMapper, cropSnapshotFiller);
        this.remoteSubmitDelegate = new SatelliteTaskRemoteSubmitDelegate(
            satelliteProperties,
            satelliteRemoteClientProvider,
            baseMapper,
            crudDelegate
        );
        this.remoteDeleteDelegate = new SatelliteTaskRemoteDeleteDelegate(satelliteRemoteDeleteClientProvider);
        this.queryDelegate = new SatelliteTaskQueryDelegate(baseMapper, taskResultMapper, taskTypeDict);
        this.fieldMapper = fieldMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTask(SatelliteTaskCreateDto dto) {
        assertFieldSupportsSatellite(dto.getFieldId());
        String dkId = crudDelegate.createTask(dto);
        submitTasksAfterCommit(List.of(dkId));
        return dkId;
    }

    @Override
    public void submitPendingTasksBatch() {
        remoteSubmitDelegate.submitPendingTasksBatch();
    }

    @Override
    public void submitTasksNow(List<String> dkIds) {
        remoteSubmitDelegate.submitTasksNow(dkIds);
    }

    @Override
    public SatelliteTaskDetailVo getTaskDetail(String dkId) {
        return queryDelegate.getTaskDetail(dkId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(String dkId) {
        if (StringUtils.isBlank(dkId)) {
            throw new ServiceException("dkId不能为空");
        }
        SfSatelliteTask task = baseMapper.selectByDkId(dkId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        assertTaskTenant(task.getTenantId());
        remoteDeleteDelegate.deleteRemote(task);
        int rows = baseMapper.deleteById(task.getTaskId());
        if (rows <= 0) {
            throw new ServiceException("任务删除失败");
        }
        deleteScheduleWhenNoActiveTasksRemain(task.getScheduleId());
    }

    @Override
    public PageResult<SatelliteTaskListVo> queryPageList(SatelliteTaskQueryBo bo, PageQuery pageQuery) {
        return queryDelegate.queryPageList(bo, pageQuery);
    }

    @Override
    public void updateTaskStatusForCallback(String dkId, Integer status, String message) {
        crudDelegate.updateTaskStatusForCallback(dkId, status, message);
    }

    @Override
    public List<ScheduleTaskExecutionVo> listTasksByScheduleId(Long scheduleId) {
        return queryDelegate.listTasksByScheduleId(scheduleId);
    }

    private void submitTasksAfterCommit(List<String> dkIds) {
        if (dkIds == null || dkIds.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    remoteSubmitDelegate.submitTasksNow(dkIds);
                }
            });
            return;
        }
        remoteSubmitDelegate.submitTasksNow(dkIds);
    }

    private void assertFieldSupportsSatellite(Long fieldId) {
        if (fieldId == null) {
            return;
        }
        SfField field = fieldMapper.selectById(fieldId);
        if (field == null) {
            throw new ServiceException("地块不存在");
        }
        if (FieldType.isGreenhouse(FieldType.normalizeOrDefault(field.getFieldType()))) {
            throw new ServiceException("大棚顶棚遮挡地表，卫星遥感数据不适用");
        }
    }

    private void assertTaskTenant(String taskTenantId) {
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException("未登录");
        }
        if (!Objects.equals(taskTenantId, loginUser.getTenantId())) {
            throw new ServiceException("无权限删除该任务");
        }
    }

    /**
     * 周期计划的最后一个子任务被删除后，计划已无继续保留的意义，随任务删除一并逻辑删除。
     */
    private void deleteScheduleWhenNoActiveTasksRemain(Long scheduleId) {
        if (scheduleId == null || baseMapper.countActiveByScheduleId(scheduleId) > 0) {
            return;
        }
        if (scheduleMapper.selectById(scheduleId) == null) {
            return;
        }
        int rows = scheduleMapper.deleteById(scheduleId);
        if (rows <= 0) {
            throw new ServiceException("计划删除失败");
        }
    }
}
