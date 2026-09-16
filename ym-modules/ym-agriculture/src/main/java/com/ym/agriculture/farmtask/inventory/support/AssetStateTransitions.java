package com.ym.agriculture.farmtask.inventory.support;

import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;

import java.util.Locale;

/** 资产设备状态机；Web、小程序及测试共享同一组不可绕过的转换规则。 */
public final class AssetStateTransitions {

    private AssetStateTransitions() {
    }

    public static String checkOut(String current) {
        if (!InventoryConstants.ASSET_IDLE.equals(current)) {
            throw invalid("只有闲置设备可以领用");
        }
        return InventoryConstants.ASSET_IN_USE;
    }

    public static String returnDevice(String current) {
        if (!InventoryConstants.ASSET_IN_USE.equals(current)) {
            throw invalid("只有在用设备可以归还");
        }
        return InventoryConstants.ASSET_IDLE;
    }

    public static String changeStatus(String current, String requested) {
        String target = requested == null ? null : requested.trim();
        if (target == null || target.isEmpty()) {
            throw invalid("目标状态不能为空");
        }
        target = target.toUpperCase(Locale.ROOT);
        if (InventoryConstants.ASSET_SCRAPPED.equals(current)) {
            throw invalid("已报废设备不可恢复");
        }
        if (InventoryConstants.ASSET_IN_USE.equals(current)) {
            throw invalid("在用设备必须先归还才能维修或报废");
        }
        boolean allowed = InventoryConstants.ASSET_IDLE.equals(current)
            && (InventoryConstants.ASSET_MAINTENANCE.equals(target)
            || InventoryConstants.ASSET_SCRAPPED.equals(target))
            || InventoryConstants.ASSET_MAINTENANCE.equals(current)
            && InventoryConstants.ASSET_IDLE.equals(target);
        if (!allowed) {
            throw invalid("不允许的资产状态转换：" + current + " → " + target);
        }
        return target;
    }

    private static InventoryBusinessException invalid(String message) {
        return new InventoryBusinessException(InventoryConstants.ERROR_ASSET_STATE_INVALID, message);
    }
}
