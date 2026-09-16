package com.ym.agriculture.farming.market.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 来源商品到平台商品的稳定映射。 */
@Data
@TableName("sf_market_product_mapping")
public class SfMarketProductMapping {
    /** 映射主键。 */
    @TableId("mapping_id")
    private Long mappingId;
    /** 来源编码。 */
    private String sourceCode;
    /** 来源商品标识。 */
    private String externalProductKey;
    /** 平台商品主键。 */
    private Long productId;
    /** 最近来源商品名称。 */
    private String lastSourceName;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
