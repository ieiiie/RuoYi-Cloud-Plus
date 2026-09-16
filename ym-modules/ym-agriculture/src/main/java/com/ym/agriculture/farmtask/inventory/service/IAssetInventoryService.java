package com.ym.agriculture.farmtask.inventory.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetActionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetDeviceBatchBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetDeviceVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetTypeSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetTypeVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;

import java.util.List;

public interface IAssetInventoryService {

    List<AssetTypeVo> typeSummary();

    MutationVo addType(AssetTypeSaveBo bo);

    MutationVo updateType(Long id, AssetTypeSaveBo bo);

    void removeType(Long id, Long version);

    List<MutationVo> batchAddDevices(AssetDeviceBatchBo bo);

    PageResult<AssetDeviceVo> pageDevices(AssetQuery query, PageQuery pageQuery);

    AssetDeviceVo getDevice(Long id);

    MutationVo transition(Long id, String action, AssetActionBo bo);

    void removeDevice(Long id, Long version);

    long assetCount();
}
