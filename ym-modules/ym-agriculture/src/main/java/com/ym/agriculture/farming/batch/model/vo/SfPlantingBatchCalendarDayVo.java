package com.ym.agriculture.farming.batch.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 某日种植批次聚合。
 */
@Data
public class SfPlantingBatchCalendarDayVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String date;
    private List<SfPlantingBatchCalendarItemVo> batches;
}
