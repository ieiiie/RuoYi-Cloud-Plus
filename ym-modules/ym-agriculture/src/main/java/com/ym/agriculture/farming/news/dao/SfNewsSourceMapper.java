package com.ym.agriculture.farming.news.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.news.model.entity.SfNewsSource;

/** 平台全局农业资讯来源白名单 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfNewsSourceMapper extends BaseMapperPlus<SfNewsSource, SfNewsSource> {

    /** 按编码读取启用来源。 */
    default SfNewsSource selectEnabledByCode(String sourceCode) {
        return selectOne(Wrappers.<SfNewsSource>lambdaQuery()
            .eq(SfNewsSource::getSourceCode, sourceCode)
            .eq(SfNewsSource::getEnabledFlag, true)
            .last("limit 1"));
    }
}
