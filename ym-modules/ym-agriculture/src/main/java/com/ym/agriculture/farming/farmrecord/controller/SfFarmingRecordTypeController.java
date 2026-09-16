package com.ym.agriculture.farming.farmrecord.controller;

import com.ym.common.core.domain.R;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordTypeSaveBo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordTypeVo;
import com.ym.agriculture.farming.farmrecord.service.ISfFarmingRecordTypeService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 农事类型主数据（CRUD）。
 * <p>需登录租户上下文；与 {@code sf_farming_record_type} 对应。</p>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/farming/types")
public class SfFarmingRecordTypeController extends BaseController {

    private final ISfFarmingRecordTypeService typeService;

    @GetMapping("/enabled")
    public R<List<SfFarmingRecordTypeVo>> listEnabled() {
        return R.ok(typeService.listEnabled());
    }

    @GetMapping("/list")
    public R<List<SfFarmingRecordTypeVo>> listAll() {
        return R.ok(typeService.listAllNormal());
    }

    @GetMapping("/{typeId}")
    public R<SfFarmingRecordTypeVo> get(@NotNull @PathVariable Long typeId) {
        return R.ok(typeService.get(typeId));
    }

    @Log(title = "农事类型", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SfFarmingRecordTypeSaveBo bo) {
        return toAjax(typeService.add(bo));
    }

    @Log(title = "农事类型", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SfFarmingRecordTypeSaveBo bo) {
        return toAjax(typeService.update(bo));
    }

    @Log(title = "农事类型", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/{typeId}")
    public R<Void> remove(@NotNull @PathVariable Long typeId) {
        return toAjax(typeService.remove(typeId));
    }
}
