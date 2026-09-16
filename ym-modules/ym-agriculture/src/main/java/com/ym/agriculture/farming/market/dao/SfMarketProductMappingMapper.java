package com.ym.agriculture.farming.market.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.market.model.entity.SfMarketProductMapping;

/** 农业行情来源商品映射 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfMarketProductMappingMapper extends BaseMapperPlus<SfMarketProductMapping, SfMarketProductMapping> {

    /** 按来源及来源商品标识查询映射。 */
    default SfMarketProductMapping selectBySourceKey(String sourceCode, String externalProductKey) {
        return selectOne(Wrappers.<SfMarketProductMapping>lambdaQuery()
            .eq(SfMarketProductMapping::getSourceCode, sourceCode)
            .eq(SfMarketProductMapping::getExternalProductKey, externalProductKey)
            .last("limit 1"));
    }
}
