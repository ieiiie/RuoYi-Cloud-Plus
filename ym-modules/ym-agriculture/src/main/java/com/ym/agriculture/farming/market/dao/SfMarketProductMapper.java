package com.ym.agriculture.farming.market.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.market.model.entity.SfMarketProduct;

/** 农业行情平台商品 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfMarketProductMapper extends BaseMapperPlus<SfMarketProduct, SfMarketProduct> {

    /** 按品类和标准化名称查询平台商品。 */
    default SfMarketProduct selectByCategoryAndName(String category, String normalizedName) {
        return selectOne(Wrappers.<SfMarketProduct>lambdaQuery()
            .eq(SfMarketProduct::getCategory, category)
            .eq(SfMarketProduct::getNormalizedName, normalizedName)
            .last("limit 1"));
    }
}
