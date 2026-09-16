package com.ym.agriculture.farming.bigscreen.support;

import java.math.BigDecimal;

/**
 * 大屏地图设备坐标规范化：无完整经纬度时统一返回 null。
 */
public final class BigscreenMapCoordinateSupport {

    private BigscreenMapCoordinateSupport() {
    }

    /**
     * 设备经纬度；lng、lat 须同时存在，否则均为 null。
     */
    public record Coordinate(BigDecimal lng, BigDecimal lat) {
    }

    public static Coordinate resolve(BigDecimal lng, BigDecimal lat) {
        if (lng == null || lat == null) {
            return new Coordinate(null, null);
        }
        return new Coordinate(lng, lat);
    }
}
