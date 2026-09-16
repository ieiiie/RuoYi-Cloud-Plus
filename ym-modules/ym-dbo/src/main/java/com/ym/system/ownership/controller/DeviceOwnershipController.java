package com.ym.system.ownership.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.system.ownership.model.*;
import com.ym.system.ownership.service.DeviceOwnershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/iot/device-ownership")
public class DeviceOwnershipController {
    private final DeviceOwnershipService service;
    @GetMapping("/pool") @SaCheckPermission("saas:iot-device-ownership:query")
    public R<List<UnassignedDeviceVo>> pool(@RequestParam(defaultValue="0") long afterDeviceId,@RequestParam(defaultValue="50") int limit) { return R.ok(service.pool(afterDeviceId,limit)); }
    @GetMapping("/assigned") @SaCheckPermission("saas:iot-device-ownership:query")
    public R<List<AssignedDeviceVo>> assigned(@RequestParam(defaultValue="0") long afterDeviceId,@RequestParam(defaultValue="50") int limit,@RequestParam(required=false) String tenantId) { return R.ok(service.assigned(afterDeviceId,limit,tenantId)); }
    @GetMapping("/tenants") @SaCheckPermission("saas:iot-device-ownership:query")
    public R<List<OwnershipTenantVo>> tenants() { return R.ok(service.tenants()); }
    @GetMapping("/{deviceId}/current") @SaCheckPermission("saas:iot-device-ownership:query")
    public R<DeviceOwnershipSnapshot> current(@PathVariable Long deviceId) { return R.ok(service.current(deviceId)); }
    @GetMapping("/{deviceId}/blockers") @SaCheckPermission("saas:iot-device-ownership:query")
    public R<List<OwnershipBlockerVo>> blockers(@PathVariable Long deviceId) { return R.ok(service.blockers(deviceId)); }
    @GetMapping("/{deviceId}/history") @SaCheckPermission("saas:iot-device-ownership:query")
    public R<List<OwnershipHistoryVo>> history(@PathVariable Long deviceId,@RequestParam(defaultValue="-1") long afterVersion,@RequestParam(defaultValue="50") int limit) { return R.ok(service.history(deviceId,afterVersion,limit)); }
    @PostMapping("/{deviceId}/assign") @SaCheckPermission("saas:iot-device-ownership:assign") @Log(title="设备归属分配",businessType=BusinessType.UPDATE)
    public R<DeviceOwnershipSnapshot> assign(@PathVariable Long deviceId,@Valid @RequestBody OwnershipChangeBo bo) { return R.ok(service.assign(deviceId,bo)); }
    @PostMapping("/{deviceId}/release") @SaCheckPermission("saas:iot-device-ownership:release") @Log(title="设备归属释放",businessType=BusinessType.UPDATE)
    public R<DeviceOwnershipSnapshot> release(@PathVariable Long deviceId,@Valid @RequestBody OwnershipReleaseBo bo) { return R.ok(service.release(deviceId,bo)); }
    @PostMapping("/{deviceId}/transfer") @SaCheckPermission("saas:iot-device-ownership:transfer") @Log(title="设备归属转移",businessType=BusinessType.UPDATE)
    public R<DeviceOwnershipSnapshot> transfer(@PathVariable Long deviceId,@Valid @RequestBody OwnershipChangeBo bo) { return R.ok(service.transfer(deviceId,bo)); }
    @PostMapping("/{deviceId}/retry-fence") @SaCheckPermission("saas:iot-device-ownership:retry") @Log(title="设备归属栅栏重试",businessType=BusinessType.UPDATE)
    public R<DeviceOwnershipSnapshot> retryFence(@PathVariable Long deviceId,@Valid @RequestBody OwnershipReleaseBo bo) { return R.ok(service.retryFence(deviceId,bo)); }
}
