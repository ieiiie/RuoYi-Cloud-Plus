package com.ym.agriculture.farming.farmwork.dao;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.farmwork.model.constants.FarmWorkNodeType;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farming.farmwork.model.vo.SfFarmWorkDictVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * stask 农事字典 Mapper。
 *
 * @author ym-cloud
 */
@Mapper
public interface SfFarmWorkDictMapper extends BaseMapperPlus<SfFarmWorkDict, SfFarmWorkDictVo> {

    /**
     * 查询租户内全部未删除节点。
     *
     * @param tenantId 租户编号
     * @return 节点列表
     */
    default List<SfFarmWorkDict> selectNormalList(String tenantId) {
        return selectList(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SfFarmWorkDict::getParentId)
            .orderByAsc(SfFarmWorkDict::getSortOrder)
            .orderByAsc(SfFarmWorkDict::getDictId));
    }

    /**
     * 查询租户内未删除节点。
     *
     * @param tenantId 租户编号
     * @param dictId   农事字典主键
     * @return 节点实体
     */
    default SfFarmWorkDict selectNormalById(String tenantId, Long dictId) {
        return selectOne(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getDictId, dictId)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL));
    }

    /**
     * 查询租户内多个未删除节点。
     *
     * @param tenantId 租户编号
     * @param dictIds  农事字典主键集合
     * @return 节点列表
     */
    default List<SfFarmWorkDict> selectNormalByIds(String tenantId, Collection<Long> dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .in(SfFarmWorkDict::getDictId, dictIds)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL));
    }

    /**
     * 查询存在启用项目的农事分类。
     *
     * @param tenantId    租户编号
     * @param categoryIds 分类主键集合
     * @return 分类节点列表
     */
    default List<SfFarmWorkDict> selectMobileCategories(String tenantId, Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .in(SfFarmWorkDict::getDictId, categoryIds)
            .eq(SfFarmWorkDict::getNodeType, FarmWorkNodeType.CATEGORY.name())
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SfFarmWorkDict::getSortOrder)
            .orderByAsc(SfFarmWorkDict::getDictId));
    }

    /**
     * 查询租户内全部启用农事项目。
     *
     * @param tenantId 租户编号
     * @return 启用项目列表
     */
    default List<SfFarmWorkDict> selectMobileEnabledItems(String tenantId) {
        return selectList(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getNodeType, FarmWorkNodeType.ITEM.name())
            .eq(SfFarmWorkDict::getStatus, SystemConstants.NORMAL)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SfFarmWorkDict::getParentId)
            .orderByAsc(SfFarmWorkDict::getSortOrder)
            .orderByAsc(SfFarmWorkDict::getDictId));
    }

    /**
     * 查询指定分类下启用农事项目。
     *
     * @param tenantId   租户编号
     * @param categoryId 分类主键
     * @return 启用项目列表
     */
    default List<SfFarmWorkDict> selectMobileEnabledItemsByCategoryId(String tenantId, Long categoryId) {
        return selectList(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getParentId, categoryId)
            .eq(SfFarmWorkDict::getNodeType, FarmWorkNodeType.ITEM.name())
            .eq(SfFarmWorkDict::getStatus, SystemConstants.NORMAL)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SfFarmWorkDict::getSortOrder)
            .orderByAsc(SfFarmWorkDict::getDictId));
    }

    /**
     * 判断租户内编码是否已存在。
     *
     * @param tenantId      租户编号
     * @param dictCode      编码
     * @param excludeDictId 排除的节点主键，新增时为空
     * @return 是否存在
     */
    default boolean existsTenantCode(String tenantId, String dictCode, Long excludeDictId) {
        return exists(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getDictCode, dictCode)
            .ne(excludeDictId != null, SfFarmWorkDict::getDictId, excludeDictId)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL));
    }

    default SfFarmWorkDict selectTenantByDictCode(String tenantId, String dictCode) {
        if (com.ym.common.core.utils.StringUtils.isBlank(tenantId)
            || com.ym.common.core.utils.StringUtils.isBlank(dictCode)) {
            return null;
        }
        return selectOne(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getDictCode, dictCode)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
            .last("LIMIT 1"));
    }

    /**
     * 判断租户内同类型节点名称是否已存在。
     *
     * @param tenantId      租户编号
     * @param dictName      名称
     * @param nodeType      节点类型：CATEGORY 或 ITEM
     * @param excludeDictId 排除的节点主键，新增时为空
     * @return 是否存在
     */
    default boolean existsTenantName(String tenantId, String dictName, String nodeType, Long excludeDictId) {
        return exists(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getNodeType, nodeType)
            .eq(SfFarmWorkDict::getDictName, dictName)
            .ne(excludeDictId != null, SfFarmWorkDict::getDictId, excludeDictId)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL));
    }

    /**
     * 判断同一父节点下同类型节点名称是否已存在。
     *
     * @param tenantId      租户编号
     * @param parentId      父节点ID
     * @param dictName      名称
     * @param nodeType      节点类型：CATEGORY 或 ITEM
     * @param excludeDictId 排除的节点主键，新增时为空
     * @return 是否存在
     */
    default boolean existsSiblingName(String tenantId, Long parentId, String dictName, String nodeType, Long excludeDictId) {
        return exists(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getParentId, parentId)
            .eq(SfFarmWorkDict::getNodeType, nodeType)
            .eq(SfFarmWorkDict::getDictName, dictName)
            .ne(excludeDictId != null, SfFarmWorkDict::getDictId, excludeDictId)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL));
    }

    /**
     * 统计分类下未删除项目数量。
     *
     * @param tenantId 租户编号
     * @param parentId 分类主键
     * @return 子项目数量
     */
    default long countNormalChildren(String tenantId, Long parentId) {
        return selectCount(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getParentId, parentId)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL));
    }

    /**
     * 查询同级最大排序值。
     *
     * @param tenantId 租户编号
     * @param parentId 父节点ID
     * @return 最大排序值；无节点时返回 0
     */
    default Integer selectMaxSortOrder(String tenantId, Long parentId) {
        SfFarmWorkDict row = selectOne(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .select(SfFarmWorkDict::getSortOrder)
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getParentId, parentId)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
            .orderByDesc(SfFarmWorkDict::getSortOrder)
            .last("LIMIT 1"));
        return row == null || row.getSortOrder() == null ? 0 : row.getSortOrder();
    }

    /**
     * 更新节点排序值。
     *
     * @param tenantId  租户编号
     * @param dictId    农事字典主键
     * @param sortOrder 排序序号
     * @return 是否成功
     */
    default boolean updateSortOrder(String tenantId, Long dictId, Integer sortOrder) {
        LambdaUpdateWrapper<SfFarmWorkDict> uw = Wrappers.lambdaUpdate();
        uw.eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getDictId, dictId)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
            .set(SfFarmWorkDict::getSortOrder, sortOrder);
        return update(null, uw) > 0;
    }

    /**
     * 清空农事项目自定义表单模板。
     *
     * @param tenantId 租户编号
     * @param dictId   农事字典主键
     * @return 是否成功
     */
    default boolean clearCustomFormTemplateJson(String tenantId, Long dictId) {
        LambdaUpdateWrapper<SfFarmWorkDict> uw = Wrappers.lambdaUpdate();
        uw.eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getDictId, dictId)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
            .set(SfFarmWorkDict::getCustomFormTemplateJson, null);
        return update(null, uw) > 0;
    }
}
