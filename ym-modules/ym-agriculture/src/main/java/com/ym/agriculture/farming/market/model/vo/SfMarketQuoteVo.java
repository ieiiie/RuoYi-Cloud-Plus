package com.ym.agriculture.farming.market.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

/** 管理端及移动端使用的农业行情报价。 */
@Data
public class SfMarketQuoteVo {
    /** 报价主键。 */
    private Long quoteId;
    /** 平台商品主键。 */
    private Long productId;
    /** 商品名称。 */
    private String productName;
    /** 规格或等级。 */
    private String specification;
    /** 商品与规格组合展示名称。 */
    private String displayName;
    /** 商品品类编码。 */
    private String category;
    /** 商品品类名称。 */
    private String categoryName;
    /** 平台标准价格。 */
    private Double price;
    /** 平台标准单位。 */
    private String unit;
    /** 上一有效报价。 */
    private Double previousPrice;
    /** 涨跌额。 */
    private Double changeAmount;
    /** 涨跌幅，0.7 表示 0.7%。 */
    private Double changePercent;
    /** 趋势：up/down/flat/unknown。 */
    private String trend;
    /** 报价类型。 */
    private String quoteType;
    /** 产地或地区。 */
    private String origin;
    /** 报价市场。 */
    private String marketName;
    /** 行情业务日期。 */
    private LocalDate quoteDate;
    /** 采集时间。 */
    private Date collectedAt;
    /** 来源编码。 */
    private String sourceCode;
    /** 来源名称。 */
    private String sourceName;
    /** 来源地址。 */
    private String sourceUrl;
    /** 报价告警 JSON。 */
    private String warningsJson;
    /** 告警是否已人工确认。 */
    private Boolean warningAcknowledged;
}
