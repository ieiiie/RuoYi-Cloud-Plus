package com.ym.agriculture.farming.field.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 置信度直方图单桶（与 {@code /ai-inference/score-distribution} 返回列表项对应）。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SfFieldAiInferenceScoreBucketVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 桶顺序 0..5，便于前端排序
     */
    private int bucketOrder;

    /**
     * 展示用区间说明，如 {@code [0.0, 0.2)}
     */
    private String bucketLabel;

    /**
     * 桶左边界（含或按展示约定）
     */
    private double bucketMin;

    /**
     * 桶右边界（不含或按展示约定）
     */
    private double bucketMax;

    /**
     * 该桶内推理条数
     */
    private long count;
}
