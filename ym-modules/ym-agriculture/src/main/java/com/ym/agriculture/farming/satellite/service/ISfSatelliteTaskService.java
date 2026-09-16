package com.ym.agriculture.farming.satellite.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.satellite.model.bo.SatelliteTaskQueryBo;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteTaskCreateDto;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteTaskDetailVo;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteTaskListVo;
import com.ym.agriculture.farming.satellite.model.vo.ScheduleTaskExecutionVo;

import java.util.List;

/**
 * 遥感任务（ym-gis 能力迁入）。
 */
public interface ISfSatelliteTaskService {

    /**
     * 创建任务：生成 dk_id 落库并立即推送到外部遥感服务。
     *
     * @return dk_id
     */
    String createTask(SatelliteTaskCreateDto dto);

    /**
     * 手工批量补偿提交状态为待提交/失败可重试的遥感任务到外部服务。
     */
    void submitPendingTasksBatch();

    void submitTasksNow(List<String> dkIds);

    SatelliteTaskDetailVo getTaskDetail(String dkId);

    /**
     * 按提交给第三方的 dk_id 删除遥感任务配置。
     */
    void deleteTask(String dkId);

    PageResult<SatelliteTaskListVo> queryPageList(SatelliteTaskQueryBo bo, PageQuery pageQuery);

    /**
     * 回调更新任务状态（成功/失败）。
     */
    void updateTaskStatusForCallback(String dkId, Integer status, String message);

    /**
     * 按 scheduleId 查询该计划下所有的执行记录。
     */
    List<ScheduleTaskExecutionVo> listTasksByScheduleId(Long scheduleId);
}
