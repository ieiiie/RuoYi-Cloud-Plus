package com.ym.agriculture.farming.batch.controller;

import com.ym.agriculture.farming.batch.model.bo.SfPlantingBatchBo;
import com.ym.agriculture.farming.batch.model.bo.SfDetectorInfoBo;
import com.ym.agriculture.farming.batch.model.bo.SfPlantingBatchTransitionBo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchCalendarDayVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchDetailVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchExportVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.batch.model.vo.SfDetectorInfoVo;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 种植批次管理。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/batch")
public class SfPlantingBatchController extends BaseController {

    private final ISfPlantingBatchService batchService;

    @GetMapping("/list")
    public R<List<SfPlantingBatchVo>> list(SfPlantingBatchBo bo) {
        return R.ok(batchService.queryList(bo));
    }

    @GetMapping("/page")
    public R<PageResult<SfPlantingBatchVo>> page(SfPlantingBatchBo bo, PageQuery pageQuery) {
        return R.ok(batchService.queryPageList(bo, pageQuery));
    }

    @GetMapping("/calendar")
    public R<List<SfPlantingBatchCalendarDayVo>> calendar(
        @RequestParam @Min(2000) @Max(2100) int year,
        @RequestParam @Min(1) @Max(12) int month) {
        return R.ok(batchService.queryCalendar(year, month));
    }

    @GetMapping("/{batchId}")
    public R<SfPlantingBatchVo> getInfo(@PathVariable Long batchId) {
        return R.ok(batchService.queryById(batchId));
    }

    @GetMapping("/{batchId}/detail")
    public R<SfPlantingBatchDetailVo> getDetail(@PathVariable Long batchId) {
        return R.ok(batchService.queryDetailById(batchId));
    }

    /**
     * 分页查询地块绑定检测仪、当前品种及最新上报时间。
     */
    @GetMapping("/detector-info/page")
    public R<PageResult<SfDetectorInfoVo>> detectorInfoPage(SfDetectorInfoBo bo, PageQuery pageQuery) {
        return R.ok(batchService.queryDetectorInfoPage(bo, pageQuery));
    }

    @Log(title = "种植批次", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SfPlantingBatchBo bo) {
        return toAjax(batchService.insertByBo(bo));
    }

    @Log(title = "种植批次", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SfPlantingBatchBo bo) {
        return toAjax(batchService.updateByBo(bo));
    }

    @Log(title = "种植批次状态流转", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{batchId}/transition")
    public R<Void> transition(@PathVariable Long batchId,
                              @Validated @RequestBody SfPlantingBatchTransitionBo bo) {
        return toAjax(batchService.transition(batchId, bo));
    }

    @Log(title = "种植批次", businessType = BusinessType.DELETE)
    @DeleteMapping("/{batchIds}")
    public R<Void> remove(@PathVariable Long[] batchIds) {
        return toAjax(batchService.deleteWithValidByIds(List.of(batchIds)));
    }

    @GetMapping("/suggest-cropping-index")
    public R<Integer> suggestCroppingIndex(@RequestParam Long fieldId,
                                           @RequestParam LocalDate sowingDate) {
        return R.ok(batchService.suggestCroppingIndex(fieldId, sowingDate));
    }

    @GetMapping("/suggest-harvest-date")
    public R<LocalDate> suggestHarvestDate(@RequestParam Long varietyId,
                                           @RequestParam LocalDate sowingDate) {
        return R.ok(batchService.suggestHarvestDate(varietyId, sowingDate));
    }

    @Log(title = "种植批次", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SfPlantingBatchBo bo, HttpServletResponse response) {
        List<SfPlantingBatchExportVo> list = batchService.queryExportList(bo);
        ExcelBuilder.of(list, SfPlantingBatchExportVo.class)
            .sheetName("种植批次数据")
            .toResponse(response);
    }
}
