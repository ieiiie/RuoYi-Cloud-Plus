package com.ym.agriculture.farming.tenantinit.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.tenantinit.model.entity.SfFarmWorkDictTemplate;

import java.util.List;

public interface SfFarmWorkDictTemplateMapper extends BaseMapperPlus<SfFarmWorkDictTemplate, SfFarmWorkDictTemplate> {

    default List<SfFarmWorkDictTemplate> selectEnabledTemplates() {
        return selectList(Wrappers.<SfFarmWorkDictTemplate>lambdaQuery()
            .eq(SfFarmWorkDictTemplate::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SfFarmWorkDictTemplate::getSortOrder)
            .orderByAsc(SfFarmWorkDictTemplate::getTemplateId));
    }
}
