package com.ym.agriculture.farming.field.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.system.api.RemoteTenantService;
import com.ym.system.api.domain.vo.RemoteTenantInfoVo;
import lombok.Value;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/** 租户省市区区划访问桥接。 */
@Component
public class SfMasterTenantRegionAccessor {

    @DubboReference
    private RemoteTenantService remoteTenantService;

    public TenantCodes selectCodesByTenantId(String tenantId) {
        if (StringUtils.isBlank(tenantId)) return null;
        RemoteTenantInfoVo tenant = remoteTenantService.getTenant(tenantId.trim());
        if (tenant == null) return null;
        return new TenantCodes(normalize(tenant.getProvinceCode()), normalize(tenant.getCityCode()),
            normalize(tenant.getDistrictCode()));
    }

    public Set<String> listActiveProvinceCodes() {
        Set<String> provinces = new LinkedHashSet<>();
        for (RemoteTenantInfoVo tenant : remoteTenantService.listActiveTenants()) {
            String province = normalize(tenant.getProvinceCode());
            if (StringUtils.isBlank(province) && StringUtils.isNotBlank(tenant.getDistrictCode())) {
                String district = normalize(tenant.getDistrictCode());
                if (StringUtils.isNotBlank(district) && district.length() >= 2) {
                    province = normalize(padLeft(district, 6).substring(0, 2) + "0000");
                }
            }
            if (StringUtils.isNotBlank(province)) {
                String padded = padLeft(province, 6);
                provinces.add(padded.substring(0, 2) + "0000");
            }
        }
        return provinces;
    }

    private static String normalize(String raw) {
        return SfMasterTenantDistrictAccessor.normalizeRegionCode(raw);
    }

    private static String padLeft(String digits, int length) {
        String value = digits;
        while (value.length() < length) value = "0" + value;
        return value.length() > length ? value.substring(0, length) : value;
    }

    @Value
    public static class TenantCodes {
        String provinceCode;
        String cityCode;
        String districtCode;
    }
}
