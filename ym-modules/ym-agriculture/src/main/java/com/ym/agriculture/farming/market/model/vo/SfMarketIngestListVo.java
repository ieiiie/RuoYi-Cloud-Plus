package com.ym.agriculture.farming.market.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

/** 管理端农业行情采集记录列表项。 */
@Data
public class SfMarketIngestListVo {
    /** 暂存主键。 */
    private Long id;
    /** 幂等请求号。 */
    private String requestId;
    /** 采集批次。 */
    private String crawlRunId;
    /** 来源编码。 */
    private String sourceCode;
    /** 来源名称。 */
    private String sourceName;
    /** 商品名称。 */
    private String productName;
    /** 规格或等级。 */
    private String specification;
    /** 来源价格。 */
    private Double price;
    /** 来源单位。 */
    private String unit;
    /** 行情业务日期。 */
    private LocalDate quoteDate;
    /** 处理状态。 */
    private String ingestStatus;
    /** 处理动作。 */
    private String resultAction;
    /** 错误编码。 */
    private String errorCode;
    /** 错误说明。 */
    private String errorMessage;
    /** 告警 JSON。 */
    private String warningsJson;
    /** 告警是否已确认。 */
    private Boolean warningAcknowledged;
    /** 接收时间。 */
    private Date receivedAt;
    /** 处理完成时间。 */
    private Date processedAt;
}
