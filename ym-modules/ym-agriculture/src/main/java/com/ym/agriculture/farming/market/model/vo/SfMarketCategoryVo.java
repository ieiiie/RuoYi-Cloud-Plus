package com.ym.agriculture.farming.market.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 移动端农业行情品类。 */
@Data
@AllArgsConstructor
public class SfMarketCategoryVo {
    /** 品类编码。 */
    private String category;
    /** 品类名称。 */
    private String categoryName;
    /** 展示排序。 */
    private int sort;
}
