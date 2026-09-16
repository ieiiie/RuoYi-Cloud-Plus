package com.ym.iot.ownership.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.ym.common.core.domain.R;
import com.ym.iot.ownership.domain.dto.DeviceOwnershipSnapshot;
import com.ym.iot.ownership.service.IDeviceAccessService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/** Business API is read-only; DBO alone manages ownership. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/iot/device/ownership")
public class DeviceOwnershipController {
    private final IDeviceAccessService access;
    @GetMapping("/{deviceId}/current") @SaCheckPermission("iot:device:list")
    public R<DeviceOwnershipSnapshot> current(@PathVariable Long deviceId) {
        access.requireAccess(deviceId);
        return R.ok(access.currentOwnership(deviceId));
    }
}
