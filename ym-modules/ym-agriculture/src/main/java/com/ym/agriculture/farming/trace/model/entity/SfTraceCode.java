package com.ym.agriculture.farming.trace.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.Date;

/**
 * 瓜果溯源码 sf_trace_code。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_trace_code")
public class SfTraceCode extends SfTraceTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("trace_code_id")
    private Long traceCodeId;

    private Long traceBatchId;
    private String traceCode;
    private Integer seqNo;
    private String status;
    private Date firstScanTime;
    private Integer scanCount;
    private Date lastScanTime;
    private Date lastPrintTime;
    private String remark;

    public SfTraceCode(Long traceCodeId) {
        this.traceCodeId = traceCodeId;
    }
}
