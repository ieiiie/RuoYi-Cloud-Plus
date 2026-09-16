package com.ym.agriculture.farming.market.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/** 爬虫直写的农业行情采集暂存记录。 */
@Data
@TableName("sf_market_quote_ingest_staging")
public class SfMarketQuoteIngestStaging {
    /** 暂存主键。 */
    @TableId("id")
    private Long id;
    /** 幂等请求号。 */
    private String requestId;
    /** 采集批次。 */
    private String crawlRunId;
    /** 来源编码。 */
    private String sourceCode;
    /** 来源商品稳定标识。 */
    private String externalProductKey;
    /** 来源商品名称。 */
    private String productName;
    /** 规格或等级。 */
    private String specification;
    /** 来源原始品类。 */
    private String sourceCategory;
    /** 报价类型：MARKET/OFFICIAL_AVERAGE。 */
    private String quoteType;
    /** 来源价格。 */
    private BigDecimal price;
    /** 来源单位。 */
    private String unit;
    /** 产地或地区。 */
    private String origin;
    /** 报价市场。 */
    private String marketName;
    /** 行情业务日期。 */
    private LocalDate quoteDate;
    /** UTC 采集时间。 */
    private Date fetchedAtUtc;
    /** 来源地址。 */
    private String sourceUrl;
    /** 原始价格文本。 */
    private String rawPriceText;
    /** 原始载荷 JSON。 */
    private String rawPayloadJson;
    /** 载荷 SHA-256。 */
    private String payloadSha256;
    /** 平台处理状态。 */
    private String ingestStatus;
    /** 处理动作。 */
    private String resultAction;
    /** 处理次数。 */
    private Integer processAttempts;
    /** 告警 JSON。 */
    private String warningsJson;
    /** 人工修正 JSON。 */
    private String correctionJson;
    /** 错误编码。 */
    private String errorCode;
    /** 错误说明。 */
    private String errorMessage;
    /** 平台商品主键。 */
    private Long productId;
    /** 正式报价主键。 */
    private Long quoteId;
    /** 最近处理人主键。 */
    private Long handledBy;
    /** 最近处理人名称。 */
    private String handlerName;
    /** 人工处理说明。 */
    private String handleComment;
    /** 人工处理时间。 */
    private Date handledAt;
    /** 告警是否已确认。 */
    private Boolean warningAcknowledged;
    /** 接收时间。 */
    private Date receivedAt;
    /** 处理开始时间。 */
    private Date processingStartedAt;
    /** 处理完成时间。 */
    private Date processedAt;
}
