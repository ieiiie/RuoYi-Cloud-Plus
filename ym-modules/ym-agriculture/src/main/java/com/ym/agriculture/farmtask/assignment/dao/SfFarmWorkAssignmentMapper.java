package com.ym.agriculture.farmtask.assignment.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.assignment.model.entity.SfFarmWorkAssignment;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * stask 农事分配 Mapper。
 */
@Mapper
public interface SfFarmWorkAssignmentMapper extends BaseMapperPlus<SfFarmWorkAssignment, SfFarmWorkAssignment> {

    /**
     * 查询租户内多个大棚的分配记录。
     *
     * @param tenantId      租户编号
     * @param greenhouseIds 大棚ID集合
     * @return 分配记录列表
     */
    default List<SfFarmWorkAssignment> selectByGreenhouseIds(String tenantId, Collection<Long> greenhouseIds) {
        if (greenhouseIds == null || greenhouseIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .in(SfFarmWorkAssignment::getGreenhouseId, greenhouseIds));
    }

    /**
     * 批量查询指定大棚和农事项范围内的分配记录。
     *
     * @param tenantId      租户编号
     * @param greenhouseIds 大棚ID集合
     * @param workItemIds   农事项ID集合
     * @return 分配记录列表
     */
    default List<SfFarmWorkAssignment> selectByGreenhousesAndWorkItems(String tenantId,
        Collection<Long> greenhouseIds, Collection<Long> workItemIds) {
        if (greenhouseIds == null || greenhouseIds.isEmpty() || workItemIds == null || workItemIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .in(SfFarmWorkAssignment::getGreenhouseId, greenhouseIds)
            .in(SfFarmWorkAssignment::getWorkItemId, workItemIds));
    }

