package com.ym.agriculture.farming.bigscreen.support;

import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;

/** 仅用于大屏展示的阀型解析器。 */
final class BigscreenValveTypeResolver {
    static final String PRODUCT_KEY_SEPARATE = "separate_MOTORVALVE";
    static final String PRODUCT_KEY_SEPARATE_LABEL = "分体阀";

    private BigscreenValveTypeResolver() {}

    static BigscreenValveType resolve(RemoteDeviceSummaryVo device, String channelTagValue) {
        String code = inferValveTypeCodeFromProductKey(device == null ? null : device.getProductKey());
        if (code == null) code = inferFromTag(channelTagValue);
        if (code == null && device != null) code = inferFromName(device.getDeviceName());
        return switch (code == null ? "single" : code) {
            case "five" -> BigscreenValveType.FIVE_WAY;
            case "three" -> BigscreenValveType.THREE_WAY;
            default -> BigscreenValveType.SINGLE;
        };
    }

    static String inferValveTypeCodeFromProductKey(String productKey) {
        if (productKey == null) return null;
        String value = productKey.trim();
        if ("five_MOTORVALVE".equalsIgnoreCase(value)) return "five";
        if ("tee_MOTORVALVE".equalsIgnoreCase(value)) return "three";
        if ("single_MOTORVALVE".equalsIgnoreCase(value) || PRODUCT_KEY_SEPARATE.equalsIgnoreCase(value)) return "single";
        return null;
    }

    private static String inferFromTag(String tag) {
        if (tag == null) return null;
        return switch (tag.trim()) { case "5" -> "five"; case "3" -> "three"; case "1" -> "single"; default -> null; };
    }

    private static String inferFromName(String name) {
        if (name == null) return null;
        if (name.contains("五通")) return "five";
        if (name.contains("三通")) return "three";
        if (name.contains("单通")) return "single";
        return null;
    }
}
