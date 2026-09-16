package com.ym.agriculture.farming.field.controller;

import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceLabelStatVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferencePageVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceScoreBucketVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceSummaryVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceTimeseriesPointVo;
import com.ym.agriculture.farming.field.service.ISfFieldAiInferenceChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 地块算法推理结果图表与明细分页（只读）
 * <p>
 * 数据经 {@code sf_uav_ai_task} 与 {@code sf_ai_inference_log} 按 {@code alg_task_no = task_no} 关联；
 * 权限与 {@link SfFieldController#getInfo(Long)} 一致。
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/field")
public class SfFieldAiInferenceController extends BaseController {

    private final ISfFieldAiInferenceChartService chartService;

    /**
     * 汇总：条数、去重任务、置信度统计、标签分布
     *
     * @param fieldId         地块主键
     * @param plantingBatchId 可选，限定种植批次
     * @param startTime       可选，告警日起 {@code yyyy-MM-dd}
     * @param endTime         可选，告警日止 {@code yyyy-MM-dd}
     * @return 统一响应，{@code data} 为汇总 VO
     */
    @GetMapping("/{fieldId}/ai-inference/summary")
    public R<SfFieldAiInferenceSummaryVo> summary(
        @PathVariable Long fieldId,
        @RequestParam(required = false) Long plantingBatchId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime) {
        return R.ok(chartService.querySummary(fieldId, plantingBatchId, startTime, endTime));
    }

    /**
     * 时间序列：按天或按小时桶化的条数与平均分
     *
     * @param fieldId         地块主键
     * @param plantingBatchId 可选
     * @param granularity     {@code day} 或 {@code hour}，默认 {@code day}
     * @param startTime       可选
     * @param endTime         可选
     * @return 统一响应，{@code data} 为时序点列表
     */
    @GetMapping("/{fieldId}/ai-inference/timeseries")
    public R<List<SfFieldAiInferenceTimeseriesPointVo>> timeseries(
        @PathVariable Long fieldId,
        @RequestParam(required = false) Long plantingBatchId,
        @RequestParam(defaultValue = "day") String granularity,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime) {
        return R.ok(chartService.queryTimeseries(fieldId, plantingBatchId, granularity, startTime, endTime));
    }

    /**
     * 按分类标签分布
     *
     * @param fieldId         地块主键
     * @param plantingBatchId 可选
     * @param startTime       可选
     * @param endTime         可选
     * @return 统一响应，{@code data} 为标签统计列表
     */
    @GetMapping("/{fieldId}/ai-inference/by-label")
    public R<List<SfFieldAiInferenceLabelStatVo>> byLabel(
        @PathVariable Long fieldId,
        @RequestParam(required = false) Long plantingBatchId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime) {
        return R.ok(chartService.queryByLabel(fieldId, plantingBatchId, startTime, endTime));
    }

    /**
     * 置信度直方图分桶（与 summary 时间筛选一致）
     *
     * @param fieldId         地块主键
     * @param plantingBatchId 可选
     * @param startTime       可选
     * @param endTime         可选
     * @return 统一响应，{@code data} 为分桶列表
     */
    @GetMapping("/{fieldId}/ai-inference/score-distribution")
    public R<List<SfFieldAiInferenceScoreBucketVo>> scoreDistribution(
        @PathVariable Long fieldId,
        @RequestParam(required = false) Long plantingBatchId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime) {
        return R.ok(chartService.queryScoreDistribution(fieldId, plantingBatchId, startTime, endTime));
    }

    /**
     * 推理明细分页
     *
     * @param fieldId         地块主键
     * @param plantingBatchId 可选
     * @param startTime       可选
     * @param endTime         可选
     * @param taskNo          可选，任务号模糊
     * @param modelNo         可选，模型号模糊
     * @param labelKeyword    可选，标签关键字模糊
     * @param pageQuery       分页（须传 pageNum、pageSize）
     * @return 分页结果，{@code rows} 为明细 VO
     */
    @GetMapping("/{fieldId}/ai-inference/page")
    public R<PageResult<SfFieldAiInferencePageVo>> page(
        @PathVariable Long fieldId,
        @RequestParam(required = false) Long plantingBatchId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime,
        @RequestParam(required = false) String taskNo,
        @RequestParam(required = false) String modelNo,
        @RequestParam(required = false) String labelKeyword,
        PageQuery pageQuery) {
        return R.ok(chartService.queryPage(fieldId, plantingBatchId, startTime, endTime, taskNo, modelNo, labelKeyword, pageQuery));
    }
}
