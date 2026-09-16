package com.ym.agriculture.farming.farmwork.service;

import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.agriculture.farming.farmwork.model.bo.SfFarmWorkDictBo;
import com.ym.agriculture.farming.farmwork.model.bo.SfFarmWorkDictSortBo;
import com.ym.agriculture.farming.farmwork.model.vo.SfFarmWorkDictTreeVo;
import com.ym.agriculture.farming.farmwork.model.vo.SfFarmWorkDictVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * stask 农事字典服务。
 *
 * @author ym-cloud
 */
@Validated
public interface ISfFarmWorkDictService {

    /**
     * 查询分类与项目完整树。
     *
     * @return 农事字典树
     */
    List<SfFarmWorkDictTreeVo> tree();

    /**
     * 查询小程序可选农事字典树（仅含已启用的农事项目，无项目的分类不返回）。
     *
     * @return 农事字典树
     */
    List<SfFarmWorkDictTreeVo> miniappTree();

    /**
     * 查询移动端可选农事分类列表（仅返回存在启用项目的分类）。
     *
     * @return 农事分类列表
     */
    List<SfFarmWorkDictVo> mobileCategories();

    /**
     * 查询移动端指定分类下启用农事项目列表。
     *
     * @param categoryId 农事分类主键
     * @return 启用农事项目列表
     */
    List<SfFarmWorkDictVo> mobileItems(@NotNull Long categoryId);

    /**
     * 查询单个节点详情。
     *
     * @param dictId 农事字典主键
     * @return 节点详情
     */
    SfFarmWorkDictVo get(@NotNull Long dictId);

    /**
     * 新增分类或项目。
     *
     * @param bo 新增入参
     * @return 是否成功
     */
    boolean add(@Validated(AddGroup.class) SfFarmWorkDictBo bo);

    /**
     * 编辑分类或项目。
     *
     * @param bo 编辑入参
     * @return 是否成功
     */
    boolean update(@Validated(EditGroup.class) SfFarmWorkDictBo bo);

    /**
     * 删除分类或项目。
     *
     * @param dictId 农事字典主键
     * @return 是否成功
     */
    boolean remove(@NotNull Long dictId);

    /**
     * 同级批量排序。
     *
     * @param sortList 排序入参
     * @return 是否成功
     */
    boolean sort(@NotEmpty List<@Valid SfFarmWorkDictSortBo> sortList);

    /**
     * 校验编码在当前租户内是否可用。
     *
     * @param dictCode      编码
     * @param excludeDictId 编辑时排除的节点主键
     * @return 是否可用
     */
    boolean checkCodeUnique(@NotBlank String dictCode, Long excludeDictId);

    /**
     * 校验名称是否可用；分类按租户同类型唯一，项目按同一分类内唯一。
     *
     * @param dictName      名称
     * @param nodeType      节点类型：CATEGORY 或 ITEM
     * @param parentId      父节点ID；项目传所属分类ID时按同分类校验
     * @param excludeDictId 编辑时排除的节点主键
     * @return 是否可用
     */
    boolean checkNameUnique(@NotBlank String dictName, @NotBlank String nodeType, Long parentId, Long excludeDictId);
}
