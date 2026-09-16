package com.ym.agriculture.farming.trace.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 溯源批次统计。
 */
@Data
public class SfTraceBatchStatsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long generatedCount;
    private long voidCount;
    private long normalCount;
    private long scanCount;
    private long firstScanCount;
    private long repeatScanCount;
}
