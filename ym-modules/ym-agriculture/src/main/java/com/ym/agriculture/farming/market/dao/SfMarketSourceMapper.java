package com.ym.agriculture.farming.market.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.market.model.entity.SfMarketSource;

/** 农业行情来源 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfMarketSourceMapper extends BaseMapperPlus<SfMarketSource, SfMarketSource> {
}
