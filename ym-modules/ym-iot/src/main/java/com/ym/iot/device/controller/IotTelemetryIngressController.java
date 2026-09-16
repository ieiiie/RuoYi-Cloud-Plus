package com.ym.iot.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.iot.device.domain.bo.IotTelemetryIngressBo;
import com.ym.iot.device.service.IIotTelemetryIngressService;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物联网遥测接入。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/iot/telemetry")
public class IotTelemetryIngressController {

    private final IIotTelemetryIngressService telemetryIngressService;

    @SaCheckPermission("iot:telemetry:ingress")
    @Log(title = "物联网遥测接入", businessType = BusinessType.INSERT)
    @PostMapping("/ingress")
    public R<Integer> ingest(@Validated @RequestBody IotTelemetryIngressBo bo) {
        return R.ok(telemetryIngressService.ingest(bo));
    }
}
