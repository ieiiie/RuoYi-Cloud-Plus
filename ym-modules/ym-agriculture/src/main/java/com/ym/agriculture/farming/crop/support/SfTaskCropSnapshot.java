package com.ym.agriculture.farming.crop.support;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务创建时地块/作物冗余快照（不落关联表，仅展示与统计用）。
 */
@Data
public class SfTaskCropSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建时关联的种植批次 ID（飞行任务落库用；遥感/AI 可与入参批次一致） */
    private Long plantingBatchId;

    /**
     * 种植批次展示名，取 {@code sf_planting_batch.batch_code}（业务上作批次名称/编号展示）。
     */
    private String plantingBatchName;

    private String fieldName;
    private Long speciesId;
    private String speciesName;
    /** 物种遥感编号 */
    private Integer remoteSensingCode;
    private Long varietyId;
    private String varietyName;
}
