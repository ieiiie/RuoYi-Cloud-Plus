package com.ym.agriculture.farming.market.model.bo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 农业行情采集记录分页条件。 */
@Data
public class SfMarketIngestQueryBo {
    /** 来源编码。 */
    private String sourceCode;
    /** 处理状态。 */
    private String ingestStatus;
    /** 采集批次。 */
    private String crawlRunId;
    /** 幂等请求号。 */
    private String requestId;
    /** 商品关键词。 */
    private String keyword;
    /** 行情业务日期。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate quoteDate;
}
