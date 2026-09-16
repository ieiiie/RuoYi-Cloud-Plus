package com.ym.agriculture.farming.batch.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

/**
 * 种植批次详情。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfPlantingBatchDetailVo extends SfPlantingBatchVo {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<SfPlantingBatchLogVo> logs = Collections.emptyList();
    private List<String> analysisHints = Collections.emptyList();
}
