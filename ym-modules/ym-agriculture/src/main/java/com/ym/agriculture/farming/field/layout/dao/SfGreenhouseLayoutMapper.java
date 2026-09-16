package com.ym.agriculture.farming.field.layout.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.field.layout.model.entity.SfGreenhouseLayout;

/**
 * 大棚布局根记录 Mapper。
 */
public interface SfGreenhouseLayoutMapper extends BaseMapper<SfGreenhouseLayout> {

    default SfGreenhouseLayout selectCurrent() {
        return selectOne(Wrappers.lambdaQuery(SfGreenhouseLayout.class));
    }

    default SfGreenhouseLayout selectCurrentForUpdate() {
        return selectOne(Wrappers.lambdaQuery(SfGreenhouseLayout.class).last("FOR UPDATE"));
    }
}
