package com.ym.agriculture.api.farming.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 种植批次跨服务摘要。
 */
@Data
public class RemotePlantingBatchSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long batchId;
    private Long fieldId;
    private String fieldName;
    private Long varietyId;
    private String varietyName;
    private Long speciesId;
    private String speciesName;
    private String batchCode;
    private Integer croppingIndex;
    private Date sowingDate;
    private Date expectedHarvestDate;
    private Date actualHarvestDate;
    private String batchStatus;
}
