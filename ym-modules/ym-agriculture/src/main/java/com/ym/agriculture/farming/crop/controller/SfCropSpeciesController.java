package com.ym.agriculture.farming.crop.controller;

import com.ym.agriculture.farming.crop.model.bo.SfCropSpeciesBo;
import com.ym.agriculture.farming.crop.model.bo.SfCropStatusBo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesDetailVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesExportVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesTreeVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesVo;
import com.ym.agriculture.farming.crop.service.ISfCropSpeciesService;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.excel.utils.ExcelBuilder;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.web.core.BaseController;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 作物品类管理。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/crop/species")
public class SfCropSpeciesController extends BaseController {

    private final ISfCropSpeciesService speciesService;

    @GetMapping("/list")
    public R<List<SfCropSpeciesVo>> list(SfCropSpeciesBo bo) {
        return R.ok(speciesService.queryList(bo));
    }

    @GetMapping("/tree")
    public R<List<SfCropSpeciesTreeVo>> tree(SfCropSpeciesBo bo,
                                              @RequestParam(required = false) String varietyStatus) {
        return R.ok(speciesService.queryTree(bo, varietyStatus));
    }

    @GetMapping("/page")
    public R<PageResult<SfCropSpeciesVo>> page(SfCropSpeciesBo bo, PageQuery pageQuery) {
        return R.ok(speciesService.queryPageList(bo, pageQuery));
    }

    @GetMapping("/{speciesId}")
    public R<SfCropSpeciesVo> getInfo(@PathVariable Long speciesId) {
        return R.ok(speciesService.queryById(speciesId));
    }

    @GetMapping("/{speciesId}/detail")
    public R<SfCropSpeciesDetailVo> getDetail(@PathVariable Long speciesId) {
        return R.ok(speciesService.queryDetailById(speciesId));
    }

    @Log(title = "作物品类管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SfCropSpeciesBo bo) {
        return toAjax(speciesService.insertByBo(bo));
    }

    @Log(title = "作物品类管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SfCropSpeciesBo bo) {
        return toAjax(speciesService.updateByBo(bo));
    }

    @Log(title = "作物品类管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PatchMapping("/{speciesId}/status")
    public R<Void> updateStatus(@PathVariable Long speciesId,
                                @Validated @RequestBody SfCropStatusBo bo) {
        return toAjax(speciesService.updateStatus(speciesId, bo.getStatus()));
    }

    @Log(title = "作物品类管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{speciesIds}")
    public R<Void> remove(@PathVariable Long[] speciesIds) {
        return toAjax(speciesService.deleteWithValidByIds(List.of(speciesIds)));
    }

    @Log(title = "作物品类管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SfCropSpeciesBo bo, HttpServletResponse response) {
        List<SfCropSpeciesExportVo> list = speciesService.queryExportList(bo);
        ExcelBuilder.of(list, SfCropSpeciesExportVo.class).sheetName("作物品类数据").toResponse(response);
    }
}
