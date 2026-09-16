package com.ym.agriculture.farming.satellite.controller;

import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.satellite.model.bo.SatelliteScheduleQueryBo;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteScheduleCreateDto;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteScheduleDetailVo;
import com.ym.agriculture.farming.satellite.model.vo.SatelliteScheduleVo;
import com.ym.agriculture.farming.satellite.service.ISfSatelliteScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 卫星遥感周期计划 API（路径统一在 {@code /smart-farming/satellite/schedule} 下）。
 * <p>
 * 用于在种植批次维度创建、查询、暂停/恢复、删除周期遥感监测计划：
 * <ul>
 *     <li>POST  {@code /smart-farming/satellite/schedule}                     — 创建周期计划</li>
 *     <li>GET   {@code /smart-farming/satellite/schedule/list}                — 按种植批次/地块筛选计划列表</li>
 *     <li>GET   {@code /smart-farming/satellite/schedule/{scheduleId}}        — 计划详情</li>
 *     <li>PUT   {@code /smart-farming/satellite/schedule/{scheduleId}/pause}  — 暂停计划</li>
 *     <li>PUT   {@code /smart-farming/satellite/schedule/{scheduleId}/resume} — 恢复计划</li>
 *     <li>DELETE {@code /smart-farming/satellite/schedule/{scheduleId}}        — 删除计划</li>
 * </ul>
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/satellite/schedule")
public class SfSatelliteScheduleController extends BaseController {

    private final ISfSatelliteScheduleService scheduleService;

    /**
     * 创建周期遥感计划（支持多选分析类型，每种类型创建一条计划）。
     *
     * @param dto 计划配置（种植批次、地块、检测时间范围、周期天数、任务类型列表等）
     * @return {@code R<List<Long>>} data 为新建计划的 {@code scheduleId} 列表
     */
    @PostMapping
    public R<List<Long>> createSchedule(@Valid @RequestBody SatelliteScheduleCreateDto dto) {
        return R.ok(scheduleService.createSchedule(dto));
    }

    /**
     * 分页查询遥感周期计划列表。
     *
     * @param bo        查询条件（物种名称、分析类型等）
     * @param pageQuery 分页参数
     * @return 分页数据
     */
    @GetMapping("/page")
    public R<PageResult<SatelliteScheduleVo>> pageSchedules(SatelliteScheduleQueryBo bo, PageQuery pageQuery) {
        return R.ok(scheduleService.pageSchedules(bo, pageQuery));
    }

    /**
     * 按种植批次和/或地块筛选计划列表。
     *
     * @param plantingBatchId 种植批次 ID，可选
     * @param fieldId         地块 ID，可选
     * @return 计划列表
     */
    @GetMapping("/list")
    public R<List<SatelliteScheduleVo>> listSchedules(
        @RequestParam(required = false) Long plantingBatchId,
        @RequestParam(required = false) Long fieldId) {
        return R.ok(scheduleService.listSchedules(plantingBatchId, fieldId));
    }

    /**
     * 查询单个计划详情。
     *
     * @param scheduleId 计划主键
     * @return 计划详情
     */
    @GetMapping("/{scheduleId}")
    public R<SatelliteScheduleDetailVo> getScheduleDetail(@PathVariable Long scheduleId) {
        return R.ok(scheduleService.getScheduleDetail(scheduleId));
    }

    /**
     * 暂停活跃计划，暂停后定时任务不再为其创建子任务。
     *
     * @param scheduleId 计划主键
     */
    @PutMapping("/{scheduleId}/pause")
    public R<Void> pauseSchedule(@PathVariable Long scheduleId) {
        scheduleService.pauseSchedule(scheduleId);
        return R.ok();
    }

    /**
     * 恢复暂停的计划，恢复后定时任务将继续按周期创建子任务。
     *
     * @param scheduleId 计划主键
     */
    @PutMapping("/{scheduleId}/resume")
    public R<Void> resumeSchedule(@PathVariable Long scheduleId) {
        scheduleService.resumeSchedule(scheduleId);
        return R.ok();
    }

    /**
     * 逻辑删除计划。
     *
     * @param scheduleId 计划主键
     */
    @DeleteMapping("/{scheduleId}")
    public R<Void> deleteSchedule(@PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return R.ok();
    }
}
