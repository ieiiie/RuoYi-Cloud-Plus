package com.ym.agriculture.farming.tenantinit.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.tenantinit.model.entity.SfCropSpeciesTemplate;

import java.util.List;

public interface SfCropSpeciesTemplateMapper extends BaseMapperPlus<SfCropSpeciesTemplate, SfCropSpeciesTemplate> {

    default List<SfCropSpeciesTemplate> selectEnabledTemplates() {
        return selectList(Wrappers.<SfCropSpeciesTemplate>lambdaQuery()
            .eq(SfCropSpeciesTemplate::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SfCropSpeciesTemplate::getSortOrder)
            .orderByAsc(SfCropSpeciesTemplate::getTemplateId));
    }
}
