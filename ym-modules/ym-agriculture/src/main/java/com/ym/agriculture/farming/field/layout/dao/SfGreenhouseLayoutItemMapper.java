package com.ym.agriculture.farming.field.layout.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.field.layout.model.entity.SfGreenhouseLayoutItem;

import java.util.Collection;
import java.util.List;

/**
 * 大棚布局棚位 Mapper。
 */
public interface SfGreenhouseLayoutItemMapper extends BaseMapper<SfGreenhouseLayoutItem> {

    default List<SfGreenhouseLayoutItem> selectByLayoutId(Long layoutId) {
        return selectList(Wrappers.lambdaQuery(SfGreenhouseLayoutItem.class)
            .eq(SfGreenhouseLayoutItem::getLayoutId, layoutId)
            .orderByAsc(SfGreenhouseLayoutItem::getColumnId)
            .orderByAsc(SfGreenhouseLayoutItem::getRowOrder));
    }

    default int deleteByLayoutId(Long layoutId) {
        return delete(Wrappers.lambdaQuery(SfGreenhouseLayoutItem.class)
            .eq(SfGreenhouseLayoutItem::getLayoutId, layoutId));
    }

    default int deleteByFieldIds(Collection<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return 0;
        }
        return delete(Wrappers.lambdaQuery(SfGreenhouseLayoutItem.class)
            .in(SfGreenhouseLayoutItem::getFieldId, fieldIds));
    }
}
