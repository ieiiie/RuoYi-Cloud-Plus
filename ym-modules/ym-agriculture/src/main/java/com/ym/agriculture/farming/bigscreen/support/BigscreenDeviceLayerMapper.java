package com.ym.agriculture.farming.bigscreen.support;

import cn.hutool.core.util.StrUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.agriculture.farming.bigscreen.config.BigscreenCaliber;
import com.ym.agriculture.farming.uav.support.UavIotDeviceSupport;

/**
 * 大屏地图设备图层与虚拟分区映射。
 *
 * @author ym-cloud
 */
public final class BigscreenDeviceLayerMapper {

    /** 地图图层类型 */
    public static final String LAYER_SENSOR = "sensor";
    public static final String LAYER_CAMERA = "camera";
    public static final String LAYER_VALVE = "valve";
    public static final String LAYER_FACILITY = "facility";
    public static final String LAYER_UAV = "uav";

    /** 虚拟分区编码 */
    public static final String ZONE_MAIN = "main";
    public static final String ZONE_210 = "zone210";
    public static final String ZONE_49 = "zone49";
    public static final String ZONE_43 = "zone43";

    private BigscreenDeviceLayerMapper() {
    }

    /**
     * 解析地图图层类型（product_key 优先，名称关键词兜底）。
     *
     * @param device 设备档案
     * @return sensor / camera / valve / facility / uav
     */
    public static String resolveLayerType(RemoteDeviceSummaryVo device) {
        if (device == null) {
            return LAYER_FACILITY;
        }
        String productKey = StrUtil.trimToEmpty(device.getProductKey());
        if (UavIotDeviceSupport.isLikelyUavDock(device)) {
            return LAYER_UAV;
        }
        if (BigscreenCaliber.PRODUCT_KEY_MOTORVALVE.equalsIgnoreCase(productKey)
            || BigscreenValveTypeResolver.inferValveTypeCodeFromProductKey(productKey) != null) {
            return LAYER_VALVE;
        }
        if (BigscreenCaliber.PRODUCT_KEY_FERTILIZER.equalsIgnoreCase(productKey)) {
            return LAYER_FACILITY;
        }
        String category = StrUtil.trimToEmpty(device.getDeviceCategory());
        String name = StrUtil.nullToEmpty(device.getDeviceName());
        String code = StrUtil.nullToEmpty(device.getDeviceCode());
        if ("HK_CAMERA".equalsIgnoreCase(category) || name.contains("摄像")) {
            return LAYER_CAMERA;
        }
        if (name.contains("电动阀") || code.startsWith("250705")) {
            return LAYER_VALVE;
        }
        if (name.contains("施肥机") || name.contains("水肥")) {
            return LAYER_FACILITY;
        }
        if (BigscreenCaliber.SENSOR_DEVICE_CATEGORY.equalsIgnoreCase(category)) {
            return LAYER_SENSOR;
        }
        return LAYER_FACILITY;
    }

    /**
     * 解析虚拟分区展示标签（设备名 → 210/49/43 亩分区，非 sf_field 主键）。
     *
     * @param device 设备档案
     * @return main / zone210 / zone49 / zone43
     */
    public static String resolveVirtualZone(RemoteDeviceSummaryVo device) {
        if (device == null || StringUtils.isBlank(device.getDeviceName())) {
            return ZONE_MAIN;
        }
        String name = device.getDeviceName();
        if (name.contains("210亩")) {
            return ZONE_210;
        }
        if (name.contains("49亩")) {
            return ZONE_49;
        }
        if (name.contains("43亩")) {
            return ZONE_43;
        }
        return ZONE_MAIN;
    }

    /**
     * 虚拟分区中文标签。
     */
    public static String resolveVirtualZoneLabel(String virtualZone) {
        if (ZONE_210.equals(virtualZone)) {
            return "210亩";
        }
        if (ZONE_49.equals(virtualZone)) {
            return "49亩";
        }
        if (ZONE_43.equals(virtualZone)) {
            return "43亩";
        }
        return "主地块";
    }
}
