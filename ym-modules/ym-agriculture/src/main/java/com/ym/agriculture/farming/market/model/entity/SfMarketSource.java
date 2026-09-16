package com.ym.agriculture.farming.market.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 农业行情来源白名单。 */
@Data
@TableName("sf_market_source")
public class SfMarketSource {
    /** 来源编码。 */
    @TableId("source_code")
    private String sourceCode;
    /** 展示名称。 */
    private String sourceName;
    /** 允许来源域名 JSON。 */
    private String allowedDomainsJson;
    /** 授权说明。 */
    private String authorizationNote;
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