    /**
     * 查询租户内指定大棚的分配记录。
     *
     * @param tenantId     租户编号
     * @param greenhouseId 大棚ID
     * @return 分配记录列表
     */
    default List<SfFarmWorkAssignment> selectByGreenhouseId(String tenantId, Long greenhouseId) {
        return selectList(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getGreenhouseId, greenhouseId)
            .orderByAsc(SfFarmWorkAssignment::getLeaderId)
            .orderByAsc(SfFarmWorkAssignment::getAssignedAt));
    }

    /**
     * 查询指定组长管理的农事分配。
     *
     * @param tenantId 租户编号
     * @param leaderId 组长员工ID
     * @return 农事分配记录列表
     */
    default List<SfFarmWorkAssignment> selectByLeaderId(String tenantId, Long leaderId) {
        if (leaderId == null) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getLeaderId, leaderId)
            .orderByAsc(SfFarmWorkAssignment::getWorkItemId)
            .orderByAsc(SfFarmWorkAssignment::getGreenhouseId));
    }

    /**
     * 查询指定农事项目的分配快照。
     *
     * @param tenantId 租户编号
     * @param workItemId 农事项目编号
     * @return 分配记录
     */
    default List<SfFarmWorkAssignment> selectByWorkItemId(String tenantId, Long workItemId) {
        if (workItemId == null) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getWorkItemId, workItemId));
    }

    /**
     * 查询指定农事分类的分配快照。
     *
     * @param tenantId 租户编号
     * @param categoryId 农事分类编号
     * @return 分配记录
     */
    default List<SfFarmWorkAssignment> selectByCategoryIdSnapshot(String tenantId, Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getCategoryIdSnapshot, categoryId));
    }

    /**
     * 判断组长是否仍存在农事分配记录。
     *
     * @param tenantId 租户编号
     * @param leaderId 组长员工ID
     * @return 存在时返回 true
     */
    default boolean existsByLeaderId(String tenantId, Long leaderId) {
        if (leaderId == null) {
            return false;
        }
        return selectCount(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getLeaderId, leaderId)) > 0;
    }

    /**
     * 查询指定大棚下已分配的农事项。
     *
     * @param tenantId     租户编号
     * @param greenhouseId 大棚ID
     * @param workItemIds  农事项目ID集合
     * @return 已存在的分配记录
     */
    default List<SfFarmWorkAssignment> selectExistingWorkItems(String tenantId, Long greenhouseId, Collection<Long> workItemIds) {
        if (workItemIds == null || workItemIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getGreenhouseId, greenhouseId)
            .in(SfFarmWorkAssignment::getWorkItemId, workItemIds));
    }

    /**
     * 查询指定农事项已分配组长的大棚 ID 列表。
     *
     * @param tenantId   租户编号
     * @param workItemId 农事项目ID
     * @return 大棚 ID 列表，去重
     */
    default List<Long> selectGreenhouseIdsByWorkItem(String tenantId, Long workItemId) {
        if (workItemId == null) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
                .eq(SfFarmWorkAssignment::getTenantId, tenantId)
                .eq(SfFarmWorkAssignment::getWorkItemId, workItemId)
                .select(SfFarmWorkAssignment::getGreenhouseId))
            .stream()
            .map(SfFarmWorkAssignment::getGreenhouseId)
            .filter(id -> id != null)
            .distinct()
            .toList();
    }

    /**
     * 查询指定大棚和农事项的组长分配。
     *
     * @param tenantId     租户编号
     * @param greenhouseId 大棚ID
     * @param workItemId   农事项目ID
     * @return 分配记录，不存在返回 null
     */
    default SfFarmWorkAssignment selectByGreenhouseAndWorkItem(String tenantId, Long greenhouseId, Long workItemId) {
        return selectOne(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getGreenhouseId, greenhouseId)
            .eq(SfFarmWorkAssignment::getWorkItemId, workItemId)
            .last("limit 1"));
    }

    /**
     * 物理删除指定租户下的分配记录。
     *
     * @param tenantId     租户编号
     * @param assignmentId 分配记录ID
     * @return 删除行数
     */
    default int deleteNormalById(String tenantId, Long assignmentId) {
        return delete(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getAssignmentId, assignmentId));
    }

    /**
     * 物理删除指定组长在指定大棚下的多条分配记录。
     *
     * @param tenantId      租户编号
     * @param greenhouseId  大棚ID
     * @param leaderId      组长人员ID
     * @param assignmentIds 分配记录ID集合
     * @return 删除行数
     */
    default int deleteBatchByScope(String tenantId, Long greenhouseId, Long leaderId, Collection<Long> assignmentIds) {
        if (assignmentIds == null || assignmentIds.isEmpty()) {
            return 0;
        }
        return delete(Wrappers.<SfFarmWorkAssignment>lambdaQuery()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getGreenhouseId, greenhouseId)
            .eq(SfFarmWorkAssignment::getLeaderId, leaderId)
            .in(SfFarmWorkAssignment::getAssignmentId, assignmentIds));
    }

    /**
     * 同步已分配记录的农事项目快照。
     *
     * @param tenantId     租户编号
     * @param workItemId   农事项目ID
     * @param itemName     农事项目名称
     * @param itemCode     农事项目编码
     * @param categoryId   农事分类ID
     * @param categoryName 农事分类名称
     * @return 更新行数
     */
    default int updateWorkItemSnapshot(String tenantId, Long workItemId, String itemName, String itemCode,
        Long categoryId, String categoryName) {
        return update(null, Wrappers.<SfFarmWorkAssignment>lambdaUpdate()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getWorkItemId, workItemId)
            .set(SfFarmWorkAssignment::getWorkItemNameSnapshot, itemName)
            .set(SfFarmWorkAssignment::getWorkItemCodeSnapshot, itemCode)
            .set(SfFarmWorkAssignment::getCategoryIdSnapshot, categoryId)
            .set(SfFarmWorkAssignment::getCategoryNameSnapshot, categoryName));
    }

    /**
     * 同步已分配记录的农事分类名称快照。
     *
     * @param tenantId     租户编号
     * @param categoryId   农事分类ID
     * @param categoryName 农事分类名称
     * @return 更新行数
     */
    default int updateCategorySnapshot(String tenantId, Long categoryId, String categoryName) {
        return update(null, Wrappers.<SfFarmWorkAssignment>lambdaUpdate()
            .eq(SfFarmWorkAssignment::getTenantId, tenantId)
            .eq(SfFarmWorkAssignment::getCategoryIdSnapshot, categoryId)
            .set(SfFarmWorkAssignment::getCategoryNameSnapshot, categoryName));
    }
}
