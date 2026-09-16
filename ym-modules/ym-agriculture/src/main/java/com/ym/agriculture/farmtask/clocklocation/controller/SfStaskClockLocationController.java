package com.ym.agriculture.farmtask.clocklocation.controller;

import com.ym.agriculture.farmtask.clocklocation.model.bo.SfStaskClockLocationBo;
import com.ym.agriculture.farmtask.clocklocation.model.vo.SfStaskClockLocationVo;
import com.ym.agriculture.farmtask.clocklocation.service.ISfStaskClockLocationService;
import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.redis.annotation.RepeatSubmit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 打卡地点管理。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/stask/clock-location")
public class SfStaskClockLocationController {

    private final ISfStaskClockLocationService clockLocationService;

    @GetMapping
    public R<SfStaskClockLocationVo> get() {
        return R.ok(clockLocationService.getCurrent());
    }

    @Log(title = "农事任务打卡地点", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> save(@Valid @RequestBody SfStaskClockLocationBo bo) {
        clockLocationService.save(bo);
        return R.ok();
    }
}
