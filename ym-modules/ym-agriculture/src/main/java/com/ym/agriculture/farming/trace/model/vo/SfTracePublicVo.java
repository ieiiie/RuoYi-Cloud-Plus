package com.ym.agriculture.farming.trace.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * H5 公开溯源查询响应。
 */
@Data
public class SfTracePublicVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** NORMAL / VOID / NOT_FOUND / DISABLED / UNPUBLISHED */
    private String status;
    private String traceCode;
    private Boolean repeatScan;
    private Date firstScanTime;
    private Integer scanCount;

    private String productName;
    private String qualityGrade;
    private String originText;
    private String producerName;
    private List<TraceCertificationVo> certifications;

    private String speciesName;
    private String varietyName;
    private String fieldName;
    private String batchCode;
    private Date sowingDate;
    private Date expectedHarvestDate;
    private Date actualHarvestDate;
}
