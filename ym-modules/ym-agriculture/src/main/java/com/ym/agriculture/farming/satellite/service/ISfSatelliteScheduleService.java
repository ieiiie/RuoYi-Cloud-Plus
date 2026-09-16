package com.ym.agriculture.farming.satellite.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.satellite.model.bo.SatelliteScheduleQueryBo;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteScheduleCreateDto;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteScheduleCreateResult;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteScheduleDetailVo;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteScheduleVo;

import java.util.List;

/**
 * 卫星遥感周期计划服务。
 * <p>
 * 创建时一次性展开全部子任务到 {@code sf_satellite_task}，由提交 Cron 根据日期守卫逐批提交（{@code end_date <= 今天} 后才提交）。
 * 暂停/恢复级联冻结/解冻未提交子任务，删除级联逻辑删除未提交子任务。
 *
 * @author ym-cloud
 * @see com.ym.agriculture.farming.satellite.model.entity.SfSatelliteSchedule
 */
public interface ISfSatelliteScheduleService {

    /**
     * 创建周期遥感计划（支持多选分析类型），同时一次性展开全部日期窗口子任务。
     *
     * @param dto 创建入参，包含种植批次、地块、检测时间范围、周期天数、任务类型列表等
     * @return 新创建计划的 {@code scheduleId} 列表
     */
    List<Long> createSchedule(SatelliteScheduleCreateDto dto);

    /**
     * 创建一条周期遥感计划，并返回该计划及其关联子任务编号。
     *
     * @param dto 创建入参，包含种植批次、地块、检测时间范围、任务类型列表等
     * @return 新创建的计划及其子任务编号
     */
    SatelliteScheduleCreateResult createScheduleWithTasks(SatelliteScheduleCreateDto dto);

    /**
     * 查询计划详情。
     *
     * @param scheduleId 计划主键
     * @return 计划视图对象
     * @throws com.ym.common.core.exception.ServiceException 计划不存在时抛出
     */
    SatelliteScheduleDetailVo getScheduleDetail(Long scheduleId);

    /**
     * 按种植批次和/或地块筛选计划列表，按创建时间倒序。
     *
     * @param plantingBatchId 种植批次 ID，可选
     * @param fieldId         地块 ID，可选
     * @return 计划列表
     */
    List<SatelliteScheduleVo> listSchedules(Long plantingBatchId, Long fieldId);

    /**
     * 分页查询计划列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页数据
     */
    PageResult<SatelliteScheduleVo> pageSchedules(SatelliteScheduleQueryBo bo, PageQuery pageQuery);

    /**
     * 暂停周期中的计划（{@code scheduleStatus} 1→3）。
     *
     * @param scheduleId 计划主键
     */
    void pauseSchedule(Long scheduleId);

    /**
     * 恢复已停用的计划（{@code scheduleStatus} 3→1）。
     *
     * @param scheduleId 计划主键
     */
    void resumeSchedule(Long scheduleId);

    /**
     * 逻辑删除计划，级联逻辑删除所有待提交子任务。
     *
     * @param scheduleId 计划主键
     */
    void deleteSchedule(Long scheduleId);

    /**
     * 已废弃：创建计划时已一次性展开全部子任务，此方法为空操作（保留接口兼容性）。
     */
    void executeScheduledTasks();
}
