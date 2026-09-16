package com.ym.agriculture.farmtask.inventory.support;

import com.ym.agriculture.farmtask.inventory.model.InventoryModels.InventorySummaryVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.WorkbenchVo;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

/** 将租户工作台统计按现有页面权限投影为 Web 库存汇总响应。 */
@Component
public class InventorySummaryAssembler {

    public static final String OUTBOUND_PERMISSION = "inventory:outbound:list";
    public static final String RETURN_PERMISSION = "inventory:return:list";
    public static final String BALANCE_PERMISSION = "inventory:balance:list";
    public static final String MATERIAL_PERMISSION = "inventory:material:list";
    public static final String ASSET_PERMISSION = "inventory:asset:list";

    /**
     * 只有通过对应页面列表权限的统计字段才会被设置；有权限的零值仍保留为 0。
     *
     * @param source 工作台租户统计
     * @param permissionChecker 当前登录用户权限判断器
     * @return 按字段裁剪后的 Web 汇总
     */
    public InventorySummaryVo project(WorkbenchVo source, Predicate<String> permissionChecker) {
        InventorySummaryVo result = new InventorySummaryVo();
        if (permissionChecker.test(OUTBOUND_PERMISSION)) {
            result.setPendingOutboundCount(source.getPendingOutboundCount());
        }
        if (permissionChecker.test(RETURN_PERMISSION)) {
            result.setPendingReturnCount(source.getPendingReturnCount());
        }
        if (permissionChecker.test(BALANCE_PERMISSION)) {
            result.setAbnormalInventoryCount(source.getAbnormalInventoryCount());
        }
        if (permissionChecker.test(MATERIAL_PERMISSION)) {
            result.setEnabledMaterialCount(source.getEnabledMaterialCount());
        }
        if (permissionChecker.test(ASSET_PERMISSION)) {
            result.setAssetCount(source.getAssetCount());
        }
        return result;
    }
}
