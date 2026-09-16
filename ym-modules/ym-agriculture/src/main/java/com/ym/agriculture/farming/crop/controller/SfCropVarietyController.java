package com.ym.agriculture.farming.crop.controller;

import com.ym.agriculture.farming.crop.model.bo.SfCropStatusBo;
import com.ym.agriculture.farming.crop.model.bo.SfCropVarietyBo;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyExportVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyVo;
import com.ym.agriculture.farming.crop.service.ISfCropVarietyService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 作物品种管理。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/crop/variety")
public class SfCropVarietyController extends BaseController {

    private final ISfCropVarietyService varietyService;

    @GetMapping("/list")
    public R<List<SfCropVarietyVo>> list(SfCropVarietyBo bo) {
        return R.ok(varietyService.queryList(bo));
    }

    @GetMapping("/page")
    public R<PageResult<SfCropVarietyVo>> page(SfCropVarietyBo bo, PageQuery pageQuery) {
        return R.ok(varietyService.queryPageList(bo, pageQuery));
    }

    @GetMapping("/{varietyId}")
    public R<SfCropVarietyVo> getInfo(@PathVariable Long varietyId) {
        return R.ok(varietyService.queryById(varietyId));
    }

    @GetMapping("/species/{speciesId}")
    public R<List<SfCropVarietyVo>> listBySpeciesId(@PathVariable Long speciesId) {
        return R.ok(varietyService.queryListBySpeciesId(speciesId));
    }

    @Log(title = "作物品种管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SfCropVarietyBo bo) {
        return toAjax(varietyService.insertByBo(bo));
    }

    @Log(title = "作物品种管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SfCropVarietyBo bo) {
        return toAjax(varietyService.updateByBo(bo));
    }

    @Log(title = "作物品种管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PatchMapping("/{varietyId}/status")
    public R<Void> updateStatus(@PathVariable Long varietyId,
                                @Validated @RequestBody SfCropStatusBo bo) {
        return toAjax(varietyService.updateStatus(varietyId, bo.getStatus()));
    }

    @Log(title = "作物品种管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{varietyIds}")
    public R<Void> remove(@PathVariable Long[] varietyIds) {
        return toAjax(varietyService.deleteWithValidByIds(List.of(varietyIds)));
    }

    @Log(title = "作物品种管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SfCropVarietyBo bo, HttpServletResponse response) {
        List<SfCropVarietyExportVo> list = varietyService.queryExportList(bo);
        ExcelBuilder.of(list, SfCropVarietyExportVo.class).sheetName("作物品种数据").toResponse(response);
    }
}
