package com.ym.agriculture.farming.market.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/** 对移动端发布的农业行情正式报价。 */
@Data
@TableName("sf_market_quote")
public class SfMarketQuote {
    /** 报价主键。 */
    @TableId("quote_id")
    private Long quoteId;
    /** 最近暂存记录主键。 */
    private Long latestIngestId;
    /** 平台商品主键。 */
    private Long productId;
    /** 来源编码。 */
    private String sourceCode;
    /** 来源商品标识。 */
    private String externalProductKey;
    /** 价格序列 SHA-256。 */
    private String seriesKeySha256;
    /** 规格或等级。 */
    private String specification;
    /** 报价类型。 */
    private String quoteType;
    /** 来源原始价格。 */
    private BigDecimal sourcePrice;
    /** 来源原始单位。 */
    private String sourceUnit;
    /** 平台标准价格。 */
    private BigDecimal price;
    /** 平台标准单位。 */
    private String unit;
    /** 产地或地区。 */
    private String origin;
    /** 报价市场。 */
    private String marketName;
    /** 行情业务日期。 */
    private LocalDate quoteDate;
    /** 采集时间。 */
    private Date collectedAt;
    /** 来源地址。 */
    private String sourceUrl;
    /** 上一有效报价。 */
    private BigDecimal previousPrice;
    /** 涨跌额。 */
    private BigDecimal changeAmount;
    /** 涨跌幅，0.7 表示 0.7%。 */
    private BigDecimal changePercent;
    /** 趋势：up/down/flat/unknown。 */
    private String trend;
    /** 报价告警 JSON。 */
    private String warningsJson;
    /** 发布状态：PUBLISHED/OFFLINE。 */
    private String publishStatus;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
