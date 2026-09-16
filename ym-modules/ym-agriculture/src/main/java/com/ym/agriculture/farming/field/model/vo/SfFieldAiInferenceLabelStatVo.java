package com.ym.agriculture.farming.field.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 分类分布（饼图/柱状图）单项。
 *
 * @author ym-cloud
 */
@Data
public class SfFieldAiInferenceLabelStatVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分类标签名
     */
    private String label;

    /**
     * 该标签下推理条数
     */
    private long count;

    /**
     * 该标签下可解析置信度的平均值；无样本时为 null
     */
    private BigDecimal avgScore;

    /**
     * 该标签下可解析置信度的最大值；无样本时为 null
     */
    private BigDecimal maxScore;
}
