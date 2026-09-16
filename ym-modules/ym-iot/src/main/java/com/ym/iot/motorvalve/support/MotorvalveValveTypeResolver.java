package com.ym.iot.motorvalve.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ym.iot.device.domain.IotDevice;
import com.ym.iot.device.domain.vo.IotDeviceVo;
import com.ym.iot.motorvalve.enums.ValveType;

import lombok.extern.slf4j.Slf4j;

/**
 * 从设备档案解析电动阀阀门类型。
 *
 * <p>优先级：iot_product.product_key &gt; configJson.motorvalveValveType
 * &gt; iot_device_tag(chanel) &gt; 设备名称备注 &gt; 请求参数 &gt; 默认单通。
 */
@Slf4j
public final class MotorvalveValveTypeResolver {

    public static final String CONFIG_VALVE_TYPE_KEY = "motorvalveValveType";

    /** 单通电动阀产品标识。 */
    public static final String PRODUCT_KEY_SINGLE = "single_MOTORVALVE";

    /** 分体电动阀产品标识，控制算法按单通阀处理。 */
    public static final String PRODUCT_KEY_SEPARATE = "separate_MOTORVALVE";

    /** 三通电动阀产品标识。 */
    public static final String PRODUCT_KEY_TEE = "tee_MOTORVALVE";

    /** 五通电动阀产品标识。 */
    public static final String PRODUCT_KEY_FIVE = "five_MOTORVALVE";

    /** 分体阀展示名称；控制算法仍复用单通阀。 */
    public static final String PRODUCT_KEY_SEPARATE_LABEL = "分体阀";

    /** 设备标签键：通道数（5=五通，3=三通，1=单通） */
    public static final String TAG_KEY_CHANNEL = "chanel";

    public static final String TAG_VALUE_FIVE_PORT = "5";

    public static final String TAG_VALUE_THREE_WAY = "3";

    public static final String TAG_VALUE_SINGLE_PORT = "1";

    private MotorvalveValveTypeResolver() {
    }

    /**
     * 解析阀门类型：productKey 优先；请求参数仅在设备侧配置均不可识别时作为历史兜底。
     *
     * <p>实体本身不包含 productKey 时使用此兼容入口；控制链路应优先调用
     * {@link #resolve(IotDevice, String, String, String)} 传入产品标识。
     */
    public static ValveType resolve(IotDevice device, String requestValveType) {
        return resolve(device, null, requestValveType, null);
    }

    /**
     * 解析阀门类型（含 chanel 标签值），兼容旧调用。
     */
    public static ValveType resolve(IotDevice device, String requestValveType, String channelTagValue) {
        return resolve(device, null, requestValveType, channelTagValue);
    }

    /**
     * 解析阀门类型（productKey 优先，chanel 标签仅作历史兜底）。
     */
    public static ValveType resolve(IotDevice device, String productKey, String requestValveType,
                                    String channelTagValue) {
        String fromProductKey = inferValveTypeCodeFromProductKey(productKey);
        if (fromProductKey != null) {
            return ValveType.fromCode(fromProductKey);
        }
        String fromConfig = readFromConfigJson(device);
        if (fromConfig != null) {
            return ValveType.fromCode(fromConfig);
        }
        String fromTag = inferValveTypeCodeFromChannelTag(channelTagValue);
        if (fromTag != null) {
            return ValveType.fromCode(fromTag);
        }
        String fromName = inferValveTypeCodeFromName(device != null ? device.getDeviceName() : null);
        if (fromName != null) {
            return ValveType.fromCode(fromName);
        }
        if (requestValveType != null && !requestValveType.isBlank()) {
            return ValveType.fromCode(requestValveType);
        }
        return ValveType.SINGLE_PORT;
    }

    /**
     * 从设备视图解析阀门类型：productKey 优先，其次 chanel 标签和设备名称，默认单通阀。
     */
    public static ValveType resolve(IotDeviceVo device) {
        return resolve(device, null);
    }

    /**
     * 从设备视图解析阀门类型（productKey 优先，含 chanel 标签兜底）。
     */
    public static ValveType resolve(IotDeviceVo device, String channelTagValue) {
        return resolve(device, device != null ? device.getProductKey() : null, channelTagValue);
    }

    /**
     * 从设备视图解析阀门类型（productKey 优先，chanel 标签仅作历史兜底）。
     */
    public static ValveType resolve(IotDeviceVo device, String productKey, String channelTagValue) {
        String fromProductKey = inferValveTypeCodeFromProductKey(productKey);
        if (fromProductKey != null) {
            return ValveType.fromCode(fromProductKey);
        }
        String fromTag = inferValveTypeCodeFromChannelTag(channelTagValue);
        if (fromTag != null) {
            return ValveType.fromCode(fromTag);
        }
        String fromName = inferValveTypeCodeFromName(device != null ? device.getDeviceName() : null);
        if (fromName != null) {
            return ValveType.fromCode(fromName);
        }
        return ValveType.SINGLE_PORT;
    }

