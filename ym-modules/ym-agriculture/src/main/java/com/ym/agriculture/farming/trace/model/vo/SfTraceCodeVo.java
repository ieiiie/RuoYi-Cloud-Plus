package com.ym.agriculture.farming.trace.model.vo;

import com.ym.agriculture.farming.trace.model.entity.SfTraceCode;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 溯源码视图。
 */
@Data
@AutoMapper(target = SfTraceCode.class)
public class SfTraceCodeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long traceCodeId;
    private String tenantId;
    private Long traceBatchId;
    private String traceCode;
    private Integer seqNo;
    private String status;
    private Date firstScanTime;
    private Integer scanCount;
    private Date lastScanTime;
    private Date lastPrintTime;
    private Date createTime;

    private String traceBatchNo;
    private String productName;
}
