package com.ym.agriculture.farming.weatheralert.support;

import com.ym.common.core.utils.StringUtils;
import lombok.Value;

/**
 * 租户区划与预警区划匹配：区县命中、市级覆盖下属、省级覆盖全省。
 */
public final class WeatherAlertRegionMatcher {

    private WeatherAlertRegionMatcher() {
    }

    public static boolean matches(TenantRegion tenant, String provinceCode, String cityCode, String districtCode) {
        if (tenant == null) {
            return false;
        }
        String tProvince = NmcAlertParser.pad6(tenant.getProvinceCode());
        String tCity = NmcAlertParser.pad6(tenant.getCityCode());
        String tDistrict = NmcAlertParser.pad6(tenant.getDistrictCode());
        String aProvince = NmcAlertParser.pad6(provinceCode);
        String aCity = NmcAlertParser.pad6(cityCode);
        String aDistrict = NmcAlertParser.pad6(districtCode);

        if (StringUtils.isNotBlank(aDistrict) && aDistrict.equals(tDistrict)) {
            return true;
        }
        if (StringUtils.isNotBlank(aCity) && StringUtils.isBlank(aDistrict) && aCity.equals(tCity)) {
            return true;
        }
        if (StringUtils.isNotBlank(aCity) && aCity.equals(tCity) && StringUtils.isNotBlank(aDistrict)) {
            // 同市其他区县预警：默认不展示，避免噪音；仅市级/省级覆盖
            return false;
        }
        if (StringUtils.isNotBlank(aProvince)
            && StringUtils.isBlank(aCity)
            && StringUtils.isBlank(aDistrict)
            && aProvince.equals(tProvince)) {
            return true;
        }
        // 预警仅有省码但带市/区时，要求与租户同省且区或市命中
        if (StringUtils.isNotBlank(tDistrict) && tDistrict.equals(aDistrict)) {
            return true;
        }
        if (StringUtils.isNotBlank(tCity) && tCity.equals(aCity) && StringUtils.isBlank(aDistrict)) {
            return true;
        }
        return StringUtils.isNotBlank(tProvince)
            && tProvince.equals(aProvince)
            && StringUtils.isBlank(aCity)
            && StringUtils.isBlank(aDistrict);
    }

    @Value
    public static class TenantRegion {
        String provinceCode;
        String cityCode;
        String districtCode;
    }
}
