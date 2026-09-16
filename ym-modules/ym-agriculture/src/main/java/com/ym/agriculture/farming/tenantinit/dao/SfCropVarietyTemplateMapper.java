package com.ym.agriculture.farming.tenantinit.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.tenantinit.model.entity.SfCropVarietyTemplate;

import java.util.List;

public interface SfCropVarietyTemplateMapper extends BaseMapperPlus<SfCropVarietyTemplate, SfCropVarietyTemplate> {

    default List<SfCropVarietyTemplate> selectEnabledTemplates() {
        return selectList(Wrappers.<SfCropVarietyTemplate>lambdaQuery()
            .eq(SfCropVarietyTemplate::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SfCropVarietyTemplate::getSortOrder)
            .orderByAsc(SfCropVarietyTemplate::getTemplateId));
    }
}
