package com.ym.agriculture.farming.satellite.controller;

import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.satellite.model.bo.SatelliteTaskQueryBo;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteTaskCreateDto;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteTaskDetailVo;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteTaskListVo;
import com.ym.agriculture.farming.satellite.model.vo.ScheduleTaskExecutionVo;
import com.ym.agriculture.farming.satellite.service.ISfSatelliteTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 卫星遥感任务 API（ym-gis 迁入，路径统一在 {@code /smart-farming/satellite} 下）。
 * <p>
 * 对应《地块遥感任务提交接口文档》，用于在农事平台侧创建、查询卫星遥感任务：
 * <ul>
 *     <li>POST {@code /smart-farming/satellite/task}：创建任务并向外部遥感服务发起申请，返回下游使用的 {@code dk_id}</li>
 *     <li>GET {@code /smart-farming/satellite/task/{dkId}}：按地块标识查询单个任务详情</li>
 *     <li>GET {@code /smart-farming/satellite/tasks}：按条件分页查询任务列表</li>
 * </ul>
 * 所有接口统一返回 {@link com.ym.common.core.domain.R} 或 {@link PageResult}：
 * <ul>
 *     <li>{@code R} 响应体字段说明：
 *     <ul>
 *         <li>{@code code} 业务状态码（如 200 表示成功，其他为失败）</li>
 *         <li>{@code msg} 提示信息（成功/失败原因）</li>
 *         <li>{@code data} 具体业务数据（如 {@code dk_id}、任务详情对象等）</li>
 *     </ul>
 *     </li>
 *     <li>{@code PageResult} 响应体字段说明：
 *     <ul>
 *         <li>{@code code} 业务状态码</li>
 *         <li>{@code msg} 提示信息</li>
 *         <li>{@code rows} 当前页数据列表</li>
 *         <li>{@code total} 符合条件的总记录数</li>
 *     </ul>
 *     </li>
 * </ul>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/satellite")
public class SfSatelliteTaskController extends BaseController {

    private final ISfSatelliteTaskService satelliteTaskService;

    /**
     * 创建遥感任务。
     * <p>
     * 根据前端提交的任务配置（地块空间信息、作物类型、时间范围、任务类型 {@code task_type} 等），
     * 在本系统及外部遥感服务中创建一条卫星遥感任务记录，并生成唯一的地块标识 {@code dk_id}。
     *
     * @param dto 创建任务请求体，包含地块几何 {@code dk_geom}、作物类型 {@code code_croptype}、
     *            起止日期 {@code start_date}/{@code end_date}、任务类型 {@code task_type} 等信息
     * @return {@code R<String>}：
     * <ul>
     *     <li>{@code code} 调用是否成功的状态码</li>
     *     <li>{@code msg} 提示信息</li>
     *     <li>{@code data} 为创建成功后的遥感侧地块标识 {@code dk_id}</li>
     * </ul>
     */
    @PostMapping("/task")
    public R<String> createTask(@Valid @RequestBody SatelliteTaskCreateDto dto) {
        return R.ok(satelliteTaskService.createTask(dto));
    }

    /**
     * 查询单个遥感任务详情。
     * <p>
     * 通过地块标识 {@code dkId} 获取对应任务的基础信息、配置项以及当前处理状态，
     * 便于前端在任务详情页展示本次遥感申请的完整数据。
     *
     * @param dkId 遥感侧地块标识 {@code dk_id}，由创建任务接口返回
     * @return {@code R<SatelliteTaskDetailVo>}：
     * <ul>
     *     <li>{@code code} 调用是否成功的状态码</li>
     *     <li>{@code msg} 提示信息</li>
     *     <li>{@code data} 为任务详情视图对象，包含任务配置及状态</li>
     * </ul>
     */
    @GetMapping("/task/{dkId}")
    public R<SatelliteTaskDetailVo> getTaskDetail(@PathVariable String dkId) {
        return R.ok(satelliteTaskService.getTaskDetail(dkId));
    }

    /**
     * 删除遥感任务。
     * <p>
     * {@code dkId} 必须是创建任务时提交给第三方并保存到 {@code sf_satellite_task.dk_id} 的同一个标识。
     * 后端会使用该任务记录中的 {@code dk_id/start_date/end_date/task_type} 调用第三方删除接口，
     * 第三方删除成功后再逻辑删除本地任务配置。
     */
    @DeleteMapping("/task/{dkId}")
    public R<Void> deleteTask(@PathVariable String dkId) {
        satelliteTaskService.deleteTask(dkId);
        return R.ok();
    }

    /**
     * 分页查询遥感任务列表。
     * <p>
     * 通过 {@link SatelliteTaskQueryBo} 提供的查询条件（如地块 ID、作物类型、任务类型、日期范围、处理状态等）
     * 以及 {@link PageQuery} 提供的分页参数，按页返回任务列表数据。
     *
     * @param bo        查询条件封装对象
     * @param pageQuery 分页参数（页码、每页大小、排序等）
     * @return {@code PageResult<SatelliteTaskListVo>}：
     * <ul>
     *     <li>{@code rows} 当前页任务列表，每行对应一个 {@code SatelliteTaskListVo}</li>
     *     <li>{@code total} 符合条件的任务总数</li>
     *     <li>分页元数据由 {@code PageResult} 统一封装</li>
     * </ul>
     */
    @GetMapping("/tasks")
    public R<PageResult<SatelliteTaskListVo>> queryTasks(SatelliteTaskQueryBo bo, PageQuery pageQuery) {
        return R.ok(satelliteTaskService.queryPageList(bo, pageQuery));
    }

    /**
     * 按遥感周期计划查询执行记录（执行结果 Tab）。
     *
     * @param scheduleId 周期计划主键
     * @return 该计划下所有子任务的执行记录
     */
    @GetMapping("/tasks/by-schedule/{scheduleId}")
    public R<List<ScheduleTaskExecutionVo>> listTasksBySchedule(@PathVariable Long scheduleId) {
        return R.ok(satelliteTaskService.listTasksByScheduleId(scheduleId));
    }
}