    /**
     * 从产品标识推断阀型编码；无法识别时返回 null（由上层继续走历史兜底）。
     */
    public static String inferValveTypeCodeFromProductKey(String productKey) {
        if (productKey == null || productKey.isBlank()) {
            return null;
        }
        String key = productKey.trim();
        if (PRODUCT_KEY_SINGLE.equalsIgnoreCase(key) || PRODUCT_KEY_SEPARATE.equalsIgnoreCase(key)) {
            return ValveType.SINGLE_PORT.getCode();
        }
        if (PRODUCT_KEY_TEE.equalsIgnoreCase(key)) {
            return ValveType.THREE_WAY_L.getCode();
        }
        if (PRODUCT_KEY_FIVE.equalsIgnoreCase(key)) {
            return ValveType.FIVE_PORT.getCode();
        }
        return null;
    }

    /**
     * 从产品标识推断前端展示名称；无法识别时返回 null，由调用方按解析后的阀型兜底。
     *
     * <p>注意：{@code separate_MOTORVALVE} 的控制算法按单通阀处理，但业务展示应为“分体阀”。</p>
     */
    public static String inferValveTypeLabelFromProductKey(String productKey) {
        if (productKey == null || productKey.isBlank()) {
            return null;
        }
        String key = productKey.trim();
        if (PRODUCT_KEY_SINGLE.equalsIgnoreCase(key)) {
            return ValveType.SINGLE_PORT.getLabel();
        }
        if (PRODUCT_KEY_SEPARATE.equalsIgnoreCase(key)) {
            return PRODUCT_KEY_SEPARATE_LABEL;
        }
        if (PRODUCT_KEY_TEE.equalsIgnoreCase(key)) {
            return ValveType.THREE_WAY_L.getLabel();
        }
        if (PRODUCT_KEY_FIVE.equalsIgnoreCase(key)) {
            return ValveType.FIVE_PORT.getLabel();
        }
        return null;
    }

    /**
     * 解析阀型展示名称：产品标识文案优先，未知产品标识则使用已解析阀型名称。
     *
     * @param resolvedType 已解析的控制算法阀型
     * @param productKey   产品标识
     * @return 阀型展示名称
     */
    public static String resolveDisplayLabel(ValveType resolvedType, String productKey) {
        String label = inferValveTypeLabelFromProductKey(productKey);
        if (label != null) {
            return label;
        }
        return resolvedType != null ? resolvedType.getLabel() : ValveType.SINGLE_PORT.getLabel();
    }

    /**
     * 从 chanel 标签值推断阀型编码；无法识别时返回 null（由上层回落单通）。
     */
    public static String inferValveTypeCodeFromChannelTag(String tagValue) {
        if (tagValue == null || tagValue.isBlank()) {
            return null;
        }
        String value = tagValue.trim();
        if (TAG_VALUE_FIVE_PORT.equals(value)) {
            return ValveType.FIVE_PORT.getCode();
        }
        if (TAG_VALUE_THREE_WAY.equals(value)) {
            return ValveType.THREE_WAY_L.getCode();
        }
        if (TAG_VALUE_SINGLE_PORT.equals(value)) {
            return ValveType.SINGLE_PORT.getCode();
        }
        return null;
    }

    /**
     * 从设备名称备注推断阀型编码；无法识别时返回 null。
     */
    public static String inferValveTypeCodeFromName(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            return null;
        }
        if (deviceName.contains("【五通】") || deviceName.contains("五通")) {
            return ValveType.FIVE_PORT.getCode();
        }
        if (deviceName.contains("【三通】") || deviceName.contains("三通")) {
            return ValveType.THREE_WAY_L.getCode();
        }
        if (deviceName.contains("【单通】") || deviceName.contains("单通")) {
            return ValveType.SINGLE_PORT.getCode();
        }
        return null;
    }

    private static String readFromConfigJson(IotDevice device) {
        if (device == null || device.getConfigJson() == null || device.getConfigJson().isBlank()) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(device.getConfigJson());
            String type = json.getString(CONFIG_VALVE_TYPE_KEY);
            if (type != null && !type.isBlank()) {
                return type.trim();
            }
        } catch (Exception e) {
            log.warn("解析 configJson.{} 失败 deviceId={}: {}",
                CONFIG_VALVE_TYPE_KEY, device.getDeviceId(), e.getMessage());
        }
        return null;
    }
}
