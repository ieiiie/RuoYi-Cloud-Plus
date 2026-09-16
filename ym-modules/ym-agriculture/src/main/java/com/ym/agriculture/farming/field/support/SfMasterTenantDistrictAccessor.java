package com.ym.agriculture.farming.field.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.system.api.RemoteTenantService;
import com.ym.system.api.domain.vo.RemoteTenantInfoVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.List;

/** 读取平台租户行政区划的跨服务桥接。 */
@Component
public class SfMasterTenantDistrictAccessor {

    @DubboReference
    private RemoteTenantService remoteTenantService;

    public String selectNormalizedDistrictCode(String tenantId) {
        if (StringUtils.isBlank(tenantId)) return null;
        RemoteTenantInfoVo tenant = remoteTenantService.getTenant(tenantId.trim());
        return tenant == null ? null : normalizeRegionCode(tenant.getDistrictCode());
    }

    public List<String> listActiveTenantIdsByAdcode(String adcode) {
        String normalized = normalizeRegionCode(adcode);
        if (StringUtils.isBlank(normalized)) return List.of();
        return remoteTenantService.listActiveTenants().stream()
            .filter(tenant -> tenant != null && StringUtils.isNotBlank(tenant.getTenantId()))
            .filter(tenant -> normalized.equals(normalizeRegionCode(tenant.getDistrictCode())))
            .map(tenant -> tenant.getTenantId().trim()).distinct().sorted().toList();
    }

    static String normalizeRegionCode(String raw) {
        if (raw == null) return null;
        String value = raw.trim().replaceAll("\\s+", "");
        if (value.isEmpty()) return null;
        if (value.chars().allMatch(Character::isDigit)) {
            int index = 0;
            while (index < value.length() - 1 && value.charAt(index) == '0') index++;
            return value.substring(index);
        }
        return value;
    }
}
