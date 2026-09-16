package com.ym.agriculture.farmtask.clocklocation.support;

import com.ym.agriculture.farmtask.clocklocation.model.vo.SfStaskClockLocationVo;

/**
 * 组长到岗打卡围栏判定辅助。
 */
public final class StaskClockFenceHelper {

    private StaskClockFenceHelper() {
    }

    /**
     * 是否已配置打卡地点（租户存在 {@code stask.leader.clock-location} 系统参数 JSON）。
     */
    public static boolean isConfigured(SfStaskClockLocationVo location) {
        return location != null && location.getClockLocationId() != null;
    }

    /**
     * 是否需做地理围栏强制校验。
     * <p>
     * 仅在围栏启用且中心点和半径完整时强制校验。这样历史的半配置地点不会意外阻断打卡。
     */
    public static boolean isFenceCheckRequired(SfStaskClockLocationVo location) {
        return location != null
            && "1".equals(location.getEnabled())
            && location.getCenterLng() != null
            && location.getCenterLat() != null
            && location.getRadiusMeters() != null
            && location.getRadiusMeters() >= 0;
    }

    /**
     * 未配置打卡地点时的小程序只读占位（打卡地点仅供地图展示，不强制围栏校验）。
     */
    public static SfStaskClockLocationVo unconfigured() {
        SfStaskClockLocationVo vo = new SfStaskClockLocationVo();
        vo.setConfigured(false);
        vo.setFenceCheckRequired(false);
        vo.setEnabled("0");
        return vo;
    }

    /**
     * 填充 {@link SfStaskClockLocationVo} 的围栏相关派生字段。
     */
    public static void enrichFenceFlags(SfStaskClockLocationVo vo) {
        if (vo == null) {
            return;
        }
        vo.setConfigured(vo.getClockLocationId() != null);
        vo.setFenceCheckRequired(isFenceCheckRequired(vo));
    }
}
