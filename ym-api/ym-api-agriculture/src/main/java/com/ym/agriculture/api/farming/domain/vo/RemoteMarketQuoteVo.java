package com.ym.agriculture.api.farming.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/** 农业行情报价。 */
@Data
public class RemoteMarketQuoteVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long quoteId;
    private Long productId;
    private String productName;
    private String specification;
    private String displayName;
    private String category;
    private String categoryName;
    private Double price;
    private String unit;
    private Double previousPrice;
    private Double changeAmount;
    private Double changePercent;
    private String trend;
    private String quoteType;
    private String origin;
    private String marketName;
    private LocalDate quoteDate;
    private Date collectedAt;
    private String sourceCode;
    private String sourceName;
    private String sourceUrl;
    private String warningsJson;
    private Boolean warningAcknowledged;
}
