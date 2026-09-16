package com.ym.agriculture.farming.batch.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 种植批次日历单元摘要。
 */
@Data
public class SfPlantingBatchCalendarItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long batchId;
    private String batchCode;
    private String fieldName;
    private String varietyName;
    private String batchStatus;
    private Date sowingDate;
    private Date expectedHarvestDate;
}
