package com.ym.agriculture.farming.market.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 运营修正行情暂存记录并重新处理的参数。 */
@Data
public class SfMarketCorrectionBo {
    /** 修正后的商品名称；为空表示沿用原值。 */
    @Size(max = 128)
    private String productName;
    /** 修正后的规格；传空字符串表示清空。 */
    @Size(max = 128)
    private String specification;
    /** 平台品类编码；为空表示重新按来源品类映射。 */
    private String category;
    /** 修正后的报价类型。 */
    private String quoteType;
    /** 修正后的来源价格。 */
    private BigDecimal price;
    /** 修正后的来源单位。 */
    @Size(max = 32)
    private String unit;
    /** 修正后的产地；传空字符串表示清空。 */
    @Size(max = 128)
    private String origin;
    /** 修正后的市场；传空字符串表示清空。 */
    @Size(max = 191)
    private String marketName;
    /** 修正后的行情业务日期。 */
    private LocalDate quoteDate;
    /** 修正原因和核验说明。 */
    @NotBlank
    @Size(max = 500)
    private String comment;
}
