package com.ym.agriculture.farming.field.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceLabelStatVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferencePageVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceScoreBucketVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceSummaryVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceTimeseriesPointVo;

import java.util.Date;
import java.util.List;

/**
 * 地块维度算法推理图表与明细只读查询（经 UAV AI 任务关联）。
 */
public interface ISfFieldAiInferenceChartService {

    SfFieldAiInferenceSummaryVo querySummary(Long fieldId, Long plantingBatchId, Date startTime, Date endTime);

    List<SfFieldAiInferenceTimeseriesPointVo> queryTimeseries(
        Long fieldId, Long plantingBatchId, String granularity, Date startTime, Date endTime);

    List<SfFieldAiInferenceLabelStatVo> queryByLabel(Long fieldId, Long plantingBatchId, Date startTime, Date endTime);

    /**
     * 置信度直方图：可解析 {@code cls_score_value} 在 [0,1] 上五分桶及桶外条数。
     */
    List<SfFieldAiInferenceScoreBucketVo> queryScoreDistribution(
        Long fieldId, Long plantingBatchId, Date startTime, Date endTime);

    PageResult<SfFieldAiInferencePageVo> queryPage(
        Long fieldId,
        Long plantingBatchId,
        Date startTime,
        Date endTime,
        String taskNo,
        String modelNo,
        String labelKeyword,
        PageQuery pageQuery);
}
