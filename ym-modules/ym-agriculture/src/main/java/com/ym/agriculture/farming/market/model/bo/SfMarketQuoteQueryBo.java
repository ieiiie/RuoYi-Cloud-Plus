package com.ym.agriculture.farming.market.model.bo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 农业行情正式报价分页条件。 */
@Data
public class SfMarketQuoteQueryBo {
    /** 商品名称、规格、产地或市场关键词。 */
    private String keyword;
    /** 商品品类编码。 */
    private String category;
    /** 来源编码。 */
    private String sourceCode;
    /** 报价类型：MARKET/OFFICIAL_AVERAGE。 */
    private String quoteType;
    /** 市场名称。 */
    private String marketName;
    /** 行情业务日期，格式 yyyy-MM-dd。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate quoteDate;
}
