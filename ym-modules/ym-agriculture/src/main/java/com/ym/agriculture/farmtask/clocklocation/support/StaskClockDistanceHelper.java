package com.ym.agriculture.farmtask.clocklocation.support;

import com.ym.agriculture.farmtask.clocklocation.model.vo.SfStaskClockLocationVo;

import java.math.BigDecimal;

/**
 * 组长到岗打卡距离计算辅助（仅存档，不拦截打卡）。
 */
public final class StaskClockDistanceHelper {

    private static final double EARTH_RADIUS_METERS = 6371008.8D;
    private static final double PI = Math.PI;
    private static final double AXIS = 6378245.0D;
    private static final double OFFSET = 0.00669342162296594323D;

    private StaskClockDistanceHelper() {
    }

    /**
     * 计算打卡坐标与租户打卡地点中心点的球面距离。
     *
     * @param clockLng 打卡经度（GCJ-02）
     * @param clockLat 打卡纬度（GCJ-02）
     * @param location 租户打卡地点配置
     * @return 距离，单位：米；坐标或中心点缺失时返回 {@code null}
     */
    public static Integer computeDistanceMeters(BigDecimal clockLng, BigDecimal clockLat,
        SfStaskClockLocationVo location) {
        if (clockLng == null || clockLat == null || location == null) {
            return null;
        }
        if (location.getCenterLng() == null || location.getCenterLat() == null) {
            return null;
        }
        double[] clockPoint = gcj02ToCgcs2000(clockLng.doubleValue(), clockLat.doubleValue());
        double distance = distanceMeters(clockPoint[0], clockPoint[1],
            location.getCenterLng().doubleValue(), location.getCenterLat().doubleValue());
        return (int) Math.round(distance);
    }

    private static double distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = latRad2 - latRad1;
        double deltaLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(latRad1) * Math.cos(latRad2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * GCJ-02（火星坐标）反偏为大地坐标，用于与天地图 CGCS2000 中心点比较。
     */
    private static double[] gcj02ToCgcs2000(double lng, double lat) {
        if (outOfChina(lng, lat)) {
            return new double[] {lng, lat};
        }
        double[] delta = delta(lng, lat);
        return new double[] {lng * 2 - delta[0], lat * 2 - delta[1]};
    }

    private static double[] delta(double lng, double lat) {
        double dLat = transformLat(lng - 105.0D, lat - 35.0D);
        double dLng = transformLng(lng - 105.0D, lat - 35.0D);
        double radLat = lat / 180.0D * PI;
        double magic = Math.sin(radLat);
        magic = 1 - OFFSET * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0D) / ((AXIS * (1 - OFFSET)) / (magic * sqrtMagic) * PI);
        dLng = (dLng * 180.0D) / (AXIS / sqrtMagic * Math.cos(radLat) * PI);
        return new double[] {lng + dLng, lat + dLat};
    }

    private static boolean outOfChina(double lng, double lat) {
        return lng < 72.004D || lng > 137.8347D || lat < 0.8293D || lat > 55.8271D;
    }

    private static double transformLat(double lng, double lat) {
        double ret = -100.0D + 2.0D * lng + 3.0D * lat + 0.2D * lat * lat + 0.1D * lng * lat
            + 0.2D * Math.sqrt(Math.abs(lng));
        ret += (20.0D * Math.sin(6.0D * lng * PI) + 20.0D * Math.sin(2.0D * lng * PI)) * 2.0D / 3.0D;
        ret += (20.0D * Math.sin(lat * PI) + 40.0D * Math.sin(lat / 3.0D * PI)) * 2.0D / 3.0D;
        ret += (160.0D * Math.sin(lat / 12.0D * PI) + 320.0D * Math.sin(lat * PI / 30.0D)) * 2.0D / 3.0D;
        return ret;
    }

    private static double transformLng(double lng, double lat) {
        double ret = 300.0D + lng + 2.0D * lat + 0.1D * lng * lng + 0.1D * lng * lat
            + 0.1D * Math.sqrt(Math.abs(lng));
        ret += (20.0D * Math.sin(6.0D * lng * PI) + 20.0D * Math.sin(2.0D * lng * PI)) * 2.0D / 3.0D;
        ret += (20.0D * Math.sin(lng * PI) + 40.0D * Math.sin(lng / 3.0D * PI)) * 2.0D / 3.0D;
        ret += (150.0D * Math.sin(lng / 12.0D * PI) + 300.0D * Math.sin(lng / 30.0D * PI)) * 2.0D / 3.0D;
        return ret;
    }
}
