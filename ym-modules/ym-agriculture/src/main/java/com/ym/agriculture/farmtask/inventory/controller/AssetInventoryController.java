package com.ym.agriculture.farmtask.inventory.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetActionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetDeviceBatchBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetDeviceVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetTypeSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetTypeVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;
import com.ym.agriculture.farmtask.inventory.service.IAssetInventoryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/stask/assets")
public class AssetInventoryController extends BaseController {

    private final IAssetInventoryService assetService;

    @SaCheckPermission("inventory:asset:list")
    @GetMapping("/types/summary")
    public R<List<AssetTypeVo>> typeSummary() {
        return R.ok(assetService.typeSummary());
    }

    @Log(title = "资产种类", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @SaCheckPermission("inventory:asset:add")
    @PostMapping("/types")
    public R<MutationVo> addType(@Valid @RequestBody AssetTypeSaveBo bo) {
        return R.ok(assetService.addType(bo));
    }

    @Log(title = "资产种类", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @SaCheckPermission("inventory:asset:edit")
    @PutMapping("/types/{id}")
    public R<MutationVo> updateType(@PathVariable Long id, @Valid @RequestBody AssetTypeSaveBo bo) {
        return R.ok(assetService.updateType(id, bo));
    }

    @Log(title = "资产种类", businessType = BusinessType.DELETE)
    @SaCheckPermission("inventory:asset:remove")
    @DeleteMapping("/types/{id}")
    public R<Void> removeType(@PathVariable Long id, @RequestParam Long version) {
        assetService.removeType(id, version);
        return R.ok();
    }

    @Log(title = "资产设备", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @SaCheckPermission("inventory:asset:add")
    @PostMapping("/devices/batch")
    public R<List<MutationVo>> batchDevices(@Valid @RequestBody AssetDeviceBatchBo bo) {
        return R.ok(assetService.batchAddDevices(bo));
    }

    @SaCheckPermission("inventory:asset:list")
    @GetMapping("/devices")
    public R<PageResult<AssetDeviceVo>> devices(AssetQuery query, PageQuery pageQuery) {
        return R.ok(assetService.pageDevices(query, pageQuery));
    }

    @SaCheckPermission("inventory:asset:list")
    @GetMapping("/devices/{id}")
    public R<AssetDeviceVo> device(@PathVariable Long id) {
        return R.ok(assetService.getDevice(id));
    }

    @Log(title = "资产状态", businessType = BusinessType.UPDATE)
    @SaCheckPermission("inventory:asset:use")
    @PostMapping("/devices/{id}/actions/{action}")
    public R<MutationVo> transition(@PathVariable Long id, @PathVariable String action,
                                    @Valid @RequestBody AssetActionBo bo) {
        return R.ok(assetService.transition(id, action, bo));
    }

    @Log(title = "资产设备", businessType = BusinessType.DELETE)
    @SaCheckPermission("inventory:asset:remove")
    @DeleteMapping("/devices/{id}")
    public R<Void> remove(@PathVariable Long id, @RequestParam Long version) {
        assetService.removeDevice(id, version);
        return R.ok();
    }
}
