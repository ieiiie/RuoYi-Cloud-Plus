package com.ym.agriculture.farming.weatheralert.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.system.api.RemoteRegionService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 省码 → 中央气象台省份过滤名。
 * <p>NMC 常用短名（如“浙江”“北京”），优先去掉省/市/自治区等后缀。
 */
@Component
public class NmcProvinceNameResolver {

    @DubboReference
    private RemoteRegionService regionService;

    private final Map<String, String> cache = new LinkedHashMap<>();

    public String resolveProvinceName(String provinceCode) {
        String padded = NmcAlertParser.pad6(provinceCode);
        if (StringUtils.isBlank(padded)) {
            return null;
        }
        synchronized (cache) {
            if (cache.containsKey(padded)) {
                return cache.get(padded);
            }
        }
        String name = loadName(padded);
        String nmcName = toNmcProvinceName(name);
        synchronized (cache) {
            cache.put(padded, nmcName);
        }
        return nmcName;
    }

    private String loadName(String paddedAdcode) {
        return regionService.getRegionName(paddedAdcode);
    }

    static String toNmcProvinceName(String regionName) {
        if (StringUtils.isBlank(regionName)) {
            return null;
        }
        String name = regionName.trim();
        String[] suffixes = {"特别行政区", "维吾尔自治区", "壮族自治区", "回族自治区", "自治区", "省", "市"};
        for (String suffix : suffixes) {
            if (name.endsWith(suffix) && name.length() > suffix.length()) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }
}
