package com.ym.agriculture.farming.solarterms.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.solarterms.model.entity.SfSolarTermDefinition;

import java.util.List;

/** {@code sf_solar_term_definition} Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfSolarTermDefinitionMapper extends BaseMapper<SfSolarTermDefinition> {

    default List<SfSolarTermDefinition> selectEnabledOrdered() {
        return selectList(Wrappers.<SfSolarTermDefinition>lambdaQuery()
            .eq(SfSolarTermDefinition::getEnabledFlag, 1)
            .orderByAsc(SfSolarTermDefinition::getTermOrder));
    }

    default SfSolarTermDefinition selectEnabledByCode(String termCode) {
        return selectOne(Wrappers.<SfSolarTermDefinition>lambdaQuery()
            .eq(SfSolarTermDefinition::getTermCode, termCode)
            .eq(SfSolarTermDefinition::getEnabledFlag, 1)
            .last("LIMIT 1"));
    }
}
