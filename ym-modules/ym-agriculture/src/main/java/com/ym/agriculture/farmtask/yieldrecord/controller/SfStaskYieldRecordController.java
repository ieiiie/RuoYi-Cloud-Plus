package com.ym.agriculture.farmtask.yieldrecord.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ym.agriculture.farmtask.yieldrecord.model.bo.SfStaskYieldBatchCreateBo;
import com.ym.agriculture.farmtask.yieldrecord.model.bo.SfStaskYieldQueryBo;
import com.ym.agriculture.farmtask.yieldrecord.model.bo.SfStaskYieldUpdateBo;
import com.ym.agriculture.farmtask.yieldrecord.model.vo.SfStaskYieldRecordVo;
import com.ym.agriculture.farmtask.yieldrecord.service.ISfStaskYieldRecordService;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.web.core.BaseController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 产量记录管理。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/stask/yields")
public class SfStaskYieldRecordController extends BaseController {

    private final ISfStaskYieldRecordService yieldRecordService;

    @SaCheckPermission("smartfarming:staskYield:list")
    @GetMapping("/page")
    public R<PageResult<SfStaskYieldRecordVo>> page(SfStaskYieldQueryBo bo, PageQuery pageQuery) {
        return R.ok(yieldRecordService.page(bo, pageQuery));
    }

    @SaCheckPermission("smartfarming:staskYield:query")
    @GetMapping("/{yieldId}")
    public R<SfStaskYieldRecordVo> get(@PathVariable Long yieldId) {
        return R.ok(yieldRecordService.getById(yieldId));
    }

    @Log(title = "产量管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @SaCheckPermission("smartfarming:staskYield:add")
    @PostMapping("/batch")
    public R<List<Long>> batchCreate(@Valid @RequestBody SfStaskYieldBatchCreateBo bo) {
        return R.ok(yieldRecordService.batchCreate(bo));
    }

    @Log(title = "产量管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @SaCheckPermission("smartfarming:staskYield:edit")
    @PutMapping("/{yieldId}")
    public R<Void> update(@PathVariable Long yieldId, @Valid @RequestBody SfStaskYieldUpdateBo bo) {
        yieldRecordService.update(yieldId, bo);
        return R.ok();
    }

    @Log(title = "产量管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("smartfarming:staskYield:remove")
    @DeleteMapping("/{yieldIds}")
    public R<Void> remove(@PathVariable Long[] yieldIds) {
        yieldRecordService.remove(Arrays.asList(yieldIds));
        return R.ok();
    }
}
