package com.ym.agriculture.farming.field.layout.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.field.layout.model.entity.SfGreenhouseLayoutColumn;

import java.util.List;

/**
 * 大棚布局列 Mapper。
 */
public interface SfGreenhouseLayoutColumnMapper extends BaseMapper<SfGreenhouseLayoutColumn> {

    default List<SfGreenhouseLayoutColumn> selectByLayoutId(Long layoutId) {
        return selectList(Wrappers.lambdaQuery(SfGreenhouseLayoutColumn.class)
            .eq(SfGreenhouseLayoutColumn::getLayoutId, layoutId)
            .orderByAsc(SfGreenhouseLayoutColumn::getColumnOrder));
    }

    default int deleteByLayoutId(Long layoutId) {
        return delete(Wrappers.lambdaQuery(SfGreenhouseLayoutColumn.class)
            .eq(SfGreenhouseLayoutColumn::getLayoutId, layoutId));
    }
}
