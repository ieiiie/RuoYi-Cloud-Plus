package com.ym.agriculture.farming.field.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 地块关联算法推理汇总（图表页卡片 + 分布预览）。
 * <p>
 * {@link #rangeCount}：筛选时间范围内的<strong>检测/推理流水条数</strong>（表行数）。<br>
 * {@link #distinctTaskNoCount} / {@link #distinctUavJobIdCount}：<strong>去重后的算法任务号 / UAV 任务 ID 数</strong>（运维维度，非主图表指标）。<br>
 * {@link #rangeAvgScore} 等：仅统计 {@code cls_score_value} 可解析为数值的行。
 *
 * @author ym-cloud
 */
@Data
public class SfFieldAiInferenceSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 该地块下关联推理流水总条数（不受 alarm 时间过滤）
     */
    private long totalCount;

    /**
     * 时间范围内的流水条数；未传起止时间则与 totalCount 相同
     */
    private long rangeCount;

    /**
     * 去重后的算法 task_no 数
     */
    private long distinctTaskNoCount;

    /**
     * 去重后的 UAV jobId 数
     */
    private long distinctUavJobIdCount;

    /**
     * 时间范围内 cls_score_value 可解析为数值的条数
     */
    private long scoreParsedCount;

    /**
     * 可解析样本上的平均置信度；无样本时为 null
     */
    private BigDecimal rangeAvgScore;

    /**
     * 可解析样本上的最大置信度；无样本时为 null
     */
    private BigDecimal rangeMaxScore;

    /**
     * 可解析样本上的最小置信度；无样本时为 null
     */
    private BigDecimal rangeMinScore;

    /**
     * 时间范围内按 cls_score_label 聚合的条数（label → count）
     */
    private Map<String, Long> labelCounts = new LinkedHashMap<>();
}
