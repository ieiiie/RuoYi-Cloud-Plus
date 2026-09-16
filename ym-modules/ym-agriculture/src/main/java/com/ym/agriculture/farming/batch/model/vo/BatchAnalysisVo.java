package com.ym.agriculture.farming.batch.model.vo;

import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferencePageVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 种植批次维度的 AI 分析 + 遥感结果聚合视图。
 *
 * @author ym-cloud
 */
@Data
public class BatchAnalysisVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 最近 AI 推理记录 */
    private List<SfFieldAiInferencePageVo> latestAiInferences;

    /** 遥感子任务列表（每个子任务下挂自身回调得到的遥感结果） */
    private List<BatchSatelliteSubTaskVo> satelliteSubTasks;
}
