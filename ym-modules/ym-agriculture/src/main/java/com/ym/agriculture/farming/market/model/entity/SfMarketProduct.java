package com.ym.agriculture.farming.market.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 农业行情平台商品。 */
@Data
@TableName("sf_market_product")
public class SfMarketProduct {
    /** 商品主键。 */
    @TableId("product_id")
    private Long productId;
    /** 商品展示名称。 */
    private String productName;
    /** 标准化匹配名称。 */
    private String normalizedName;
    /** 商品品类。 */
    private String category;
    /** 展示排序。 */
    private Integer sortOrder;
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
