package com.ym.agriculture.farmtask.workorder.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * stask 任务包农事项明细 Mapper。
 */
@Mapper
public interface SfStaskWorkOrderItemMapper extends BaseMapperPlus<SfStaskWorkOrderItem, SfStaskWorkOrderItem> {

    /**
     * 查询任务包农事项明细。
     *
     * @param tenantId  租户编号
     * @param packageId 任务包ID
     * @return 农事项明细
     */
    default List<SfStaskWorkOrderItem> selectByPackageId(String tenantId, Long packageId) {
        return selectList(Wrappers.<SfStaskWorkOrderItem>lambdaQuery()
            .eq(SfStaskWorkOrderItem::getTenantId, tenantId)
            .eq(SfStaskWorkOrderItem::getPackageId, packageId)
            .orderByAsc(SfStaskWorkOrderItem::getSortOrder)
            .orderByAsc(SfStaskWorkOrderItem::getItemId));
    }

    /**
     * 批量查询多个任务包的农事项明细。
     *
     * @param tenantId  租户编号
     * @param packageIds 任务包ID集合
     * @return 农事项明细列表
     */
    default List<SfStaskWorkOrderItem> selectByPackageIds(String tenantId, Collection<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrderItem>lambdaQuery()
            .eq(SfStaskWorkOrderItem::getTenantId, tenantId)
            .in(SfStaskWorkOrderItem::getPackageId, packageIds)
            .orderByAsc(SfStaskWorkOrderItem::getPackageId)
            .orderByAsc(SfStaskWorkOrderItem::getSortOrder)
            .orderByAsc(SfStaskWorkOrderItem::getItemId));
    }

    /**
     * 物理删除任务包农事项明细。
     *
     * @param tenantId  租户编号
     * @param packageId 任务包ID
     * @return 删除行数
     */
    default int deleteByPackageId(String tenantId, Long packageId) {
        return delete(Wrappers.<SfStaskWorkOrderItem>lambdaQuery()
            .eq(SfStaskWorkOrderItem::getTenantId, tenantId)
            .eq(SfStaskWorkOrderItem::getPackageId, packageId));
    }
}
