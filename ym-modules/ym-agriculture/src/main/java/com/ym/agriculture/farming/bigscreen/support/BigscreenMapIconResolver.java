package com.ym.agriculture.farming.bigscreen.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteProductVo;
import com.ym.agriculture.farming.field.support.SfFieldMasterDictAccessor;
import com.ym.system.api.domain.vo.RemoteDictDataVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 大屏地图设备图标解析：产品主数据 → 设备大类字典 remark → null。
 */
@Component
@RequiredArgsConstructor
public class BigscreenMapIconResolver {

    /** 与 {@code iot_device.device_category} 对齐 */
    private static final String IOT_DEVICE_TYPE_DICT = "iot_device_type";

    private final SfBigscreenIotAccessor iotAccessor;
    private final SfFieldMasterDictAccessor masterDictAccessor;

    /**
     * 批量解析设备地图图标。
     *
     * @param devices 租户设备列表
     * @return deviceId → 图标
     */
    public Map<Long, ResolvedIcon> resolveBatch(List<RemoteDeviceSummaryVo> devices) {
        if (devices == null || devices.isEmpty()) {
            return Map.of();
        }
        List<Long> productIds = devices.stream()
            .map(RemoteDeviceSummaryVo::getProductId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<String> productKeys = devices.stream()
            .map(RemoteDeviceSummaryVo::getProductKey)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .toList();

        Map<Long, RemoteProductVo> productById = iotAccessor.queryProductsByIds(productIds).stream()
            .filter(p -> p.getProductId() != null)
            .collect(Collectors.toMap(RemoteProductVo::getProductId, p -> p, (a, b) -> a));
        Map<String, RemoteProductVo> productByKey = new LinkedHashMap<>();
        for (RemoteProductVo product : iotAccessor.queryProductsByProductKeys(productKeys)) {
            if (product != null && StringUtils.isNotBlank(product.getProductKey())) {
                productByKey.putIfAbsent(product.getProductKey().trim(), product);
            }
        }
        Map<String, RemoteDictDataVo> dictByCategory = indexDictByCategory(
            masterDictAccessor.getDictDataList(IOT_DEVICE_TYPE_DICT));

        Map<Long, ResolvedIcon> result = new HashMap<>();
        for (RemoteDeviceSummaryVo device : devices) {
            if (device == null || device.getDeviceId() == null) {
                continue;
            }
            result.put(device.getDeviceId(), resolveOne(device, productById, productByKey, dictByCategory));
        }
        return result;
    }

    private static ResolvedIcon resolveOne(RemoteDeviceSummaryVo device,
                                         Map<Long, RemoteProductVo> productById,
                                         Map<String, RemoteProductVo> productByKey,
                                         Map<String, RemoteDictDataVo> dictByCategory) {
        RemoteProductVo product = null;
        if (device.getProductId() != null) {
            product = productById.get(device.getProductId());
        }
        if (product == null && StringUtils.isNotBlank(device.getProductKey())) {
            product = productByKey.get(device.getProductKey().trim());
        }
        if (product != null && StringUtils.isNotBlank(product.getMapIconUrl())) {
            return new ResolvedIcon(product.getMapIconUrl(),
                StringUtils.isNotBlank(product.getMapSelectedIconUrl()) ? product.getMapSelectedIconUrl() : null);
        }
        String category = device.getDeviceCategory();
        if (StringUtils.isNotBlank(category) && dictByCategory != null) {
            RemoteDictDataVo row = dictByCategory.get(category.trim());
            if (row != null) {
                String fromRemark = extractUrl(row.getRemark());
                if (StringUtils.isNotBlank(fromRemark)) {
                    return new ResolvedIcon(fromRemark, null);
                }
            }
        }
        return ResolvedIcon.empty();
    }

    private static Map<String, RemoteDictDataVo> indexDictByCategory(List<RemoteDictDataVo> dictRows) {
        if (dictRows == null || dictRows.isEmpty()) {
            return Map.of();
        }
        Map<String, RemoteDictDataVo> indexed = new LinkedHashMap<>();
        for (RemoteDictDataVo row : dictRows) {
            if (row != null && StringUtils.isNotBlank(row.getDictValue())) {
                indexed.putIfAbsent(row.getDictValue().trim(), row);
            }
        }
        return indexed;
    }

    /** 自 remark 中截取首个 http 起始子串作为图标 URL。 */
    private static String extractUrl(String remark) {
        if (StringUtils.isBlank(remark)) {
            return null;
        }
        int index = remark.indexOf("http");
        if (index < 0) {
            return null;
        }
        return remark.substring(index).trim();
    }

    /**
     * 解析后的地图图标。
     *
     * @param mapIconUrl         默认图标 URL
     * @param mapSelectedIconUrl 选中态图标 URL（可选）
     */
    public record ResolvedIcon(String mapIconUrl, String mapSelectedIconUrl) {

        public static ResolvedIcon empty() {
            return new ResolvedIcon(null, null);
        }
    }
}
