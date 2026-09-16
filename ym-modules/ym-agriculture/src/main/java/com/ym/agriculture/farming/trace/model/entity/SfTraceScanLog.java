package com.ym.agriculture.farming.trace.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.Date;

/**
 * 瓜果溯源扫码日志 sf_trace_scan_log（无逻辑删除）。
 */
@Data
@NoArgsConstructor
@TableName("sf_trace_scan_log")
public class SfTraceScanLog {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("scan_log_id")
    private Long scanLogId;

    private String tenantId;
    private Long traceCodeId;
    private String traceCode;
    private Date scanTime;
    private String scanResult;
    private Integer isRepeat;
    private String ip;
    private String userAgent;
    private String referer;
}
