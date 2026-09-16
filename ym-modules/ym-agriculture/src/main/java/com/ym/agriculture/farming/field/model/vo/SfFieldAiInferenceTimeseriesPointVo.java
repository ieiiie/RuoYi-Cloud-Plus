package com.ym.agriculture.farming.field.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 时间序列图单点（折线/面积图）。
 *
 * @author ym-cloud
 */
@Data
public class SfFieldAiInferenceTimeseriesPointVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 桶起点：day 为 yyyy-MM-dd；hour 为 yyyy-MM-dd HH:00:00
     */
    private String bucketStart;

    /**
     * 该时间桶内推理条数
     */
    private long count;

    /**
     * 可解析为数字的 cls_score_value 的平均值；无则 null
     */
    private BigDecimal avgScore;
}
