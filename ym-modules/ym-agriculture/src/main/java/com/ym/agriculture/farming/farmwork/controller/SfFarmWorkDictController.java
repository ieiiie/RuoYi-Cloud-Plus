package com.ym.agriculture.farming.farmwork.controller;

import com.ym.common.core.domain.R;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.farmwork.model.bo.SfFarmWorkDictBo;
import com.ym.agriculture.farming.farmwork.model.bo.SfFarmWorkDictSortBo;
import com.ym.agriculture.farming.farmwork.model.vo.SfFarmWorkDictTreeVo;
import com.ym.agriculture.farming.farmwork.model.vo.SfFarmWorkDictVo;
import com.ym.agriculture.farming.farmwork.service.ISfFarmWorkDictService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * stask 任务派发农事分类与项目字典。
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/stask/farm-work")
public class SfFarmWorkDictController extends BaseController {

    private final ISfFarmWorkDictService farmWorkDictService;

    /**
     * 获取农事分类与项目完整树。
     *
     * @return 统一响应，{@code data} 为分类树，分类节点 {@code children} 为项目列表
     */
    @GetMapping("/tree")
    public R<List<SfFarmWorkDictTreeVo>> tree() {
        return R.ok(farmWorkDictService.tree());
    }

    /**
     * 获取农事字典节点详情。
     *
     * @param dictId 农事字典主键
     * @return 统一响应，{@code data} 为节点详情
    */
    @GetMapping("/{dictId}")
    public R<SfFarmWorkDictVo> get(@NotNull @PathVariable Long dictId) {
        return R.ok(farmWorkDictService.get(dictId));
    }

    /**
     * 校验编码是否可用。
     *
     * @param code      编码，分类与项目在同一租户内共用唯一命名空间
     * @param excludeId 编辑时排除的农事字典主键
     * @return 统一响应，{@code data=true} 表示可用
     */
    @GetMapping("/check-code")
    public R<Boolean> checkCode(@NotBlank @RequestParam("code") String code,
        @RequestParam(value = "excludeId", required = false) Long excludeId) {
        return R.ok(farmWorkDictService.checkCodeUnique(code, excludeId));
    }

    /**
     * 校验名称是否可用。
     *
     * @param name      名称
     * @param nodeType  节点类型：CATEGORY-分类，ITEM-项目
     * @param parentId  父节点ID；项目传所属分类ID时按同分类校验
     * @param excludeId 编辑时排除的农事字典主键
     * @return 统一响应，{@code data=true} 表示可用
     */
    @GetMapping("/check-name")
    public R<Boolean> checkName(@NotBlank @RequestParam("name") String name,
        @NotBlank @RequestParam("nodeType") String nodeType,
        @RequestParam(value = "parentId", required = false) Long parentId,
        @RequestParam(value = "excludeId", required = false) Long excludeId) {
        return R.ok(farmWorkDictService.checkNameUnique(name, nodeType, parentId, excludeId));
    }

    /**
     * 新增农事分类或农事项目。
     *
     * @param bo 新增入参；分类 {@code parentId=0}，项目 {@code parentId} 为所属分类ID
     * @return 统一响应，成功无 {@code data} 体
     */
    @Log(title = "stask农事字典", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SfFarmWorkDictBo bo) {
        return toAjax(farmWorkDictService.add(bo));
    }

    /**
     * 编辑农事分类或农事项目。
     *
     * @param dictId 农事字典主键
     * @param bo     编辑入参
     * @return 统一响应，成功无 {@code data} 体
     */
    @Log(title = "stask农事字典", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/{dictId}")
    public R<Void> edit(@NotNull @PathVariable Long dictId,
        @Validated(EditGroup.class) @RequestBody SfFarmWorkDictBo bo) {
        bo.setDictId(dictId);
        return toAjax(farmWorkDictService.update(bo));
    }

    /**
     * 删除农事分类或农事项目。
     *
     * @param dictId 农事字典主键；分类下存在项目时不允许删除
     * @return 统一响应，成功无 {@code data} 体
     */
    @Log(title = "stask农事字典", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/{dictId}")
    public R<Void> remove(@NotNull @PathVariable Long dictId) {
        return toAjax(farmWorkDictService.remove(dictId));
    }

    /**
     * 同级批量更新排序。
     *
     * @param sortList 排序列表；只能传同一分类下的同类型节点
     * @return 统一响应，成功无 {@code data} 体
     */
    @Log(title = "stask农事字典", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/sort")
    public R<Void> sort(@NotEmpty @Valid @RequestBody List<SfFarmWorkDictSortBo> sortList) {
        return toAjax(farmWorkDictService.sort(sortList));
    }

}
